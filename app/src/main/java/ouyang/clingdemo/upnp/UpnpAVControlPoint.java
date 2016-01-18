package ouyang.clingdemo.upnp;

import android.util.Log;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.igd.callback.GetStatusInfo;
import org.fourthline.cling.support.model.Connection;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;

import java.util.HashMap;
import java.util.Map;

public class UpnpAVControlPoint implements RegistryListener {

    private static final String TAG = UpnpAVControlPoint.class.getSimpleName();
    private Map<String, UpnpDevice> devices = new HashMap<>();
    private UpnpService upnp;
    private MediaRenderer dmr;
    private UpnpDeviceListener listener;

    public UpnpAVControlPoint() {
        upnp = new UpnpServiceImpl(new AndroidUpnpServiceConfiguration(8081), this);
        dmr = new MediaRenderer(upnp.getControlPoint());
    }

    public void startScan(UpnpDeviceListener listener) {
        Log.d(TAG, "startScan");

        this.listener = listener;
        this.upnp.getControlPoint().search();
    }

    public void stopScan() {
        Log.d(TAG, "stopScan");

        this.upnp.shutdown();
    }

    public boolean isConnected() {
        return dmr.isConnected();
    }

    public boolean isConnected(String ip) {
        if (dmr.isConnected()) {
            if (dmr.getDevice().getDeviceIp().equals(ip)) {
                return true;
            }
        }

        return false;
    }

    public void connect(String deviceId, MediaRenderer.EventListener listener) throws UpnpException {
        Log.d(TAG, "connect: " + deviceId);

        if (dmr.isConnected()) {
            throw new UpnpException("already connected");
        }

        UpnpDevice device = devices.get(deviceId);
        if (device == null) {
            throw new UpnpException("deviceId invalid");
        }

        dmr.setDevice(device);
        dmr.setIgnoreStopped(true);
        dmr.setListener(listener);
        dmr.subscribeAVT();
        dmr.subscribeRCS();
        dmr.setConnected(true);
    }

    public void disconnect() throws UpnpException {
        Log.d(TAG, "disconnect");

        if (! dmr.isConnected()) {
            throw new UpnpException("not connected");
        }

        dmr.unsubscribeAVT();
        dmr.unsubscribeRCS();
        dmr.setConnected(false);
    }

    public void play(String url) throws UpnpException {
        this.play(url, null);
    }

    public void play(final String url, final String title) throws UpnpException {
        Log.d(TAG, "play: " + url);

        this.upnp.getControlPoint().execute(new GetTransportInfo(dmr.AVTransport()) {
            @Override
            public void received(ActionInvocation actionInvocation, TransportInfo transportInfo) {
                Log.d(TAG, "GetTransportInfo: " + transportInfo.getCurrentTransportState());

                switch (transportInfo.getCurrentTransportState()) {
                    case STOPPED:
                        try {
                            _play(url, title);
                        } catch (UpnpException e) {
                            e.printStackTrace();
                        }
                        break;

                    case NO_MEDIA_PRESENT:
                    case PLAYING:
                    case TRANSITIONING:
                    case PAUSED_PLAYBACK:
                    case PAUSED_RECORDING:
                    case RECORDING:
                    case CUSTOM:
                        Log.d(TAG, "execute: Stop");
                        try {
                            upnp.getControlPoint().execute(new Stop(dmr.AVTransport()) {
                                @Override
                                public void success(ActionInvocation invocation) {
                                    super.success(invocation);
                                    Log.d(TAG, "Stop success");

                                    try {
                                        _play(url, title);
                                    } catch (UpnpException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                                    Log.d(TAG, "Stop failure: " + s);
                                }
                            });
                        } catch (UpnpException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }

            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                Log.d(TAG, "GetTransportInfo failure: " + s);
            }
        });
    }

    public void _play(String url, String title) throws UpnpException {
        Log.d(TAG, "_play: " + url);

        this.upnp.getControlPoint().execute(new SetAVTransportURI(dmr.AVTransport(), url) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                dmr.setIgnoreStopped(false);

                try {
                    upnp.getControlPoint().execute(new Play(dmr.AVTransport()) {
                        @Override
                        public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                            Log.d(TAG, "Play failure: " + s);
                        }
                    });
                } catch (UpnpException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                Log.d(TAG, "SetAVTransportURI failure: " + s);
            }
        });
    }

    public void stop() throws UpnpException {
        Log.d(TAG, "stop");

        this.upnp.getControlPoint().execute(new Stop(dmr.AVTransport()) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                dmr.setIgnoreStopped(true);
            }

            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                Log.d(TAG, "Stop failure: " + s);
            }
        });
    }

    public void setPlaybackRate(int rate) throws UpnpException {
        Log.d(TAG, "setPlaybackRate: " + rate);

        if (rate == 0) {
            this.upnp.getControlPoint().execute(new Pause(dmr.AVTransport()) {
                @Override
                public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                    Log.d(TAG, "Pause failure: " + s);
                }
            });
        } else {
            this.upnp.getControlPoint().execute(new Play(dmr.AVTransport()) {
                @Override
                public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                    Log.d(TAG, "Play failure: " + s);
                }
            });
        }
    }

    public void setVolume(int volume) throws UpnpException {
        Log.d(TAG, "setVolume");

        this.upnp.getControlPoint().execute(new SetVolume(dmr.RenderingControl(), volume) {
            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                Log.d(TAG, "SetVolume failure: " + s);
            }
        });
    }

    public String getDeviceIp(String deviceId) throws UpnpException {
        UpnpDevice device = devices.get(deviceId);
        if (device == null) {
            throw new UpnpException("deviceId invalid");
        }

        return device.getDeviceIp();
    }

    public interface GetVolumeHandler {
        void onSucceed(int volume);
        void onFailed(String m);
    }

    public void getVolume(final GetVolumeHandler handler) throws UpnpException {
        Log.d(TAG, "getVolume");

        this.upnp.getControlPoint().execute(new GetVolume(dmr.RenderingControl()) {
            @Override
            public void received(ActionInvocation actionInvocation, int i) {
                handler.onSucceed(i);
            }

            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                Log.d(TAG, "GetVolume failure: " + s);
                handler.onFailed(s);
            }
        });
    }

    public void setPlaybackProgress(int progress) throws UpnpException {
        Log.d(TAG, "setPlaybackProgress");

        int second = progress % 60;
        int minute = (progress / 60) % 60;
        int hour = progress / 3600;
        String target = String.format("%1$02d:%2$02d:%3$02d", hour, minute, second);

        this.upnp.getControlPoint().execute(new Seek(dmr.AVTransport(), SeekMode.ABS_TIME, target) {
            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                Log.d(TAG, "Seek failure: " + s);
            }
        });
    }

    public interface GetPositionInfoHandler {
        void onSucceed(long elapsed, long duration);
        void onFailed(String m);
    }

    public void getPositionInfo(final GetPositionInfoHandler handler) throws UpnpException {
        Log.d(TAG, "getPositionInfo");

        this.upnp.getControlPoint().execute(new GetPositionInfo(dmr.AVTransport()) {
            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                handler.onFailed(s);
            }

            @Override
            public void received(ActionInvocation actionInvocation, PositionInfo positionInfo) {
                handler.onSucceed(positionInfo.getTrackElapsedSeconds(), positionInfo.getTrackDurationSeconds());
            }
        });
    }

    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice remoteDevice) {
//        Log.d(TAG, "remoteDeviceDiscoveryStarted");
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice remoteDevice, Exception e) {
//        Log.d(TAG, "remoteDeviceDiscoveryFailed");
    }

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice remoteDevice) {
//        Log.d(TAG, "remoteDeviceAdded");

        UpnpDevice device = new UpnpDevice(remoteDevice);
        devices.put(device.getDeviceId(), device);

        if (listener != null) {
            listener.onDeviceFound(device);
        }
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice remoteDevice) {
//        Log.d(TAG, "remoteDeviceUpdated");
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice remoteDevice) {
//        Log.d(TAG, "remoteDeviceRemoved");

        UpnpDevice device = new UpnpDevice(remoteDevice);
        devices.remove(device.getDeviceId());

        if (listener != null) {
            listener.onDeviceLost(device);
        }
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice localDevice) {
//        Log.d(TAG, "localDeviceAdded");
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice localDevice) {
//        Log.d(TAG, "localDeviceRemoved");
    }

    @Override
    public void beforeShutdown(Registry registry) {
//        Log.d(TAG, "beforeShutdown");
    }

    @Override
    public void afterShutdown() {
//        Log.d(TAG, "afterShutdown");
    }
}
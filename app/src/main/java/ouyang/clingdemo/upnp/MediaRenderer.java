package ouyang.clingdemo.upnp;

import android.util.Log;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.support.model.PlayMode;
import org.fourthline.cling.support.model.TransportState;

import ouyang.clingdemo.upnp.callback.AVTransportCallback;
import ouyang.clingdemo.upnp.callback.RenderingControlCallback;

public class MediaRenderer {

    private static final String TAG = "MediaRenderer";
    private static ServiceType AVT = new UDAServiceType("AVTransport");
    private static ServiceType RCS = new UDAServiceType("RenderingControl");

    private ControlPoint cp;
    private String deviceId;
    private UpnpDevice device;
    private boolean connected;
    private SubscriptionCallback avtCallback;
    private SubscriptionCallback rcsCallback;
    private EventListener listener;
    private TransportState lastState = TransportState.CUSTOM;
    private boolean ignoreStopped;

    public void setListener(EventListener listener) {
        this.listener = listener;
    }

    public void setIgnoreStopped(boolean ignoreStopped) {
        this.ignoreStopped = ignoreStopped;
    }

    public interface EventListener {
        void onVolumeChanged(int newVolume);
        void onLoading();
        void onPlaying();
        void onNoMediaPresent_Stopped();
        void onStopped();
        void onPaused();
    }

    public MediaRenderer(ControlPoint cp) {
        this.cp = cp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public SubscriptionCallback getAvtCallback() {
        return avtCallback;
    }

    public void setAvtCallback(SubscriptionCallback avtCallback) {
        this.avtCallback = avtCallback;
    }

    public SubscriptionCallback getRcsCallback() {
        return rcsCallback;
    }

    public void setRcsCallback(SubscriptionCallback rcsCallback) {
        this.rcsCallback = rcsCallback;
    }

    public UpnpDevice getDevice() {
        return device;
    }

    public void setDevice(UpnpDevice device) {
        this.device = device;
    }

    public void subscribeAVT() throws UpnpException {
        RemoteService avt = device.getService(AVT);
        if (avt == null) {
            throw new UpnpException("invalid operation: service not found");
        }

        avtCallback = new AVTransportCallback(avt) {
            @Override
            protected void onDisconnect(CancelReason reason) {
                Log.d(TAG, "AVT onDisconnect: " + reason);
            }

            @Override
            protected void onStateChange(int instanceId, TransportState state) {
                Log.d(TAG, "AVT onStateChange: " + state);

                if (listener == null) {
                    return;
                }

                switch (state) {
                    case STOPPED:
                        if (! ignoreStopped) {
                            if (lastState == TransportState.NO_MEDIA_PRESENT) {
                                listener.onNoMediaPresent_Stopped();
                            }
                            else if (lastState == TransportState.TRANSITIONING) {
                                Log.d(TAG, "lastState is TRANSITIONING, ignore it!");
                            }
                            else {
                                listener.onStopped();
                            }
                        }
                        else {
                            Log.d(TAG, "STOPPED is ignore!");
                        }
                        break;

                    case PLAYING:
                        listener.onPlaying();
                        break;

                    case TRANSITIONING:
                        listener.onLoading();
                        break;

                    case PAUSED_PLAYBACK:
                        listener.onPaused();
                        break;

                    case PAUSED_RECORDING:
                        break;

                    case RECORDING:
                        break;

                    case NO_MEDIA_PRESENT:
                        break;

                    case CUSTOM:
                        break;
                }

                lastState = state;
            }

            @Override
            protected void onPlayModeChange(int instanceId, PlayMode playMode) {
                Log.d(TAG, "AVT onPlayModeChange: " + playMode);
            }

            @Override
            protected void onCurrentTrackURIChange(int instanceId, String uri) {
                Log.d(TAG, "AVT onCurrentTrackURIChange: " + uri);
            }
        };

        cp.execute(avtCallback);
    }

    public void subscribeRCS() throws UpnpException {
        RemoteService rcs = device.getService(RCS);
        if (rcs == null) {
            throw new UpnpException("invalid operation: service not found");
        }

        rcsCallback = new RenderingControlCallback(rcs) {

            @Override
            protected void onDisconnect(CancelReason reason) {
                Log.d(TAG, "RCS onDisconnect: " + reason);
            }

            @Override
            protected void onMasterVolumeChanged(int instanceId, int newVolume) {
                Log.d(TAG, "RCS onMasterVolumeChanged: " + newVolume);
                if (listener != null) {
                    listener.onVolumeChanged(newVolume);
                }
            }
        };

        cp.execute(rcsCallback);
    }

    public void unsubscribeAVT() {
    }

    public void unsubscribeRCS() {
    }

    public Service AVTransport() throws UpnpException {
        if (! this.isConnected()) {
            throw new UpnpException("not connected");
        }

        if (device == null) {
            throw new UpnpException("device is null");
        }

        Service service = device.getService(AVT);
        if (service == null) {
            throw new UpnpException("invalid operation: service not found");
        }

        return service;
    }

    public Service RenderingControl() throws UpnpException {
        if (! this.isConnected()) {
            throw new UpnpException("not connected");
        }

        if (device == null) {
            throw new UpnpException("device is null");
        }

        Service service = device.getService(RCS);
        if (service == null) {
            throw new UpnpException("invalid operation: service not found");
        }

        return service;
    }
}
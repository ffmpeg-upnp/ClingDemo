package com.miui.upnp;

import android.util.Log;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.connectionmanager.callback.GetProtocolInfo;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;
import org.seamless.util.MimeType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class UpnpAVControlPoint implements RegistryListener {

    private static final String TAG = UpnpAVControlPoint.class.getSimpleName();
    private Map<String, UpnpDevice> devices = new HashMap<String, UpnpDevice>();
    private UpnpService upnp;
    private MediaRenderer dmr;
    private UpnpDeviceListener listener;
    private boolean started;

    public void startScan(UpnpDeviceListener listener) throws UpnpException {
        Log.d(TAG, "startScan");

        if (started) {
            throw new UpnpException("already started");
        }

        this.started = true;
        this.listener = listener;
        this.upnp = new UpnpServiceImpl(new AndroidUpnpServiceConfiguration(8081), this);
        this.dmr = new MediaRenderer(this.upnp.getControlPoint());
        this.upnp.getControlPoint().search(new UDADeviceTypeHeader(new UDADeviceType("MediaRenderer")));
    }

    public void stopScan() throws UpnpException {
        Log.d(TAG, "stopScan");

        if (! started) {
            throw new UpnpException("not started");
        }

        this.started = false;
        this.upnp.getControlPoint().getRegistry().shutdown();
        this.upnp.shutdown();
    }

    public boolean isConnected() throws UpnpException {
        if (! started) {
            throw new UpnpException("not started");
        }

        return dmr.isConnected();
    }

    public boolean isConnected(String ip) throws UpnpException {
        if (! started) {
            throw new UpnpException("not started");
        }

        return dmr.isConnected() && dmr.getDevice().getDeviceIp().equals(ip);
    }

    public String getConnectedDeviceIp() throws UpnpException {
        if (! started) {
            throw new UpnpException("not started");
        }

        if (dmr.isConnected()) {
            return dmr.getDevice().getDeviceIp();
        }

        return null;
    }

    public void connect(String deviceId, MediaRenderer.EventListener listener) throws UpnpException {
        Log.d(TAG, "connect: " + deviceId);

        if (! started) {
            throw new UpnpException("not started");
        }

        if (dmr.isConnected()) {
            throw new UpnpException("already connected");
        }

        UpnpDevice device = devices.get(deviceId);
        if (device == null) {
            throw new UpnpException("deviceId invalid");
        }

        dmr.setDevice(device);
        dmr.setListener(listener);

        this.connectIfNecessary();
    }

    public void disconnect() throws UpnpException {
        Log.d(TAG, "disconnect");

        if (! started) {
            throw new UpnpException("not started");
        }

        if (! dmr.isConnected()) {
            throw new UpnpException("not connected");
        }

        dmr.setConnected(false);
        dmr.unsubscribeAVT();
        dmr.unsubscribeRCS();
    }

    private void connectIfNecessary() throws UpnpException {
        if (! started) {
            throw new UpnpException("not started");
        }

        if (! dmr.isConnected()) {
            dmr.setIgnoreStopped(true);
            dmr.subscribeAVT();
            dmr.subscribeRCS();
            dmr.setConnected(true);
        }
    }

    public void play(String url) throws UpnpException {
        this.play(url, null, null);
    }

    public void play(final String url, final String title, final String extra) throws UpnpException {
        Log.d(TAG, "play: " + url);

        if (! started) {
            throw new UpnpException("not started");
        }

        this.connectIfNecessary();

        this.upnp.getControlPoint().execute(new GetTransportInfo(dmr.AVTransport()) {
            @Override
            public void received(ActionInvocation actionInvocation, TransportInfo transportInfo) {
                Log.d(TAG, "GetTransportInfo: " + transportInfo.getCurrentTransportState());

                switch (transportInfo.getCurrentTransportState()) {
                    case STOPPED:
                        _play(url, title, extra);
                        break;

                    case NO_MEDIA_PRESENT:
                    case PLAYING:
                    case TRANSITIONING:
                    case PAUSED_PLAYBACK:
                    case PAUSED_RECORDING:
                    case RECORDING:
                    case CUSTOM:
                        _stopAndPlay(url, title, extra);
                        break;

                    default:
                        break;
                }
            }

            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                Log.d(TAG, "GetTransportInfo failure: " + s);
            }
        });
    }

    private void _stopAndPlay(final String url, final String title, final String extra) {
        Log.d(TAG, "execute: Stop");

        try {
            upnp.getControlPoint().execute(new Stop(dmr.AVTransport()) {
                @Override
                public void success(ActionInvocation invocation) {
                    super.success(invocation);
                    Log.d(TAG, "Stop success");
                    _play(url, title, extra);
                }

                @Override
                public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                    Log.d(TAG, "Stop failure: " + s);
                }
            });
        } catch (UpnpException e) {
            // Ignore exception
        }
    }

    private void _play(String url, String title, String extra) {
        Log.d(TAG, "_play: " + url);
        Log.d(TAG, "extra: " + extra);

        String metadata = null;
        UpnpAVExtra avExtra = new UpnpAVExtra();
        if (avExtra.parse(extra)) {
            long id = 0;
            String parentId = "0";
            String album = null;
            String albumUri = null;

            try {
                album = avExtra.getAlbum();
                albumUri = MediaCover.getAlbumUri(avExtra.getArtist());
            } catch (IOException e) {
                // Ignore exception
            }

            MusicTrack track = new MusicTrack(String.valueOf(id), parentId, title, avExtra.getArtist(), album, new PersonWithRole(avExtra.getArtist()));
            track.setWriteStatus(WriteStatus.NOT_WRITABLE);

            DIDLObject.Property.UPNP.ALBUM_ART_URI albumArtUri = new DIDLObject.Property.UPNP.ALBUM_ART_URI();
            if (albumUri != null) {
                try {
                    albumArtUri.setValue(new URI(albumUri));
                } catch (URISyntaxException e) {
                    // Ignore exception
                }
            }

            track.addProperty(albumArtUri);

            DIDLContent content = new DIDLContent();
            content.addItem(track);

            DIDLParser parser = new DIDLParser();

            try {
                metadata = parser.generate(content);
            } catch (Exception e) {
                // Ignore exception
            }

            Log.d(TAG, "metadata: " + metadata);
        }

        try {
            final Service avt = dmr.AVTransport();

            this.upnp.getControlPoint().execute(new SetAVTransportURI(avt, url, metadata) {
                @Override
                public void success(ActionInvocation invocation) {
                    super.success(invocation);
                    dmr.setIgnoreStopped(false);
                    upnp.getControlPoint().execute(new Play(avt) {
                        @Override
                        public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                            Log.d(TAG, "Play failure: " + s);
                        }
                    });
                }

                @Override
                public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                    Log.d(TAG, "SetAVTransportURI failure: " + s);
                }
            });
        } catch (UpnpException e) {
            // Ignore exception
        }
    }

    public void stop() throws UpnpException {
        Log.d(TAG, "stop");

        if (! started) {
            throw new UpnpException("not started");
        }

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

        if (! started) {
            throw new UpnpException("not started");
        }

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

        if (! started) {
            throw new UpnpException("not started");
        }

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

        if (! started) {
            throw new UpnpException("not started");
        }

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

        if (! started) {
            throw new UpnpException("not started");
        }

        int second = progress % 60;
        int minute = (progress / 60) % 60;
        int hour = progress / 3600;
        String target = String.format("%1$02d:%2$02d:%3$02d", hour, minute, second);
        Log.v(TAG, String.format("Seek: %s", target));

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

        if (! started) {
            throw new UpnpException("not started");
        }

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
        // NOOP
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice remoteDevice, Exception e) {
        // NOOP
    }

    @Override
    public void remoteDeviceAdded(Registry registry, final RemoteDevice remoteDevice) {
        RemoteService cms = remoteDevice.findService(MediaRenderer.CMS);
        if (cms == null) {
            return;
        }

        try {
            this.upnp.getControlPoint().execute(new GetProtocolInfo(cms) {
                @Override
                public void received(ActionInvocation actionInvocation, ProtocolInfos sink, ProtocolInfos source) {
                    UpnpDevice device = new UpnpDevice(remoteDevice);

                    Log.d(TAG, device.getFriendlyName() + " " + device.getDeviceIp());

                    for (int i = 0; i < sink.size(); i++) {
                        ProtocolInfo info = sink.get(i);
                        MimeType t = info.getContentFormatMimeType();
                        if (t == null) {
                            continue;
                        }

                        if (t.getType() == null) {
                            continue;
                        }

                        if ("video".equals(t.getType())) {
                            device.setSupportVideo(true);
                            break;
                        }
                    }

                    devices.put(device.getDeviceId(), device);

                    if (listener != null) {
                        listener.onDeviceFound(device);
                    }
                }

                @Override
                public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                    Log.e(TAG, "failure: " + s);

                    UpnpDevice device = new UpnpDevice(remoteDevice);
                    device.setSupportVideo(true);

                    Log.d(TAG, device.getFriendlyName() + " " + device.getDeviceIp());

                    devices.put(device.getDeviceId(), device);

                    if (listener != null) {
                        listener.onDeviceFound(device);
                    }
                }
             });
        } catch(IllegalArgumentException e) {
            // Ignore exception
        }
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice remoteDevice) {
        // NOOP
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice remoteDevice) {
        UpnpDevice device = new UpnpDevice(remoteDevice);
        devices.remove(device.getDeviceId());

        if (listener != null) {
            listener.onDeviceLost(device);
        }
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice localDevice) {
        // NOOP
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice localDevice) {
        // NOOP
    }

    @Override
    public void beforeShutdown(Registry registry) {
        // NOOP
    }

    @Override
    public void afterShutdown() {
        // NOOP
    }
}

package ouyang.clingdemo.upnp;

import android.util.Log;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;

import java.util.HashMap;
import java.util.Map;

public class UpnpControlPoint implements RegistryListener {

    private static final String TAG = UpnpControlPoint.class.getSimpleName();
    private Map<String, UpnpDevice> devices = new HashMap<>();
    private UpnpService upnp;
    private UpnpDeviceListener listener;

    public UpnpControlPoint() {
        upnp = new UpnpServiceImpl(new AndroidUpnpServiceConfiguration(8081), this);
    }

    public void startScan(UpnpDeviceListener listener) {
        this.listener = listener;
        this.upnp.getControlPoint().search();
    }

    public void stopScan() {
        this.upnp.shutdown();
    }

    public void s() {
        // this.upnp.getControlPoint().
    }

    public void play(String deviceId, String url) throws UpnpException {
        Log.d(TAG, "play: " + url);

        UpnpDevice device = devices.get(deviceId);
        if (device == null) {
            throw new UpnpException("deviceId invalid");
        }

        Service service = device.getService(AVSupport.AVT);
        if (service == null) {
            throw new UpnpException("invalid operation: service not found");
        }

        this.upnp.getControlPoint().execute(new SetAVTransportURI(service, url) {
            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                Log.d(TAG, "SetAVTransportURI failure: " + s);
            }
        });

        this.upnp.getControlPoint().execute(new Play(service) {
            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                Log.d(TAG, "Play failure: " + s);
            }
        });
    }

    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice remoteDevice) {
        Log.d(TAG, "remoteDeviceDiscoveryStarted");
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice remoteDevice, Exception e) {
        Log.d(TAG, "remoteDeviceDiscoveryFailed");
    }

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice remoteDevice) {
        Log.d(TAG, "remoteDeviceAdded");

        UpnpDevice device = new UpnpDevice(remoteDevice);
        devices.put(device.getDeviceId(), device);

        listener.onDeviceFound(device);
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice remoteDevice) {
        Log.d(TAG, "remoteDeviceUpdated");
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice remoteDevice) {
        Log.d(TAG, "remoteDeviceRemoved");

        UpnpDevice device = new UpnpDevice(remoteDevice);
        devices.remove(device.getDeviceId());

        listener.onDeviceLost(device);
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice localDevice) {
        Log.d(TAG, "localDeviceAdded");
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice localDevice) {
        Log.d(TAG, "localDeviceRemoved");
    }

    @Override
    public void beforeShutdown(Registry registry) {
        Log.d(TAG, "beforeShutdown");
    }

    @Override
    public void afterShutdown() {
        Log.d(TAG, "afterShutdown");
    }
}
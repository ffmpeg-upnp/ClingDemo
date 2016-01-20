package com.miui.upnp;

public interface UpnpDeviceListener {

    void onDeviceFound(UpnpDevice device);

    void onDeviceLost(UpnpDevice device);
}

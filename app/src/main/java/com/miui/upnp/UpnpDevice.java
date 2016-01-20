package com.miui.upnp;

import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.ServiceType;

public class UpnpDevice {

    private RemoteDevice device;
    private boolean supportVideo;

    public UpnpDevice(RemoteDevice device) {
        this.device = device;
    }

    public String getManufacturerURL() {
        DeviceDetails details = device.getDetails();
        if (details != null) {
            ManufacturerDetails manu = details.getManufacturerDetails();
            if (manu != null) {
                if (manu.getManufacturerURI() != null) {
                    return manu.getManufacturerURI().toString();
                }
            }
        }

        return null;
    }

    public String getDeviceIp() {
        if (device.getIdentity() != null) {
            if (device.getIdentity().getDescriptorURL() != null) {
                return device.getIdentity().getDescriptorURL().getHost();
            }
        }

        return null;
    }

    public String getDeviceId() {
        if (device.getIdentity() != null) {
            if (device.getIdentity().getUdn() != null) {
                return device.getIdentity().getUdn().getIdentifierString();
            }
        }

        return getDeviceIp();
    }

    public String getFriendlyName() {
        return device.getDetails().getFriendlyName();
    }

    public String getDeviceType() {
        return device.getType().getType();
    }

    public RemoteService getService(ServiceType type) {
        return device.findService(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpnpDevice that = (UpnpDevice) o;

        return !(device != null ? !device.equals(that.device) : that.device != null);

    }

    @Override
    public int hashCode() {
        return device != null ? device.hashCode() : 0;
    }

    public boolean isSupportVideo() {
        return supportVideo;
    }

    public void setSupportVideo(boolean supportVideo) {
        this.supportVideo = supportVideo;
    }
}
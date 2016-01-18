package ouyang.clingdemo.upnp.callback;

import android.util.Log;

import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlVariable;

public abstract class RenderingControlCallback extends SubscriptionCallback {

    private static final String TAG = "RCS";

    public RenderingControlCallback(Service service) {
        super(service);
    }

    @Override
    protected void failed(GENASubscription subscription,
                          UpnpResponse responseStatus,
                          Exception exception,
                          String defaultMsg) {
        Log.d(TAG, "subscribe failed: " + defaultMsg);
    }

    public void established(GENASubscription subscription) {
        Log.d(TAG, "established");
    }

    public void ended(GENASubscription subscription, final CancelReason reason, UpnpResponse responseStatus) {
        onDisconnect(reason);
    }

    public void eventReceived(GENASubscription subscription) {
        final LastChange lastChange;
        try {
            lastChange = new LastChange(
                    new RenderingControlLastChangeParser(),
                    subscription.getCurrentValues().get("LastChange").toString()
            );
        } catch (Exception ex) {
            return;
        }

        for (UnsignedIntegerFourBytes instanceId : lastChange.getInstanceIDs()) {
            RenderingControlVariable.Volume volume = lastChange.getEventedValue(
                    instanceId,
                    RenderingControlVariable.Volume.class
            );

            if (volume != null && volume.getValue().getChannel().equals(Channel.Master)) {
                onMasterVolumeChanged(instanceId.getValue().intValue(), volume.getValue().getVolume());
            }
        }
    }

    public void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
    }

    abstract protected void onDisconnect(CancelReason reason);

    abstract protected void onMasterVolumeChanged(int instanceId, int newVolume);
}
package ouyang.clingdemo.upnp.callback;

import android.util.Log;

import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.PlayMode;
import org.fourthline.cling.support.model.TransportState;

public abstract class AVTransportCallback extends SubscriptionCallback {

    private static final String TAG = "AVT";

    public AVTransportCallback(Service service) {
        super(service);
    }

    @Override
    protected void failed(GENASubscription subscription,
                          UpnpResponse responseStatus,
                          Exception exception,
                          String defaultMsg) {
        Log.d(TAG, "subscribe failed: " + defaultMsg);
    }

    protected void established(GENASubscription subscription) {
        Log.d(TAG, "established");
    }

    protected void ended(GENASubscription subscription, final CancelReason reason, UpnpResponse responseStatus) {
        onDisconnect(reason);
    }

    protected void eventReceived(GENASubscription subscription) {
        final LastChange lastChange;
        try {
            lastChange = new LastChange(
                    new AVTransportLastChangeParser(),
                    subscription.getCurrentValues().get("LastChange").toString()
            );
        } catch (Exception ex) {
            return;
        }

        for (UnsignedIntegerFourBytes instanceId : lastChange.getInstanceIDs()) {
            AVTransportVariable.TransportState transportState =
                    lastChange.getEventedValue(
                            instanceId,
                            AVTransportVariable.TransportState.class
                    );

            if (transportState != null) {
                onStateChange(instanceId.getValue().intValue(), transportState.getValue());
            }

            AVTransportVariable.CurrentPlayMode currentPlayMode =
                    lastChange.getEventedValue(
                            instanceId,
                            AVTransportVariable.CurrentPlayMode.class
                    );

            if (currentPlayMode != null) {
                onPlayModeChange(
                        instanceId.getValue().intValue(),
                        currentPlayMode.getValue()
                );
            }

            AVTransportVariable.CurrentTrackURI currentTrackURI = lastChange.getEventedValue(instanceId, AVTransportVariable.CurrentTrackURI.class);
            if (currentTrackURI != null) {
                onCurrentTrackURIChange(
                        instanceId.getValue().intValue(),
                        currentTrackURI.getValue() != null ? currentTrackURI.getValue().toString() : ""
                );
            }
        }
    }

    protected void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
    }

    abstract protected void onDisconnect(CancelReason reason);

    abstract protected void onStateChange(int instanceId, TransportState state);

    abstract protected void onPlayModeChange(int instanceId, PlayMode playMode);

    abstract protected void onCurrentTrackURIChange(int instanceId, String uri);
}

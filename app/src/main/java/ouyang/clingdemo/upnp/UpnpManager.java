package ouyang.clingdemo.upnp;

public class UpnpManager {

    private static final String TAG = UpnpManager.class.getSimpleName();
    private static final Object classLock = UpnpManager.class;
    private static UpnpManager instance = null;
    private static UpnpAVControlPoint cp;

    public static UpnpManager getInstance() {
        synchronized (classLock) {
            if (instance == null) {
                instance = new UpnpManager();
            }

            return instance;
        }
    }

    private UpnpManager() {
    }

    public void initialize() {
        cp = new UpnpAVControlPoint();
    }

    public static UpnpAVControlPoint getControlPoint() {
        synchronized (classLock) {
            return cp;
        }
    }
}

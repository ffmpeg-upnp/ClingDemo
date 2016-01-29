package ouyang.clingdemo.fileserver;

import android.util.Log;

import java.util.UUID;

import com.miui.upnp.UpnpException;
import ouyang.clingdemo.utils.NetUtil;

public class FileServer {

    private static final String TAG = "FileServer";
    private HttpFileServer server;

    public FileServer() {
        server = HttpFileServerFactory.create();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }

    public String getHttpUrl(String uri, String remoteIp) throws UpnpException {
        String url;

        if (uri == null) {
            return null;
        }

        if (uri.startsWith("http")) {
            url = uri;
        }
        else {
            String u = server.addLocalFile(UUID.nameUUIDFromBytes(uri.getBytes()).toString() + ".png", uri);
            if (u == null) {
                throw new UpnpException("uri invalid: " + uri);
            }

            url = String.format("http://%s%s",
                    NetUtil.getLocalIpAddress(remoteIp),
                    u);
        }

        Log.d(TAG, "url is: " + url);

        return url;
    }
}
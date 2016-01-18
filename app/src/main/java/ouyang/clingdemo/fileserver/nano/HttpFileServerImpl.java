package ouyang.clingdemo.fileserver.nano;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import ouyang.clingdemo.fileserver.HttpFileServer;

public class HttpFileServerImpl implements HttpFileServer, HttpServer.Listener {

    private static final String TAG = "HttpFileServerImpl";

    private Context context;
    private HttpServer httpServer;
    private FileListener fileListener;

    public HttpFileServerImpl() {
        httpServer = new HttpServer(0);
    }

    @Override
    public void setListener(FileListener listener) {
        this.fileListener = listener;
    }

    @Override
    public boolean start() {
        boolean ret = true;

        try {
            httpServer.setListener(this);
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    @Override
    public void stop() {
        httpServer.stop();
    }

    @Override
    public String addLocalFile(String webFile, String file) {
        if (file == null) {
            return null;
        }

        String theUrl = null;

        try {
            theUrl = URLDecoder.decode(file, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        Log.d(TAG, String.format("file: %s", file));
        Log.d(TAG, String.format("file decode: %s", theUrl));

        String localFile = null;

        boolean isLocalFile = false;
        String a[] = file.split("//");

        // /sdcard/1.mp3
        if (a.length == 1) {
            isLocalFile = true;
            localFile = file;
        } else if (a.length == 2) {

            // file:///sdcard/1.mp3
            if (a[0].equalsIgnoreCase("file:")) {
                isLocalFile = true;
                localFile = a[1];
            }
        }

        if (isLocalFile) {
//            String[] name = localFile.split("\\.");
//            String webFile = String.format("%s.%s", UUID.randomUUID().toString(),
//                    name[name.length - 1]);

            String urlWithoutIp = httpServer.addLocalFile(webFile, localFile);
            if (urlWithoutIp == null)
                return null;

//            theUrl = String.format("http://%s%s", ip, urlWithoutIp);
            theUrl = urlWithoutIp;
        }

        return theUrl;
    }

    @Override
    public void removeFile(String fileUri) {
        httpServer.removeLocalFile(fileUri);
    }

    @Override
    public void removeAll() {
        httpServer.removeAll();
    }

    @Override
    public void onSending(String file, long sendingBytes, long size, String ip, int port) {
        if (fileListener != null) {
            fileListener.onSending(file, sendingBytes, size, ip, port);
        }
    }
}
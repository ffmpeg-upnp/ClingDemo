package ouyang.clingdemo.fileserver.nano;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpServer extends NanoHTTPD  {

    private static final String TAG = HttpServer.class.getSimpleName();
    private Map<String, String> mWebFiles = new HashMap<String, String>();
    private Listener listener;

    public interface Listener {
        void onSending(String file, long sentBytes, long size, String ip, int port);
    }

    public HttpServer(int port) {
        super(port);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int port() {
        int p = super.getListeningPort();
        Log.v(TAG, "port: " + p);
        return p;
    }

    public boolean isRunning() {
        return super.isAlive();
    }

    public String addWebFolder(String webFolder, String localFolder) {
        return null;
    }

    public void removeWebFolder(String webFolder) {
    }

    public String addLocalFile(String webFile, String localFile) {
        webFile = "/" + webFile;
        mWebFiles.put(webFile, localFile);
        String url = ":" + this.port() + webFile;
        Log.v(TAG, "add localFile: " + localFile);
        return url;
    }

    public void removeLocalFile(String localFile) {
        for (Map.Entry<String, String> entry : mWebFiles.entrySet()) {
            String webFile = entry.getKey();
            String file = entry.getValue();
            if (file.equals(localFile)) {
                mWebFiles.remove(webFile);
                Log.v(TAG, "remove localFile: " + localFile);
                break;
            }
        }
    }

    public void removeAll() {
        mWebFiles.clear();
        Log.v(TAG, "remove all file");
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String uri = session.getUri();

        return respond(Collections.unmodifiableMap(headers), uri);
    }

    private Response respond(Map<String, String> headers, String uri) {
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        String localFile = mWebFiles.get(uri);
        if (localFile == null) {
            Log.v(TAG, "file not found: " + uri);
            return createResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                    "Error 404, file not found.");
        }

        Response response = serveFile(uri, headers, localFile, null);

        return response != null ? response :
                createResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                        "Error 404, file not found.");
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, InputStream message, Response.SendingListener listener) {
        Response res = new Response(status, mimeType, message, listener);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, long dataLen, InputStream message, Response.SendingListener listener) {
        Response res = new Response(status, mimeType, dataLen, message, listener);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, String message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    Response serveFile(String uri, Map<String, String> header, final String localFile, String mime) {
        Response res;
        try {
            File file = new File(localFile);

            // Calculate etag
            String etag = Integer
                    .toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length())
                            .hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is
            // requested
            final long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE,
                            MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;

                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int)dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis, new Response.SendingListener() {
                        @Override
                        public void onSending(long sendingBytes) {
                            if (listener != null) {
                                listener.onSending(localFile, sendingBytes, fileLen, null, 0);
                            }
                        }
                    });

                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/"
                            + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match")))
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = createResponse(Response.Status.OK, mime, fileLen, new FileInputStream(file), new Response.SendingListener() {
                        @Override
                        public void onSending(long sendingBytes) {
                            if (listener != null) {
                                listener.onSending(localFile, sendingBytes, fileLen, null, 0);
                            }
                        }
                    });
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            res = createResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT,
                    "FORBIDDEN: Reading file failed.");
        }

        return res;
    }
}
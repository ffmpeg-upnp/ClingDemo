package ouyang.clingdemo.fileserver;

public interface HttpFileServer {

    interface FileListener {
        void onSending(String file, long sendingBytes, long size, String ip, int port);
    }

    void setListener(FileListener listener);

    boolean start();

    void stop();

    String addLocalFile(String webFile, String localFile);

    void removeFile(String fileUri);

    void removeAll();
}
package ouyang.clingdemo.fileserver;

import ouyang.clingdemo.fileserver.nano.HttpFileServerImpl;

public class HttpFileServerFactory {

    public static HttpFileServer create() {
        return new HttpFileServerImpl();
    }
}
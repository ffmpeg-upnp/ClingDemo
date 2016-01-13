package ouyang.clingdemo.upnp;

import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceType;

public class AVSupport {

    public static ServiceType AVT = new UDAServiceType("AVTransport");
    public static ServiceType RCS = new UDAServiceType("RenderingControl");
}

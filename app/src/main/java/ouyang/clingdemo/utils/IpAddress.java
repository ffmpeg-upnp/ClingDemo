package ouyang.clingdemo.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class IpAddress {

    private IpAddress() {
    }

    public static String getP2pAddress() {
        return getAddress("p2p");
    }

    public static String getAddress(String name) {
        List<NetworkInterface> interfaces;

        try {
            interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }

        for (NetworkInterface nif : interfaces) {
            if (!nif.getName().contains(name)) {
                continue;
            }

            List<InetAddress> addresses = Collections.list(nif.getInetAddresses());

            for (InetAddress address : addresses) {
                if (!address.isLoopbackAddress()) {
                    String ip  = address.getHostAddress().toUpperCase();
                    if (IpAddress.isValidIP4Address(ip)) {
                        return ip;
                    }
                }
            }
        }

        return "";
    }

    public static boolean isValidIP4Address(String ipAddress) {
        if (ipAddress.matches("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")) {
            String[] groups = ipAddress.split("\\.");

            for (int i = 0; i <= 3; i++) {
                String segment = groups[i];
                if (segment == null || segment.length() <= 0) {
                    return false;
                }

                int value = 0;

                try {
                    value = Integer.parseInt(segment);
                } catch (NumberFormatException e) {
                    return false;
                }

                if (value > 255) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Copy from android.net.NetworkUtils;
     */
    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }
}
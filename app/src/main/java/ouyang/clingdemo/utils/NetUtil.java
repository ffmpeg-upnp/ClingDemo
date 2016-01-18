package ouyang.clingdemo.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class NetUtil {

    private static byte[] mMacAddress = null;

    public static byte[] getMacAddress() {
        final int hwAddrLengthInBytes = 6;
        if (mMacAddress == null) {
            try {
                Enumeration<NetworkInterface> netInterfaces = NetworkInterface
                        .getNetworkInterfaces();

                while (netInterfaces.hasMoreElements()) {
                    NetworkInterface ni = netInterfaces.nextElement();
                    byte[] addrBytes = ni.getHardwareAddress();
                    if (!ni.isLoopback() && addrBytes != null
                            && hwAddrLengthInBytes == addrBytes.length) {
                        mMacAddress = addrBytes;
                        break;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }

            if (null == mMacAddress) {
                mMacAddress = new byte[hwAddrLengthInBytes];
                for (int i = 0; i < hwAddrLengthInBytes; ++i) {
                    mMacAddress[i] = (byte) (Math.random() * 0xff);
                }
            }
        }

        return mMacAddress;
    }

    public static final String WLAN0 = "wlan0";

    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }

                byte[] mac = intf.getHardwareAddress();

                if (mac == null) {
                    return "";
                }

                StringBuilder buf = new StringBuilder();

                for (int idx = 0; idx < mac.length; idx++) {
                    buf.append(String.format("%02X:", mac[idx]));
                }

                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = IpAddress.isValidIP4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4) {
                                return sAddr;
                            }
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim < 0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

//    public static String getWifiApIpAddress() {
//        try {
//            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
//                 en.hasMoreElements(); ) {
//                NetworkInterface intf = en.nextElement();
//                if (intf.getName().contains("wlan")) {
//                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
//                            .hasMoreElements(); ) {
//                        InetAddress inetAddress = enumIpAddr.nextElement();
//                        if (!inetAddress.isLoopbackAddress()
//                                && (inetAddress.getAddress().length == 4)) {
//                            return inetAddress.getHostAddress();
//                        }
//                    }
//                }
//            }
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }

    public static InetAddress getWifiInetAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                            .hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            return inetAddress;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] getLocalIpInt(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wm.isWifiEnabled()) {
            return null;
        }

        WifiInfo wi = wm.getConnectionInfo();
        return intToBytes(wi.getIpAddress());
    }

    public static String getLocalIpString(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wm.isWifiEnabled()) {
            return null;
        }

        WifiInfo wi = wm.getConnectionInfo();
        return intToString(wi.getIpAddress());
    }

    private static String intToString(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
                + ((i >> 24) & 0xFF);
    }

    private static byte[] intToBytes(int i) {
        byte[] ip = new byte[4];
        ip[0] = (byte) (i & 0xFF);
        ip[1] = (byte) ((i >> 8) & 0xFF);
        ip[2] = (byte) ((i >> 16) & 0xFF);
        ip[3] = (byte) ((i >> 24) & 0xFF);
        return ip;
    }

    public static String getLocalIpAddress(String remoteIpAddress) {

        Enumeration<NetworkInterface> en;

        try {
            en = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }

        while (en.hasMoreElements()) {

            NetworkInterface nif = en.nextElement();
            Enumeration<InetAddress> enumIpAddr = nif.getInetAddresses();

            while (enumIpAddr.hasMoreElements()) {

                InetAddress inetAddress = enumIpAddr.nextElement();

                if (inetAddress.isLoopbackAddress())
                    continue;

                if (!(inetAddress instanceof Inet4Address))
                    continue;

                String ip = inetAddress.getHostAddress();

                if (remoteIpAddress == null)
                    return ip;

                String[] localIp = ip.split("\\.");
                String[] remoteIp = remoteIpAddress.split("\\.");
                if (localIp.length != 4 || remoteIp.length != 4)
                    return ip;

                if (localIp[0].equals(remoteIp[0])
                        && localIp[1].equals(remoteIp[1])
                        && localIp[2].equals(remoteIp[2]))
                    return ip;
            }
        }

        return null;
    }

    public static String getMacFromArpCache(String ip) {
        String mac = null;
        BufferedReader br = null;

        do {
            String line;

            try {
                br = new BufferedReader(new FileReader("/proc/net/arp"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                break;
            }

            try {
                while ((line = br.readLine()) != null) {
                    String[] splitted = line.split(" +");
                    if (splitted == null) {
                        continue;
                    }

                    if (splitted.length < 4) {
                        continue;
                    }

                    String theMac = splitted[3];
                    String theIp = splitted[0];

                    if (ip.equals(theIp)) {
                        if (theMac.matches("..:..:..:..:..:..")) {
                            mac = theMac;
                        }

                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        } while(false);

        if (br != null) {
            try {
                br.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return mac;
    }

    public static String getManufactory(String mac) {
        if (mac == null) {
            return null;
        }

        String[] m = mac.split(":");
        if (m.length < 3) {
            return null;
        }

        return String.format("%s-%s-%s", m[0], m[1], m[2]);
    }
}

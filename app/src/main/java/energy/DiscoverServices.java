package energy;

import javax.jmdns.ServiceInfo;

public class DiscoverServices {
    public static void main(String[] args) throws Exception {
        print("_solar._tcp.local.", "solar");
        print("_battery._tcp.local.", "battery");
        print("_grid._tcp.local.", "grid");
    }

    static void print(String type, String label) throws Exception {
        ServiceInfo info = Discovery.first(type);
        if (info == null) {
            System.out.println(label + ": not found");
        } else {
            String host = info.getHostAddresses().length > 0 ? info.getHostAddresses()[0] : info.getServer();
            System.out.println(label + ": " + host + ":" + info.getPort());
        }
    }
}

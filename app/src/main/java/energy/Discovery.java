package energy;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.net.InetAddress;

public class Discovery {
    public static ServiceInfo[] list(String type) throws Exception {
        try (JmDNS jmdns = JmDNS.create(InetAddress.getByName("192.168.0.253"))) {
            return jmdns.list(type, 5000);
        }
    }

    public static ServiceInfo first(String type) throws Exception {
        ServiceInfo[] infos = list(type);
        if (infos.length > 0) return infos[0];
        return null;
    }
}

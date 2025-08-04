package energy;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.net.InetAddress;

public class BatteryServer {
    public static void main(String[] args) throws Exception {
        int port = 50052;
        Server server = ServerBuilder.forPort(port)
                .addService(new BatteryServiceImpl())
                .build()
                .start();
        System.out.println("Battery gRPC server started on port " + port);

        JmDNS jmdns = JmDNS.create(InetAddress.getByName("192.168.0.253"));
        ServiceInfo info = ServiceInfo.create("_battery._tcp.local.", "battery-service", port, "grpc=BatteryService");
        jmdns.registerService(info);
        System.out.println("jmDNS registered: " + info.getType() + " " + info.getName());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                jmdns.unregisterAllServices();
                server.shutdown();
            } catch (Exception ignored) {}
        }));

        server.awaitTermination();
    }
}

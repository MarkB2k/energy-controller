package energy;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.net.InetAddress;

public class HealthServer {
    public static void main(String[] args) throws Exception {
        int port = 50054;
        Server server = ServerBuilder.forPort(port)
                .addService(new HealthServiceImpl())
                .build()
                .start();
        System.out.println("Health gRPC server started on port " + port);

        JmDNS jmdns = JmDNS.create(InetAddress.getByName("192.168.0.253"));
        ServiceInfo info = ServiceInfo.create("_health._tcp.local.", "health-service", port, "grpc=HealthService");
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

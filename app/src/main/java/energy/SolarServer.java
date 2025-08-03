package energy;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.net.InetAddress;

public class SolarServer {
    public static void main(String[] args) throws Exception {
        int port = 50051;
        Server server = ServerBuilder.forPort(port)
                .addService(new SolarServiceImpl())
                .build()
                .start();
        System.out.println("Solar gRPC server started on port " + port);

        JmDNS jmdns = JmDNS.create(InetAddress.getByName("192.168.0.253"));
        ServiceInfo info = ServiceInfo.create("_solar._tcp.local.", "solar-service", port, "grpc=SolarService");
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

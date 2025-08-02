package energy;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.net.InetAddress;

public class GridServer {
    public static void main(String[] args) throws Exception {
        int port = 50053;
        Server server = ServerBuilder.forPort(port)
                .addService(new GridServiceImpl())
                .build()
                .start();
        System.out.println("Grid gRPC server started on port " + port);

        JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
        ServiceInfo info = ServiceInfo.create("_grid._tcp.local.", "grid-service", port, "grpc=GridService");
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

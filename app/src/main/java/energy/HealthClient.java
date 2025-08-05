package energy;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class HealthClient {
    public static void main(String[] args) {
        ManagedChannel ch = ManagedChannelBuilder.forAddress("localhost", 50054)
                .usePlaintext()
                .build();
        HealthServiceGrpc.HealthServiceBlockingStub stub = HealthServiceGrpc.newBlockingStub(ch);
        stub.ping(Empty.getDefaultInstance());
        System.out.println("pong");
        ch.shutdownNow();
    }
}

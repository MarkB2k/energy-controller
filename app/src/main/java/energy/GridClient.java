package energy;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GridClient {
    public static void main(String[] args) {
        ManagedChannel ch = ManagedChannelBuilder.forAddress("localhost", 50053)
                .usePlaintext()
                .build();
        GridServiceGrpc.GridServiceBlockingStub stub = GridServiceGrpc.newBlockingStub(ch);
        SpotPrice p = stub.getSpotPrice(Empty.getDefaultInstance());
        System.out.println("price: " + p.getEurPerKwh() + " EUR/kWh");
        ch.shutdownNow();
    }
}

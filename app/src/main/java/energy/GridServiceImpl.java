package energy;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.time.LocalTime;

public class GridServiceImpl extends GridServiceGrpc.GridServiceImplBase {
    @Override
    public void getSpotPrice(Empty request, StreamObserver<SpotPrice> responseObserver) {
        float price = priceForNow();
        SpotPrice s = SpotPrice.newBuilder().setEurPerKwh(price).build();
        responseObserver.onNext(s);
        responseObserver.onCompleted();
    }

    private float priceForNow() {
        int h = LocalTime.now().getHour();
        float base = 0.25f;
        float peak = (h >= 17 && h <= 20) ? 0.15f : 0f;
        return base + peak;
    }
}

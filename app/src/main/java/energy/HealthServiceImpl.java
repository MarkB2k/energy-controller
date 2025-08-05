package energy;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

public class HealthServiceImpl extends HealthServiceGrpc.HealthServiceImplBase {
    @Override
    public void ping(Empty request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}

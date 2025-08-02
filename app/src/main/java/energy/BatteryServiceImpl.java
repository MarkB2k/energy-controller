package energy;

import io.grpc.stub.StreamObserver;

public class BatteryServiceImpl extends BatteryServiceGrpc.BatteryServiceImplBase {

    @Override
    public StreamObserver<ChargeSample> sendChargeSamples(StreamObserver<ChargeSummary> responseObserver) {
        return new StreamObserver<ChargeSample>() {
            double sumAmps = 0;
            double sumVolts = 0;
            int count = 0;

            @Override
            public void onNext(ChargeSample sample) {
                sumAmps += sample.getAmps();
                sumVolts += sample.getVolts();
                count++;
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                double avgA = count == 0 ? 0 : sumAmps / count;
                double avgV = count == 0 ? 0 : sumVolts / count;
                ChargeSummary summary = ChargeSummary.newBuilder()
                        .setAvgAmps((float) avgA)
                        .setAvgVolts((float) avgV)
                        .build();
                responseObserver.onNext(summary);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<ChargeRequest> chargeCycle(StreamObserver<ChargeStatus> responseObserver) {
        final float[] currentSoc = new float[]{50f};
        return new StreamObserver<ChargeRequest>() {
            @Override
            public void onNext(ChargeRequest req) {
                float target = req.getTargetSocPercent();
                if (target < 0) target = 0;
                if (target > 100) target = 100;
                currentSoc[0] = target;
                ChargeStatus status = ChargeStatus.newBuilder()
                        .setCurrentSocPercent(currentSoc[0])
                        .build();
                responseObserver.onNext(status);
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}

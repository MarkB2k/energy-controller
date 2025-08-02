package energy;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BatteryClient {
    public static void main(String[] args) throws Exception {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50052)
                .usePlaintext()
                .build();

        BatteryServiceGrpc.BatteryServiceStub async = BatteryServiceGrpc.newStub(channel);

        CountDownLatch latch1 = new CountDownLatch(1);
        StreamObserver<ChargeSummary> summaryObs = new StreamObserver<>() {
            @Override public void onNext(ChargeSummary s) { System.out.println("avg: " + s.getAvgAmps() + "A " + s.getAvgVolts() + "V"); }
            @Override public void onError(Throwable t) { latch1.countDown(); }
            @Override public void onCompleted() { latch1.countDown(); }
        };
        StreamObserver<ChargeSample> sampleIn = async.sendChargeSamples(summaryObs);
        sampleIn.onNext(ChargeSample.newBuilder().setAmps(5).setVolts(12).build());
        sampleIn.onNext(ChargeSample.newBuilder().setAmps(6).setVolts(12).build());
        sampleIn.onNext(ChargeSample.newBuilder().setAmps(7).setVolts(12).build());
        sampleIn.onCompleted();
        latch1.await(5, TimeUnit.SECONDS);

        CountDownLatch latch2 = new CountDownLatch(1);
        StreamObserver<ChargeStatus> statusObs = new StreamObserver<>() {
            @Override public void onNext(ChargeStatus s) { System.out.println("soc: " + s.getCurrentSocPercent() + "%"); }
            @Override public void onError(Throwable t) { latch2.countDown(); }
            @Override public void onCompleted() { latch2.countDown(); }
        };
        StreamObserver<ChargeRequest> reqIn = async.chargeCycle(statusObs);
        reqIn.onNext(ChargeRequest.newBuilder().setTargetSocPercent(60).build());
        reqIn.onNext(ChargeRequest.newBuilder().setTargetSocPercent(75).build());
        reqIn.onNext(ChargeRequest.newBuilder().setTargetSocPercent(40).build());
        reqIn.onCompleted();
        latch2.await(5, TimeUnit.SECONDS);

        channel.shutdownNow();
    }
}

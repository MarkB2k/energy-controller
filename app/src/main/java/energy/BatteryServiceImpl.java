package energy;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BatteryServiceImpl extends BatteryServiceGrpc.BatteryServiceImplBase {

    @Override
    public StreamObserver<ChargeSample> sendChargeSamples(final StreamObserver<ChargeSummary> responseObserver) {
        return new StreamObserver<ChargeSample>() {
            float sumA = 0f, sumV = 0f; int count = 0;
            @Override public void onNext(ChargeSample v) { sumA += v.getAmps(); sumV += v.getVolts(); count++; }
            @Override public void onError(Throwable t) { }
            @Override public void onCompleted() {
                float avgA = count == 0 ? 0f : sumA / count;
                float avgV = count == 0 ? 0f : sumV / count;
                responseObserver.onNext(ChargeSummary.newBuilder().setAvgAmps(avgA).setAvgVolts(avgV).build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<ChargeRequest> chargeCycle(final StreamObserver<ChargeStatus> responseObserver) {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        AtomicReference<Float> current = new AtomicReference<>(50f);
        AtomicReference<Float> target = new AtomicReference<>(50f);

        Runnable tick = () -> {
            float c = current.get();
            float t = target.get();
            if (Math.abs(t - c) < 0.01f) {
                responseObserver.onNext(ChargeStatus.newBuilder().setCurrentSocPercent(t).build());
                return;
            }
            float step = t > c ? 2f : -2f;
            float next = c + step;
            if ((step > 0 && next > t) || (step < 0 && next < t)) next = t;
            current.set(next);
            responseObserver.onNext(ChargeStatus.newBuilder().setCurrentSocPercent(next).build());
        };

        ScheduledFuture<?> f = exec.scheduleAtFixedRate(tick, 0, 500, TimeUnit.MILLISECONDS);

        return new StreamObserver<ChargeRequest>() {
            @Override public void onNext(ChargeRequest req) { target.set(req.getTargetSocPercent()); }
            @Override public void onError(Throwable t) { f.cancel(true); exec.shutdownNow(); }
            @Override public void onCompleted() { f.cancel(true); exec.shutdown(); responseObserver.onCompleted(); }
        };
    }
}

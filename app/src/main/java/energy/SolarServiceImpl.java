package energy;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SolarServiceImpl extends SolarServiceGrpc.SolarServiceImplBase {
    @Override
    public void streamPower(Empty request, StreamObserver<PowerReading> responseObserver) {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        Random rnd = new Random();
        final int[] sent = {0};

        exec.scheduleAtFixedRate(() -> {
            PowerReading reading = PowerReading.newBuilder()
                    .setTimestampMs(System.currentTimeMillis())
                    .setWatts(100 + rnd.nextInt(301))
                    .build();
            System.out.println("solar reading -> " + reading.getWatts() + "W");
            responseObserver.onNext(reading);
            sent[0]++;
            if (sent[0] >= 10) {
                responseObserver.onCompleted();
                exec.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}


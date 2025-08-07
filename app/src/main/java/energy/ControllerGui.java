package energy;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.stub.StreamObserver;

import javax.jmdns.ServiceInfo;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ControllerGui extends JFrame {
    private final JLabel solarAddr = new JLabel("unknown");
    private final JLabel batteryAddr = new JLabel("unknown");
    private final JLabel gridAddr = new JLabel("unknown");

    private final JTextArea solarLog = new JTextArea(12, 40);
    private final AtomicReference<ManagedChannel> solarChannel = new AtomicReference<>(null);

    private final JLabel batteryAvg = new JLabel("-");
    private final JLabel batterySoc = new JLabel("-");
    private final AtomicReference<ManagedChannel> batteryChannel = new AtomicReference<>(null);
    private final AtomicReference<StreamObserver<ChargeRequest>> batteryReq = new AtomicReference<>(null);

    private final JButton discoverBtn = new JButton("Discover");

    public ControllerGui() {
        setTitle("Energy Controller");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Solar", solarPanel());
        tabs.add("Battery", batteryPanel());
        tabs.add("Grid", gridPanel());
        add(tabs, BorderLayout.CENTER);

        discoverBtn.addActionListener(e -> doDiscover());
        add(discoverBtn, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel solarPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("address:"));
        top.add(solarAddr);
        JButton start = new JButton("Start stream");
        JButton stop = new JButton("Stop");
        start.addActionListener(e -> startSolar());
        stop.addActionListener(e -> stopSolar());
        top.add(start);
        top.add(stop);
        p.add(top, BorderLayout.NORTH);
        solarLog.setEditable(false);
        p.add(new JScrollPane(solarLog), BorderLayout.CENTER);
        return p;
    }

    private JPanel batteryPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("address:"));
        p.add(batteryAddr);

        JButton avgBtn = new JButton("Avg from samples");
        avgBtn.addActionListener(e -> sendBatterySamples());
        p.add(avgBtn);
        p.add(new JLabel("avg:"));
        p.add(batteryAvg);

        JButton connect = new JButton("Connect");
        JButton set60 = new JButton("Set 60%");
        JButton set75 = new JButton("Set 75%");
        JButton stop = new JButton("Stop");
        connect.addActionListener(e -> connectBatteryBidi());
        set60.addActionListener(e -> sendTarget(60f));
        set75.addActionListener(e -> sendTarget(75f));
        stop.addActionListener(e -> stopBattery());
        p.add(connect);
        p.add(set60);
        p.add(set75);
        p.add(stop);
        p.add(new JLabel("state of charge:"));
        p.add(batterySoc);

        return p;
    }

    private JPanel gridPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("address:"));
        p.add(gridAddr);
        JButton getPrice = new JButton("Get price");
        JLabel price = new JLabel("-");
        getPrice.addActionListener(e -> fetchPrice(price));
        p.add(getPrice);
        p.add(price);
        return p;
    }

    private void doDiscover() {
        discoverBtn.setEnabled(false);
        solarAddr.setText("searching...");
        batteryAddr.setText("searching...");
        gridAddr.setText("searching...");

        new Thread(() -> {
            ServiceInfo s = null, b = null, g = null;
            try { s = Discovery.first("_solar._tcp.local."); } catch (Exception ignored) {}
            try { b = Discovery.first("_battery._tcp.local."); } catch (Exception ignored) {}
            try { g = Discovery.first("_grid._tcp.local."); } catch (Exception ignored) {}

            ServiceInfo fs = s, fb = b, fg = g;
            SwingUtilities.invokeLater(() -> {
                solarAddr.setText(toAddr(fs));
                batteryAddr.setText(toAddr(fb));
                gridAddr.setText(toAddr(fg));
                discoverBtn.setEnabled(true);
            });
        }).start();
    }

    private String toAddr(ServiceInfo info) {
        if (info == null) return "not found";
        String host = info.getHostAddresses().length > 0 ? info.getHostAddresses()[0] : info.getServer();
        return host + ":" + info.getPort();
    }

    private Metadata authHeaders() {
        Metadata md = new Metadata();
        Metadata.Key<String> k = Metadata.Key.of("auth-token", Metadata.ASCII_STRING_MARSHALLER);
        md.put(k, "student-123");
        return md;
    }

    private Channel channelWithAuth(ManagedChannel base) {
        ClientInterceptor auth = io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(authHeaders());
        return ClientInterceptors.intercept(base, auth);
    }

    private void startSolar() {
        stopSolar();
        String addr = solarAddr.getText();
        if (!addr.contains(":")) return;
        String[] hp = addr.split(":");
        ManagedChannel base = ManagedChannelBuilder.forAddress(hp[0], Integer.parseInt(hp[1])).usePlaintext().build();
        solarChannel.set(base);
        Channel ch = channelWithAuth(base);
        SolarServiceGrpc.SolarServiceStub stub = SolarServiceGrpc.newStub(ch).withDeadlineAfter(60, TimeUnit.SECONDS);
        solarLog.setText("");
        stub.streamPower(Empty.getDefaultInstance(), new StreamObserver<PowerReading>() {
            @Override public void onNext(PowerReading v) {
                SwingUtilities.invokeLater(() -> solarLog.append(v.getTimestampMs() + " ms : " + v.getWatts() + " W\n"));
            }
            @Override public void onError(Throwable t) {
                SwingUtilities.invokeLater(() -> solarLog.append("error\n"));
            }
            @Override public void onCompleted() {
                SwingUtilities.invokeLater(() -> solarLog.append("done\n"));
            }
        });
    }

    private void stopSolar() {
        ManagedChannel ch = solarChannel.getAndSet(null);
        if (ch != null) ch.shutdownNow();
    }

    private void sendBatterySamples() {
    String addr = batteryAddr.getText();
    if (!addr.contains(":")) return;
    new Thread(() -> {
        try {
            String[] hp = addr.split(":");
            ManagedChannel base = ManagedChannelBuilder.forAddress(hp[0], Integer.parseInt(hp[1])).usePlaintext().build();
            Channel ch = channelWithAuth(base);
            BatteryServiceGrpc.BatteryServiceStub stub = BatteryServiceGrpc.newStub(ch).withDeadlineAfter(3, TimeUnit.SECONDS);

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

            StreamObserver<ChargeSummary> out = new StreamObserver<>() {
                @Override public void onNext(ChargeSummary s) {
                    SwingUtilities.invokeLater(() -> batteryAvg.setText(s.getAvgAmps() + "A " + s.getAvgVolts() + "V"));
                }
                @Override public void onError(Throwable t) {
                    SwingUtilities.invokeLater(() -> batteryAvg.setText("-"));
                    latch.countDown();
                }
                @Override public void onCompleted() {
                    latch.countDown();
                }
            };

            StreamObserver<ChargeSample> in = stub.sendChargeSamples(out);
            in.onNext(ChargeSample.newBuilder().setAmps(5).setVolts(12).build());
            in.onNext(ChargeSample.newBuilder().setAmps(6).setVolts(12).build());
            in.onNext(ChargeSample.newBuilder().setAmps(7).setVolts(12).build());
            in.onCompleted();

            latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
            base.shutdownNow();
        } catch (Exception ignored) { }
    }).start();
}


    private void connectBatteryBidi() {
        stopBattery();
        String addr = batteryAddr.getText();
        if (!addr.contains(":")) return;
        String[] hp = addr.split(":");
        ManagedChannel base = ManagedChannelBuilder.forAddress(hp[0], Integer.parseInt(hp[1])).usePlaintext().build();
        batteryChannel.set(base);
        Channel ch = channelWithAuth(base);
        BatteryServiceGrpc.BatteryServiceStub stub = BatteryServiceGrpc.newStub(ch).withDeadlineAfter(10, TimeUnit.SECONDS);
        StreamObserver<ChargeStatus> out = new StreamObserver<>() {
            @Override public void onNext(ChargeStatus s) { SwingUtilities.invokeLater(() -> batterySoc.setText(s.getCurrentSocPercent() + "%")); }
            @Override public void onError(Throwable t) { SwingUtilities.invokeLater(() -> batterySoc.setText("-")); }
            @Override public void onCompleted() { }
        };
        StreamObserver<ChargeRequest> in = stub.chargeCycle(out);
        batteryReq.set(in);
    }

    private void sendTarget(float v) {
        StreamObserver<ChargeRequest> in = batteryReq.get();
        if (in != null) in.onNext(ChargeRequest.newBuilder().setTargetSocPercent(v).build());
    }

    private void stopBattery() {
        StreamObserver<ChargeRequest> in = batteryReq.getAndSet(null);
        if (in != null) in.onCompleted();
        ManagedChannel ch = batteryChannel.getAndSet(null);
        if (ch != null) ch.shutdownNow();
    }

    private void fetchPrice(JLabel output) {
        String addr = gridAddr.getText();
        if (!addr.contains(":")) return;
        String[] hp = addr.split(":");
        ManagedChannel base = ManagedChannelBuilder.forAddress(hp[0], Integer.parseInt(hp[1])).usePlaintext().build();
        Channel ch = channelWithAuth(base);
        GridServiceGrpc.GridServiceBlockingStub stub = GridServiceGrpc.newBlockingStub(ch).withDeadlineAfter(1500, TimeUnit.MILLISECONDS);
        SpotPrice p = stub.getSpotPrice(Empty.getDefaultInstance());
        output.setText(p.getEurPerKwh() + " EUR/kWh");
        base.shutdownNow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ControllerGui().setVisible(true));
    }
}

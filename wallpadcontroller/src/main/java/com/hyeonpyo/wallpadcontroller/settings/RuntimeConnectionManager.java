package com.hyeonpyo.wallpadcontroller.settings;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;

import com.hyeonpyo.wallpadcontroller.elfin.ElfinCommandService;
import com.hyeonpyo.wallpadcontroller.elfin.ElfinReceiveService;
import com.hyeonpyo.wallpadcontroller.ew11.Ew11Transport;
import com.hyeonpyo.wallpadcontroller.properties.Ew11TransportType;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Owns replaceable MQTT/UDP/reboot resources. A failed candidate never replaces the active set. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeConnectionManager implements Ew11Transport {
    /** Providers break the discovery → registry → receiver construction cycle. */
    private final ObjectProvider<ElfinReceiveService> receiver;
    private final ObjectProvider<ElfinCommandService> commandService;
    private final ScheduledExecutorService rebootExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ew11-reboot"); thread.setDaemon(true); return thread;
    });
    private volatile MqttClient mqtt;
    private volatile ConnectionSettings settings;
    private volatile DatagramSocket udpReceiver;
    private volatile DatagramSocket udpSender;
    private volatile Thread udpThread;
    private volatile ScheduledFuture<?> rebootTask;
    private final AtomicBoolean udpRunning = new AtomicBoolean();
    private volatile String lastError = "";

    public synchronized void apply(ConnectionSettings next) {
        MqttClient candidateMqtt = null;
        DatagramSocket candidateReceiver = null;
        DatagramSocket candidateSender = null;
        boolean reuseReceiver = false;
        try {
            candidateMqtt = connect(next);
            subscribeMqtt(candidateMqtt, next); // HA commands are always MQTT, regardless of EW11 transport.
            ConnectionSettings previousSettings = settings;
            reuseReceiver = next.transport() == Ew11TransportType.UDP && udpReceiver != null
                    && previousSettings != null && previousSettings.transport() == Ew11TransportType.UDP
                    && previousSettings.udpListenPort() == next.udpListenPort()
                    && previousSettings.udpBufferSize() == next.udpBufferSize();
            if (next.transport() == Ew11TransportType.UDP) {
                candidateSender = new DatagramSocket();
                InetAddress.getByName(next.udpSendHost()); // validate before replacing anything
                if (!reuseReceiver) {
                    candidateReceiver = new DatagramSocket(next.udpListenPort());
                    candidateReceiver.setSoTimeout(1000);
                }
            }

            MqttClient previousMqtt = mqtt;
            DatagramSocket previousReceiver = udpReceiver;
            DatagramSocket previousSender = udpSender;
            mqtt = candidateMqtt;
            settings = next;
            udpSender = candidateSender;
            if (next.transport() == Ew11TransportType.UDP) {
                if (!reuseReceiver) {
                    stopUdp();
                    udpReceiver = candidateReceiver;
                    startUdp(next, candidateReceiver);
                }
            } else {
                stopUdp();
                udpReceiver = null;
            }
            scheduleReboot(next);
            close(previousMqtt);
            if (!reuseReceiver) close(previousReceiver);
            close(previousSender);
            lastError = "";
            log.info("통신 설정 즉시 적용: MQTT={}, EW11={}", next.mqttHost(), next.transport());
        } catch (Exception e) {
            close(candidateMqtt); close(candidateReceiver); close(candidateSender);
            lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.error("새 통신 설정 적용 실패. 기존 연결은 유지합니다: {}", lastError, e);
            throw new IllegalStateException("연결 적용 실패: " + lastError, e);
        }
    }

    private MqttClient connect(ConnectionSettings s) throws MqttException {
        MqttClient client = new MqttClient("tcp://" + s.mqttHost() + ":" + s.mqttPort(), s.mqttClientId(), new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true); options.setAutomaticReconnect(true); options.setConnectionTimeout(5);
        if (!s.mqttUsername().isBlank()) options.setUserName(s.mqttUsername());
        if (!s.mqttPassword().isBlank()) options.setPassword(s.mqttPassword().toCharArray());
        client.connect(options);
        return client;
    }

    private void subscribeMqtt(MqttClient client, ConnectionSettings s) throws MqttException {
        client.setCallback(callback());
        client.subscribe(s.haTopic() + "/command/#", 0);
        if (s.transport() == Ew11TransportType.MQTT) client.subscribe(s.ew11MqttReceiveTopic(), 0);
    }

    private MqttCallback callback() {
        return new MqttCallback() {
            @Override public void connectionLost(Throwable cause) { lastError = "MQTT 연결 끊김: " + cause.getMessage(); }
            @Override public void messageArrived(String topic, MqttMessage message) {
                ConnectionSettings current = settings;
                if (current != null && current.transport() == Ew11TransportType.MQTT && topic.equals(current.ew11MqttReceiveTopic())) receiver.getObject().publishDeviceState(message.getPayload());
                else if (current != null && topic.startsWith(current.haTopic() + "/command/")) commandService.getObject().sendCommand(topic, message);
            }
            @Override public void deliveryComplete(IMqttDeliveryToken token) { }
        };
    }

    private void startUdp(ConnectionSettings current, DatagramSocket socket) {
        udpRunning.set(true);
        udpThread = new Thread(() -> {
            byte[] buffer = new byte[current.udpBufferSize()];
            while (udpRunning.get() && socket != null && !socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    receiver.getObject().publishDeviceState(java.util.Arrays.copyOf(packet.getData(), packet.getLength()));
                } catch (java.net.SocketTimeoutException ignored) { }
                catch (Exception e) { if (udpRunning.get()) lastError = "UDP 수신 오류: " + e.getMessage(); }
            }
        }, "ew11-udp-receiver");
        udpThread.setDaemon(true); udpThread.start();
    }

    private void scheduleReboot(ConnectionSettings current) {
        if (rebootTask != null) rebootTask.cancel(false);
        rebootTask = null;
        if (!current.rebootEnabled()) return;
        long delay = Math.max(1000, Duration.parse(current.rebootInterval()).toMillis());
        rebootTask = rebootExecutor.scheduleWithFixedDelay(() -> rebootEw11(current), delay, delay, TimeUnit.MILLISECONDS);
    }

    private void rebootEw11(ConnectionSettings current) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create("http://" + current.rebootHost() + "/cmd?command=REBOOT").toURL().openConnection();
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
            if (!current.rebootUsername().isBlank()) {
                String basic = Base64.getEncoder().encodeToString((current.rebootUsername() + ":" + current.rebootPassword()).getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + basic);
            }
            int status = connection.getResponseCode(); connection.disconnect();
            if (status >= 400) throw new IllegalStateException("HTTP " + status);
        } catch (Exception e) { lastError = "EW11 재부팅 요청 실패: " + e.getMessage(); log.warn(lastError); }
    }

    public boolean publish(String topic, byte[] payload, int qos, boolean retained) {
        try {
            MqttClient client = mqtt;
            if (client == null || !client.isConnected()) return false;
            MqttMessage message = new MqttMessage(payload); message.setQos(qos); message.setRetained(retained); client.publish(topic, message); return true;
        } catch (Exception e) { lastError = "MQTT 발행 실패: " + e.getMessage(); return false; }
    }

    @Override public void send(byte[] payload) {
        ConnectionSettings current = settings;
        if (current == null) { log.warn("통신 설정이 아직 적용되지 않았습니다."); return; }
        if (current.transport() == Ew11TransportType.MQTT) { publish(current.ew11MqttSendTopic(), payload, 0, false); return; }
        try {
            DatagramSocket socket = udpSender;
            if (socket == null) throw new IllegalStateException("UDP 송신 소켓이 없습니다.");
            socket.send(new DatagramPacket(payload, payload.length, InetAddress.getByName(current.udpSendHost()), current.udpSendPort()));
        } catch (Exception e) { lastError = "UDP 송신 실패: " + e.getMessage(); log.error(lastError, e); }
    }

    public boolean isConnected() { MqttClient client = mqtt; return client != null && client.isConnected(); }
    public String lastError() { return lastError; }
    public ConnectionSettings activeSettings() { return settings; }
    private void stopUdp() { udpRunning.set(false); close(udpReceiver); if (udpThread != null) try { udpThread.join(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private static void close(MqttClient client) { if (client != null) try { if (client.isConnected()) client.disconnect(); client.close(); } catch (Exception ignored) { } }
    private static void close(DatagramSocket socket) { if (socket != null && !socket.isClosed()) socket.close(); }
    @PreDestroy void stop() { if (rebootTask != null) rebootTask.cancel(false); rebootExecutor.shutdownNow(); stopUdp(); close(mqtt); close(udpSender); }
}

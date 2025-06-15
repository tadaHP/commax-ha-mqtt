package com.hyeonpyo.wallpadcontroller.mqtt.sender;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.mqtt.sender.record.MqttPendingMessage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttPublishWorker {

    private final MqttClient mqttClient;
    private final BlockingQueue<MqttPendingMessage> queue = new LinkedBlockingQueue<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MQTT-Publisher-Worker");
        t.setDaemon(true);
        return t;
    });

    private long reconnectBackoffMillis = 100;

    @PostConstruct
    public void start() {
        executor.scheduleWithFixedDelay(this::processNextMessage, 0, 100, TimeUnit.MILLISECONDS);
    }

    public void enqueue(MqttPendingMessage message) {
        queue.offer(message);
        log.debug("📥 MQTT 발행 대기열 추가됨: {}", message.topic());
    }

    private void processNextMessage() {
        try {
            MqttPendingMessage msg = queue.poll();
            if (msg == null) return; // 보낼 메시지가 없음

            if (mqttClient.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(msg.payload());
                mqttMessage.setQos(msg.qos());
                mqttMessage.setRetained(msg.retained());

                mqttClient.publish(msg.topic(), mqttMessage);
                log.debug("📤 MQTT 발행 완료: {}", msg.topic());

                reconnectBackoffMillis = 100; // 정상 발행되면 백오프 초기화

            } else {
                log.warn("⚠️ MQTT 미연결 상태 - 메시지를 큐에 재추가 후 대기: {}", msg.topic());
                queue.offer(msg); // 다시 큐로
                Thread.sleep(reconnectBackoffMillis); // 점진적 대기
                reconnectBackoffMillis = Math.min(reconnectBackoffMillis + 100, 5000); // 최대 5초
            }
        } catch (Exception e) {
            log.error("❌ MQTT 발행 실패 - 재시도 예정", e);
            try {
                Thread.sleep(reconnectBackoffMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            reconnectBackoffMillis = Math.min(reconnectBackoffMillis + 100, 5000);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            log.info("🛑 MQTT Publisher Worker 종료 중...");
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("⚠️ MQTT Publisher Worker 강제 종료");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("❌ MQTT Publisher 종료 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
        }
    }
}
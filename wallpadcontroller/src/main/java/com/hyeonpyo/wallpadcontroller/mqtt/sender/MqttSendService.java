package com.hyeonpyo.wallpadcontroller.mqtt.sender;

import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.mqtt.sender.record.MqttPendingMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttSendService {

    private final MqttPublisherWorker mqttPublisherWorker;

    public void publish(String topic, String payload, int qos) {
        this.publish(topic, payload, qos, false);
    }

    public void publish(String topic, String payload, int qos, boolean retained) {
        if (topic == null || topic.trim().isEmpty()) {
            log.warn("⚠️ MQTT 발행 실패: 토픽이 null이거나 비어있습니다.");
            return;
        }
        if (payload == null) {
            log.warn("⚠️ MQTT 발행 실패: 페이로드가 null입니다. (Topic: {})", topic);
            return;
        }

        mqttPublisherWorker.enqueue(new MqttPendingMessage(topic, payload, qos, retained));
    }
}

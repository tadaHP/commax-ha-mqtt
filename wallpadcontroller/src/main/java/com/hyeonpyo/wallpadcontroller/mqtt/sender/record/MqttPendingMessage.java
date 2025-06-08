package com.hyeonpyo.wallpadcontroller.mqtt.sender.record;

import java.nio.charset.StandardCharsets;

public record MqttPendingMessage(
    String topic,
    byte[] payload,
    int qos,
    boolean retained
) {
    public MqttPendingMessage(String topic, String payload, int qos, boolean retained) {
        this(topic, payload.getBytes(StandardCharsets.UTF_8), qos, retained);
    }
}
package com.hyeonpyo.wallpadcontroller.mqtt.sender.record;

public record MqttPendingMessage(String topic, String payload, int qos, boolean retained) {}

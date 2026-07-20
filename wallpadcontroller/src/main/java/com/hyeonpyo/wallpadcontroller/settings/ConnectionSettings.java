package com.hyeonpyo.wallpadcontroller.settings;

import com.hyeonpyo.wallpadcontroller.properties.Ew11TransportType;

/** Runtime settings persisted in app_setting. Passwords are only present internally. */
public record ConnectionSettings(
        String mqttHost, int mqttPort, String mqttClientId, String mqttUsername, String mqttPassword, String haTopic,
        Ew11TransportType transport, String ew11MqttSendTopic, String ew11MqttReceiveTopic,
        String udpSendHost, int udpSendPort, int udpListenPort, int udpBufferSize,
        boolean rebootEnabled, String rebootHost, String rebootUsername, String rebootPassword, String rebootInterval) { }

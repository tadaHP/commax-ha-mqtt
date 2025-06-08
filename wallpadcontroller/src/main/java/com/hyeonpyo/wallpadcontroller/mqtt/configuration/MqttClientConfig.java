package com.hyeonpyo.wallpadcontroller.mqtt.configuration;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MqttClientConfig {
    @Bean
    public MqttClient mqttClient(MqttProperties mqttProperties) {
        try {
            MqttClient client = new MqttClient(mqttProperties.getBroker(), mqttProperties.getClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(mqttProperties.getUsername());
            options.setPassword(mqttProperties.getPassword().toCharArray());
            options.setAutomaticReconnect(true);

            log.info("⌛ MQTT 브로커 연결 시도 중: {}", mqttProperties.getBroker());
            client.connect(options);
            log.info("✅ MQTT 브로커 연결 성공: {}", mqttProperties.getBroker());
            return client;
        } catch (MqttException e) {
            log.error("❌ MQTT 연결 실패");
            throw new RuntimeException("MQTT Broker 연결에 실패했습니다.", e);
        }
    }
}

package com.hyeonpyo.wallpadcontroller.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.parser.PacketParser;
import com.hyeonpyo.wallpadcontroller.parser.type.ParsedPacketDto;
import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttService implements MqttCallback {

    private final MqttProperties mqttProperties;
    private final String EW11_TOPIC = "ew11/#";
    private final PacketParser packetParser;

    private MqttClient mqttClient;

    @PostConstruct
    public void init() {
        connect();
    }

    public void connect() {
        try {
            mqttClient = new MqttClient(mqttProperties.getBroker(), mqttProperties.getClientId(), new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(mqttProperties.getUsername());
            options.setPassword(mqttProperties.getPassword().toCharArray());

            mqttClient.setCallback(this);
            mqttClient.connect(options);

            log.info("✅ MQTT 브로커 연결 성공: {}", mqttProperties.getBroker());
            
            String haTopic = mqttProperties.getHaTopic() + "/#";

            mqttClient.subscribe(EW11_TOPIC, 0);
            log.info("📥 MQTT 구독 완료: {}", EW11_TOPIC);

            mqttClient.subscribe(haTopic, 0);
            log.info("📥 MQTT 구독 완료: {}", haTopic);

        } catch (MqttException e) {
            log.error("❌ MQTT 연결 실패", e);
        }
    }

    public void publish(String topic, String payload) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(0);
                mqttClient.publish(topic, message);
                log.info("📤 MQTT 발행: {} → {}", topic, payload);
            } else {
                log.warn("MQTT 클라이언트가 연결되어 있지 않습니다.");
            }
        } catch (MqttException e) {
            log.error("❌ MQTT 발행 오류", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("⚠️ MQTT 연결 끊김: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        byte[] payloadBytes = message.getPayload();
        StringBuilder hexBuilder = new StringBuilder();

        for (byte b : payloadBytes) {
            hexBuilder.append(String.format("%02X ", b));
        }

        String hexWithSpaces = hexBuilder.toString().trim();
        String hex = hexWithSpaces.replace(" ", "");

        log.info("📩 MQTT 수신: {} → HEX: {}", topic, hexWithSpaces);
        packetParser.parse(hex);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 메시지 발행 완료 콜백
    }
}

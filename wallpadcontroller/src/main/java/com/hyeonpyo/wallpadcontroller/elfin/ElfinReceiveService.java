package com.hyeonpyo.wallpadcontroller.elfin;

import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.device.state.DeviceStateManager;
import com.hyeonpyo.wallpadcontroller.mqtt.sender.MqttSendService;
import com.hyeonpyo.wallpadcontroller.parser.PacketParser;
import com.hyeonpyo.wallpadcontroller.parser.commax.type.PacketKind;
import com.hyeonpyo.wallpadcontroller.parser.commax.type.ParsedPacket;
import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElfinReceiveService { // orchestraction? 

    private final PacketParser packetParser;
    private final MqttProperties mqttProperties;
    private final MqttSendService mqttSendService;
    private final DeviceStateManager deviceStateManager;

    @PostConstruct
    public void publishOnlineStatus() {
        mqttSendService.publish(mqttProperties.getHaTopic() + "/status", "online", 1, true);
    }

    @PreDestroy
    public void publishOfflineStatus() {
        mqttSendService.publish(mqttProperties.getHaTopic() + "/status", "offline", 1, true);
    }

    public void publishCommax(MqttMessage message) {
        byte[] payloadBytes = message.getPayload();
        StringBuilder hexBuilder = new StringBuilder();

        for (byte b : payloadBytes) {
            hexBuilder.append(String.format("%02X ", b));
        }

        String hexWithSpaces = hexBuilder.toString().trim();
        String hex = hexWithSpaces.replace(" ", "");

        // log.info("\uD83D\uDCE9 MQTT 수신: {} → HEX: {}", "ew11/recv", hexWithSpaces);

        List<ParsedPacket> multiple = packetParser.parseMultiple(hex);

        for (ParsedPacket parsedPacket : multiple) {
            if(parsedPacket.getDeviceName().equals("Fan")){
                log.info("\uD83D\uDCE9 MQTT 수신: {} → HEX: {}", "ew11/recv", hexWithSpaces);
                log.info("장치-번호: {}-{}", parsedPacket.getDeviceName(), parsedPacket.getDeviceIndex());
                log.info("종류: {}", parsedPacket.getKind());
                log.info("패킷: {}", parsedPacket.getParsedState().toJson());
            }

            if (parsedPacket.getKind() == PacketKind.STATE) {
                deviceStateManager.updateState(parsedPacket.getDeviceName(), parsedPacket.getDeviceIndex(), parsedPacket.getParsedState().toMap());
            }
        }
    }
}

package com.hyeonpyo.wallpadcontroller.elfin;

import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.mqtt.sender.MqttSendService;
import com.hyeonpyo.wallpadcontroller.parser.PacketParser;
import com.hyeonpyo.wallpadcontroller.parser.commax.type.ParsedPacket;
import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElfinReceiveService {

    private final PacketParser packetParser;
    private final MqttProperties mqttProperties;
    private final MqttSendService mqttSendService;

    public void publishCommax(MqttMessage message) {
        byte[] payloadBytes = message.getPayload();
        StringBuilder hexBuilder = new StringBuilder();

        for (byte b : payloadBytes) {
            hexBuilder.append(String.format("%02X ", b));
        }

        String hexWithSpaces = hexBuilder.toString().trim();
        String hex = hexWithSpaces.replace(" ", "");

        log.info("\uD83D\uDCE9 MQTT 수신: {} → HEX: {}", "ew11/recv", hexWithSpaces);

        List<ParsedPacket> multiple = packetParser.parseMultiple(hex);

        for (ParsedPacket parsedPacket : multiple) {
            log.info("장치: {}", parsedPacket.getDeviceName());
            log.info("종류: {}", parsedPacket.getKind());
            log.info("패킷: {}", parsedPacket.getParsedState().toJson());

            // if (parsedPacket.getKind() == PacketKind.STATE) {
            //     String jsonPayload = parsedPacket.getParsedState().toJson();
            //     String statusTopic = mqttProperties.getHaTopic() + "/" + parsedPacket.getDeviceName() + "/status";
            //     mqttSendService.publish(statusTopic, jsonPayload, 1);
            // }
        }
    }
}

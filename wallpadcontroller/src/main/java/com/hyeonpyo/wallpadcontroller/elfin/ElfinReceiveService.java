package com.hyeonpyo.wallpadcontroller.elfin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.device.state.DeviceStateManager;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceKey;
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
    private final DeviceEntityRepository deviceEntityRepository;

    private final Map<String, DeviceEntity> registeredDevices = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
        mqttSendService.publish(mqttProperties.getHaTopic() + "/status", "online", 1, true);

        List<DeviceEntity> allDevices = deviceEntityRepository.findAll();
        for (DeviceEntity device : allDevices) {
            registeredDevices.put(device.getUniqueId(), device);
        }
        log.info("📦 등록된 기기 {}개 로드 완료", registeredDevices.size());
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
            String deviceType = parsedPacket.getDeviceName();
            int deviceIndex = parsedPacket.getDeviceIndex();
            PacketKind kind = parsedPacket.getKind();

            if (kind == PacketKind.STATE) {
                String uniqueId = "commax_" + deviceType + "_" + deviceIndex;

                // ✅ Map에 없을 때만 DB 등록 및 Map 추가
                if (!registeredDevices.containsKey(uniqueId)) {
                    try {
                        DeviceEntity entity = DeviceEntity.builder()
                                .uniqueId(uniqueId)
                                .objectId(uniqueId)
                                .type(DeviceKey.valueOf(deviceType))
                                .index(deviceIndex)
                                .build();
                        deviceEntityRepository.save(entity);
                        registeredDevices.put(uniqueId, entity);
                        log.info("📥 등록된 새 기기: {} (index: {})", uniqueId, deviceIndex);
                    } catch (Exception e) {
                        log.error("❌ 기기 등록 실패 - {}", uniqueId, e);
                    }
                }

                // 상태 업데이트
                deviceStateManager.updateState(
                        deviceType,
                        deviceIndex,
                        parsedPacket.getParsedState().toMap()
                );
            }
        }
    }
}

package com.hyeonpyo.wallpadcontroller.elfin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.command.CommandQueue;
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
@DependsOn("evParsingStructureMigration")
@RequiredArgsConstructor
public class ElfinReceiveService {

    private final PacketParser packetParser;
    private final MqttProperties mqttProperties;
    private final MqttSendService mqttSendService;
    private final DeviceStateManager deviceStateManager;
    private final DeviceEntityRepository deviceEntityRepository;
    private final CommandQueue commandQueue;

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

    /** DB의 전체 등록 기기를 읽어 인메모리 캐시를 덮어씁니다. (웹/API에서 used 등 변경 후 호출) */
    public void reloadRegisteredDevicesFromRepository() {
        registeredDevices.clear();
        for (DeviceEntity device : deviceEntityRepository.findAll()) {
            registeredDevices.put(device.getUniqueId(), device);
        }
        log.info("📦 등록 기기 캐시 전체 갱신: {}개", registeredDevices.size());
    }

    @PreDestroy
    public void publishOfflineStatus() {
        mqttSendService.publish(mqttProperties.getHaTopic() + "/status", "offline", 1, true);
    }

    public void publishDeviceState(byte[] payloadBytes) {
        StringBuilder hexBuilder = new StringBuilder();

        for (byte b : payloadBytes) {
            hexBuilder.append(String.format("%02X ", b));
        }

        String hexWithSpaces = hexBuilder.toString().trim();
        String hex = hexWithSpaces.replace(" ", "");

        for (int offset = 0; offset + 8 <= payloadBytes.length; offset += 8) {
            byte[] packet = java.util.Arrays.copyOfRange(payloadBytes, offset, offset + 8);
            commandQueue.onPacketReceived(packet);
        }

        List<ParsedPacket> multiple = packetParser.parseMultiple(hex);

         for (ParsedPacket parsedPacket : multiple) {
            String deviceType = parsedPacket.getDeviceName();
            int deviceIndex = parsedPacket.getDeviceIndex();
            PacketKind kind = parsedPacket.getKind();

            if (kind == PacketKind.STATE) {
                String uniqueId = mqttProperties.getHaTopic() + "_" + deviceType + "_" + deviceIndex;

                if (!registeredDevices.containsKey(uniqueId)) {
                    try {
                        DeviceKey key = DeviceKey.valueOf(deviceType);
                        DeviceEntity entity = DeviceEntity.builder()
                                .uniqueId(uniqueId)
                                .objectId(defaultObjectId(key, deviceIndex, uniqueId))
                                .type(key)
                                .index(deviceIndex)
                                .used(false)
                                .build();
                        DeviceEntity saved = deviceEntityRepository.save(entity);
                        registeredDevices.put(uniqueId, saved);
                        log.info("📥 등록된 새 기기(기본 비활성): {} (index: {})", uniqueId, deviceIndex);
                    } catch (Exception e) {
                        log.error("❌ 기기 등록 실패 - {}", uniqueId, e);
                    }
                }

                DeviceEntity current = registeredDevices.get(uniqueId);
                if (current != null && current.isMqttPublished()) {
                    deviceStateManager.updateState(
                            deviceType,
                            deviceIndex,
                            parsedPacket.getParsedState().toMap()
                    );
                    commandQueue.onStateUpdated(
                            deviceType,
                            deviceIndex,
                            parsedPacket.getParsedState().toMap()
                    );
                }
            }
        }
    }

    /** HA 기본 entity_id 접두에 맞춘 object_id (EV는 sensor.ev_status_{index}). */
    private static String defaultObjectId(DeviceKey type, int index, String uniqueId) {
        if (type == DeviceKey.EV) {
            return "ev_status_" + index;
        }
        return uniqueId;
    }
}

package com.hyeonpyo.wallpadcontroller.elfin;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.device.state.DeviceStateManager;
import com.hyeonpyo.wallpadcontroller.domain.builder.CommandBuilder;
import com.hyeonpyo.wallpadcontroller.ew11.Ew11Transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElfinCommandService {

    private final CommandBuilder commandBuilder;
    private final Ew11Transport ew11Transport;
    private final DeviceStateManager deviceStateManager;

    public void sendCommand(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.info("📨 HA 명령 수신 - topic: {}, payload: {}", topic, payload);

        String[] parts = topic.split("/");
        if (parts.length < 4) {
            log.warn("⚠️ 잘못된 topic 형식: {}", topic);
            return;
        }

        String deviceInfo = parts[2];
        String field = parts[3];

        String deviceType = deviceInfo.replaceAll("\\d+$", "");
        String indexStr = deviceInfo.replace(deviceType, "");
        final int deviceIndex;
        try {
            deviceIndex = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            log.warn("⚠️ device index 파싱 실패: {}", deviceInfo);
            return;
        }

        commandBuilder.build(deviceType, deviceIndex, field, payload)
            .ifPresentOrElse(packet -> {
                String collect = IntStream.range(0, packet.length)
                        .mapToObj(i -> String.format("%02X", packet[i]))
                        .collect(Collectors.joining(" "));
                log.info("📦 생성된 HEX 패킷: {}", collect);
                ew11Transport.send(packet);
                deviceStateManager.setTargetState(deviceType, deviceIndex, field, payload);
            }, () -> {
                log.warn("⚠️ 패킷 생성 실패 - deviceType: {}, index: {}, field: {}, payload: {}", deviceType, deviceIndex, field, payload);
            });
    }

}

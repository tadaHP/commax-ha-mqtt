package com.hyeonpyo.wallpadcontroller.domain.device;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hyeonpyo.wallpadcontroller.device.state.DeviceStateManager;
import com.hyeonpyo.wallpadcontroller.elfin.ElfinReceiveService;
import com.hyeonpyo.wallpadcontroller.mqtt.discovery.MqttDiscoveryPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRegistryService {

    private final DeviceEntityRepository deviceEntityRepository;
    private final ElfinReceiveService elfinReceiveService;
    private final MqttDiscoveryPublisher mqttDiscoveryPublisher;
    private final DeviceStateManager deviceStateManager;

    /**
     * DB에 used 반영 → discovery/상태 정리 → 등록 기기 인메모리 캐시를 DB 기준으로 전부 다시 로드합니다.
     *
     * @param mode {@code on} / {@code off} / {@code legacy}(null)
     */
    @Transactional
    public DeviceEntity updateMqttPublicationMode(String uniqueId, String mode) {
        DeviceEntity device = deviceEntityRepository.findById(uniqueId)
                .orElseThrow(() -> new NoSuchElementException("unknown device: " + uniqueId));
        switch (mode) {
            case "on" -> device.setUsed(Boolean.TRUE);
            case "off" -> device.setUsed(Boolean.FALSE);
            case "legacy" -> device.setUsed(null);
            default -> throw new IllegalArgumentException("mode는 on, off, legacy 중 하나여야 합니다.");
        }
        DeviceEntity saved = deviceEntityRepository.save(device);
        mqttDiscoveryPublisher.syncDeviceDiscovery(saved);
        if (!saved.isMqttPublished()) {
            deviceStateManager.clearStateForDevice(saved.getType().name(), saved.getIndex());
        }
        elfinReceiveService.reloadRegisteredDevicesFromRepository();
        log.info("기기 MQTT 모드 저장 및 캐시 전체 갱신: {} → {}", uniqueId, saved.getUsed());
        return saved;
    }
}

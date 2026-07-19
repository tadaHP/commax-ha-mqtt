package com.hyeonpyo.wallpadcontroller.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.mqtt.discovery.MqttDiscoveryPublisher;

import lombok.RequiredArgsConstructor;

/** API consumed by the future settings page for opt-in device management. */
@RestController
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceEntityRepository deviceEntityRepository;
    private final MqttDiscoveryPublisher discoveryPublisher;

    @GetMapping("/api/devices")
    public List<DeviceEntity> list() {
        return deviceEntityRepository.findAll();
    }

    @PatchMapping("/api/devices/{uniqueId}/enabled")
    public ResponseEntity<DeviceEntity> setEnabled(@PathVariable String uniqueId,
            @RequestBody Map<String, Boolean> request) {
        Boolean enabled = request.get("enabled");
        if (enabled == null) return ResponseEntity.badRequest().build();

        return deviceEntityRepository.findById(uniqueId)
                .map(device -> {
                    device.setEnabled(enabled);
                    DeviceEntity saved = deviceEntityRepository.save(device);
                    if (saved.isEnabled()) discoveryPublisher.publish(saved);
                    else discoveryPublisher.revoke(saved);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

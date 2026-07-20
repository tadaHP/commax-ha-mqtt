package com.hyeonpyo.wallpadcontroller.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceRegistryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceRestController {

    private final DeviceRegistryService deviceRegistryService;

    /**
     * JSON: {@code { "uniqueId": "...", "mode": "on" | "off" | "legacy" }}.
     * POST와 PUT 모두 허용(일부 리버스 프록시에서 PUT이 막히는 경우 대비).
     */
    @RequestMapping(value = "/mqtt-mode", method = { RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<Map<String, Object>> updateMqttMode(@RequestBody Map<String, String> body) {
        String uniqueId = body != null ? body.get("uniqueId") : null;
        String mode = body != null ? body.get("mode") : null;
        if (uniqueId == null || uniqueId.isBlank() || mode == null || mode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "uniqueId와 mode가 필요합니다."));
        }
        try {
            DeviceEntity saved = deviceRegistryService.updateMqttPublicationMode(uniqueId.trim(), mode.trim());
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("ok", true);
            ok.put("message", "저장했습니다.");
            ok.put("uniqueId", saved.getUniqueId());
            ok.put("used", saved.getUsed());
            return ResponseEntity.ok(ok);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "기기를 찾을 수 없습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
}

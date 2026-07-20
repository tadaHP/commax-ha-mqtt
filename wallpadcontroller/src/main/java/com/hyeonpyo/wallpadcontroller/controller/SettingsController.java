package com.hyeonpyo.wallpadcontroller.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hyeonpyo.wallpadcontroller.mqtt.discovery.MqttDiscoveryPublisher;
import com.hyeonpyo.wallpadcontroller.settings.ConnectionSettings;
import com.hyeonpyo.wallpadcontroller.settings.RuntimeConnectionManager;
import com.hyeonpyo.wallpadcontroller.settings.SettingsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {
    private final SettingsService settingsService;
    private final RuntimeConnectionManager connections;
    private final MqttDiscoveryPublisher discoveryPublisher;

    @GetMapping
    public Map<String, Object> get() { return response(true, null); }

    @PutMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody Map<String, Object> input) {
        try {
            Map<String, String> values = new LinkedHashMap<>();
            input.forEach((key, value) -> values.put(key, value == null ? null : String.valueOf(value)));
            ConnectionSettings settings = settingsService.save(values); // DB is intentionally committed before applying.
            return apply(settings);
        } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(response(false, e.getMessage())); }
    }

    @PostMapping("/import-env")
    public ResponseEntity<Map<String, Object>> importEnvironment(@RequestParam(defaultValue = "false") boolean force) {
        if (!force) return ResponseEntity.badRequest().body(response(false, "환경 값을 다시 반영하려면 force=true이 필요합니다."));
        try { return apply(settingsService.initialize(true)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(response(false, e.getMessage())); }
    }

    private ResponseEntity<Map<String, Object>> apply(ConnectionSettings settings) {
        settingsService.applyProperties(settings);
        try {
            connections.apply(settings);
            discoveryPublisher.publishDiscovery();
            return ResponseEntity.ok(response(true, null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response(false, e.getMessage()));
        }
    }

    private Map<String, Object> response(boolean ok, String error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", ok); body.put("settings", settingsService.publicView());
        body.put("mqttConnected", connections.isConnected()); body.put("activeSettingsApplied", connections.activeSettings() != null);
        body.put("lastError", error == null ? connections.lastError() : error);
        body.put("environmentImportAvailable", true);
        return body;
    }
}

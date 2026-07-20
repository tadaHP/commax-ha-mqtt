package com.hyeonpyo.wallpadcontroller.settings;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.hyeonpyo.wallpadcontroller.mqtt.discovery.MqttDiscoveryPublisher;

@Component
@Order(20)
@RequiredArgsConstructor
public class SettingsBootstrapRunner implements CommandLineRunner {
    private final SettingsService settingsService; private final RuntimeConnectionManager connections; private final MqttDiscoveryPublisher discoveryPublisher;
    @Override public void run(String... args) {
        var settings = settingsService.initialize(false);
        settingsService.applyProperties(settings);
        try { connections.apply(settings); discoveryPublisher.publishDiscovery(); }
        catch (IllegalStateException e) { /* The web UI remains available to correct unreachable first-run settings. */ }
    }
}

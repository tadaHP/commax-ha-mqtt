package com.hyeonpyo.wallpadcontroller.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "heating.polling")
public class HeatingPollingProperties {
    private boolean enabled = true;
    private Duration interval = Duration.ofSeconds(5);
    private Duration deviceGap = Duration.ofMillis(100);
}

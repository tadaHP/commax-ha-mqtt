package com.hyeonpyo.wallpadcontroller.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private String broker;
    private String clientId;
    private String username;
    private String password;
    private String haTopic;
}

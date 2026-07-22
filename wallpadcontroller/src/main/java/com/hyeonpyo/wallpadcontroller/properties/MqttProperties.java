package com.hyeonpyo.wallpadcontroller.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private String host;
    private int port;   
    private String clientId;
    private String username;
    private String password;
    private String haTopic;
    /** HA MQTT integration의 birth message. HA 재기동 후 상태 재발행 감지에 사용한다. */
    private BirthMessage birthMessage = new BirthMessage();

    @Data
    public static class BirthMessage {
        private String topic = "homeassistant/status";
        private String payload = "online";
    }

    public String getBrokerUrl() {
        return String.format("tcp://%s:%d", host, port);
    }
}

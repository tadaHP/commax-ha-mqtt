package com.hyeonpyo.wallpadcontroller.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "ew11")
public class Ew11Properties {
    private String transport = "mqtt";
    private Mqtt mqtt = new Mqtt();
    private Udp udp = new Udp();

    @Data
    public static class Mqtt {
        private String sendTopic = "ew11/send";
        private String receiveTopic = "ew11/recv";
    }

    @Data
    public static class Udp {
        private String host = "127.0.0.1";
        private int port = 60000;
        private int listenPort = 60001;
        private int bufferSize = 2048;
    }
}

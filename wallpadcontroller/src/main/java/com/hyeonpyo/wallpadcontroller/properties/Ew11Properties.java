package com.hyeonpyo.wallpadcontroller.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "ew11")
public class Ew11Properties {
    private Ew11TransportType transport = Ew11TransportType.MQTT;
    private Mqtt mqtt = new Mqtt();
    private Udp udp = new Udp();

    @Data
    public static class Mqtt {
        private String sendTopic = "ew11/send";
        private String receiveTopic = "ew11/recv";
    }

    @Data
    public static class Udp {
        private Send send = new Send();
        private Listen listen = new Listen();
    }

    @Data
    public static class Send {
        private String host = "127.0.0.1";
        private int port = 52493;
    }

    @Data
    public static class Listen {
        private int port = 54747;
        private int bufferSize = 512;
    }
}

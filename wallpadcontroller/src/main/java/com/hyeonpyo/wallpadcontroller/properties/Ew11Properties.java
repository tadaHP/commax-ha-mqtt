package com.hyeonpyo.wallpadcontroller.properties;

import java.time.Duration;

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
    private Reboot reboot = new Reboot();

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

    /** EW11 HTTP 재부팅 (UDP/MQTT 공통, 주기적으로 /cmd 호출) */
    @Data
    public static class Reboot {
        private boolean enabled = false;
        /** 재부팅 요청 대상 호스트 (EW11_REBOOT_HOST) */
        private String host = "";
        private String username = "";
        private String password = "";
        /** 주기 (예: 12h, 30m, 1d). 기본 12시간 */
        private Duration interval = Duration.ofHours(12);
    }
}

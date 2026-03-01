package com.hyeonpyo.wallpadcontroller.ew11;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.hyeonpyo.wallpadcontroller.properties.Ew11Properties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EW11 장치에 주기적으로 HTTP POST /cmd (CID 20003 재부팅) 요청을 보냅니다.
 * ew11.udp.send.host(EW11_UDP_SEND_HOST)를 호스트로 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ew11.reboot", name = "enabled", havingValue = "true")
public class Ew11RebootService {

    private static final String CMD_BODY = "msg={\"CID\":20003,\"PL\":{}}";
    private static final int HTTP_TIMEOUT_MS = 10_000;

    private final Ew11Properties ew11Properties;
    private final TaskScheduler taskScheduler;

    @PostConstruct
    public void scheduleReboot() {
        String username = ew11Properties.getReboot().getUsername();
        String password = ew11Properties.getReboot().getPassword();
        String host = ew11Properties.getReboot().getHost();
        if (host == null || host.isBlank()) {
            log.warn("⚠️ EW11 재부팅이 활성화되었으나 ew11.reboot.host(EW11_REBOOT_HOST)이 비어 있어 스케줄을 등록하지 않습니다.");
            return;
        }
        if (username == null || username.isBlank()) {
            log.warn("⚠️ EW11 재부팅이 활성화되었으나 ew11.reboot.username(EW11_REBOOT_USERNAME)이 비어 있어 스케줄을 등록하지 않습니다.");
            return;
        }

        String url = "http://" + host + "/cmd";
        var interval = ew11Properties.getReboot().getInterval();

        taskScheduler.scheduleAtFixedRate(
            () -> sendRebootRequest(url, username, password),
            interval
        );
        log.info("✅ EW11 주기 재부팅 스케줄 등록: {} → {} (주기: {})", url, username, interval);
    }

    private void sendRebootRequest(String url, String username, String password) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/json; charset=utf-8"));
            headers.set("Authorization", basicAuth(username, password));
            HttpEntity<String> entity = new HttpEntity<>(CMD_BODY, headers);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(java.time.Duration.ofMillis(HTTP_TIMEOUT_MS));
            factory.setReadTimeout(java.time.Duration.ofMillis(HTTP_TIMEOUT_MS));
            RestTemplate rest = new RestTemplate(factory);
            String response = rest.postForObject(url, entity, String.class);
            log.info("🔄 EW11 재부팅 요청 전송 완료: {} {}", url, response);
        } catch (Exception e) {
            log.error("❌ EW11 재부팅 요청 실패: {}", url, e);
        }
    }

    private static String basicAuth(String username, String password) {
        String cred = (username != null ? username : "") + ":" + (password != null ? password : "");
        return "Basic " + Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
    }
}

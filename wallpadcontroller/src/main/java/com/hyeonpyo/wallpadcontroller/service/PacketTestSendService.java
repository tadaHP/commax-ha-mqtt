package com.hyeonpyo.wallpadcontroller.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.ew11.Ew11Transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PacketTestSendService {

    private static final Pattern BYTE_HEX = Pattern.compile("(?i)[0-9A-F]{2}");

    private final Ew11Transport ew11Transport;

    /**
     * 8바이트 HEX 문자열을 파싱해 EW11(MQTT/UDP)로 그대로 전송합니다.
     *
     * @return 전송한 바이트 배열(로그·응답용)
     * @throws IllegalArgumentException 개수·형식이 맞지 않을 때
     */
    public byte[] parseAndSend(List<String> byteTokens) {
        if (byteTokens == null || byteTokens.size() != 8) {
            throw new IllegalArgumentException("바이트는 정확히 8개여야 합니다.");
        }
        byte[] out = new byte[8];
        for (int i = 0; i < 8; i++) {
            String t = byteTokens.get(i) == null ? "" : byteTokens.get(i).trim();
            if (!BYTE_HEX.matcher(t).matches()) {
                throw new IllegalArgumentException((i + 1) + "번째 바이트는 2자리 16진수(00~FF)여야 합니다.");
            }
            out[i] = (byte) Integer.parseInt(t, 16);
        }
        ew11Transport.send(out);
        log.info("패킷 테스트 송신: {}", formatHex(out));
        return out;
    }

    private static String formatHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 3);
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        return sb.toString();
    }
}

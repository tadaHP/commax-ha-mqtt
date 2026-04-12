package com.hyeonpyo.wallpadcontroller.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyeonpyo.wallpadcontroller.service.PacketTestSendService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/packet-test")
@RequiredArgsConstructor
public class PacketTestRestController {

    private final PacketTestSendService packetTestSendService;

    public record PacketTestSendRequest(List<String> bytes) {}

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody PacketTestSendRequest body) {
        try {
            byte[] sent = packetTestSendService.parseAndSend(body == null ? null : body.bytes());
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("ok", true);
            ok.put("hex", formatHex(sent));
            ok.put("length", sent.length);
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
    }

    private static String formatHex(byte[] data) {
        return IntStream.range(0, data.length)
                .mapToObj(i -> String.format("%02X", data[i] & 0xFF))
                .collect(Collectors.joining(" "));
    }
}

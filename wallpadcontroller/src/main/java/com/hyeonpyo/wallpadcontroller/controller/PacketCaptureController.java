package com.hyeonpyo.wallpadcontroller.controller;

import com.hyeonpyo.wallpadcontroller.service.PacketCaptureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequiredArgsConstructor
public class PacketCaptureController {

    private final PacketCaptureService packetCaptureService;

    @PostMapping("/capture/start")
    public ResponseEntity<Void> startCapture() {
        packetCaptureService.startCapture();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/capture/stop")
    public ResponseEntity<Void> stopCapture() {
        packetCaptureService.stopCapture();
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/capture/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return packetCaptureService.addEmitter();
    }
}

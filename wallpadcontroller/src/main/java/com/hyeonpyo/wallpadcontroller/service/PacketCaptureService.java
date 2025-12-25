package com.hyeonpyo.wallpadcontroller.service;

import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLog;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PacketCaptureService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private volatile boolean isCapturing = false;

    public void startCapture() {
        this.isCapturing = true;
    }

    public void stopCapture() {
        this.isCapturing = false;
    }

    public SseEmitter addEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        this.emitters.add(emitter);

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        emitter.onError(err -> this.emitters.remove(emitter));

        return emitter;
    }

    public void sendPacket(PacketLog packetLog) {
        if (!isCapturing) {
            return;
        }

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("packet")
                        .data(packetLog)); // Jackson이 JSON으로 변환
            } catch (IOException e) {
                emitter.complete();
            }
        });
    }
}

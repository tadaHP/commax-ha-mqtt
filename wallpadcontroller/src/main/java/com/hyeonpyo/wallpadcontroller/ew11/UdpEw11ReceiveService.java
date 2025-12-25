package com.hyeonpyo.wallpadcontroller.ew11;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.elfin.ElfinReceiveService;
import com.hyeonpyo.wallpadcontroller.properties.Ew11Properties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${ew11.transport:mqtt}'.equalsIgnoreCase('udp')")
public class UdpEw11ReceiveService {

    private final Ew11Properties ew11Properties;
    private final ElfinReceiveService elfinReceiveService;

    private DatagramSocket socket;
    private Thread receiverThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void start() {
        try {
            socket = new DatagramSocket(ew11Properties.getUdp().getListenPort());
            socket.setSoTimeout(1000);
            running.set(true);

            receiverThread = new Thread(this::receiveLoop, "ew11-udp-receiver");
            receiverThread.setDaemon(true);
            receiverThread.start();

            log.info("✅ EW11 UDP 수신 시작: port {}", ew11Properties.getUdp().getListenPort());
        } catch (Exception e) {
            log.error("❌ EW11 UDP 수신 초기화 실패", e);
        }
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (receiverThread != null) {
            try {
                receiverThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[ew11Properties.getUdp().getBufferSize()];

        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                byte[] payload = Arrays.copyOf(packet.getData(), packet.getLength());
                elfinReceiveService.publishDeviceState(payload);
            } catch (SocketTimeoutException e) {
                // allow graceful shutdown
            } catch (Exception e) {
                if (running.get()) {
                    log.error("❌ EW11 UDP 수신 오류", e);
                }
            }
        }
    }
}

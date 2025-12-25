package com.hyeonpyo.wallpadcontroller.ew11;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.properties.Ew11Properties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${ew11.transport:mqtt}'.equalsIgnoreCase('udp')")
public class UdpEw11Transport implements Ew11Transport {

    private final Ew11Properties ew11Properties;

    private DatagramSocket socket;
    private InetAddress address;

    @PostConstruct
    public void init() {
        try {
            address = InetAddress.getByName(ew11Properties.getUdp().getHost());
            socket = new DatagramSocket();
            log.info("✅ EW11 UDP 송신 초기화 완료: {}:{}", ew11Properties.getUdp().getHost(), ew11Properties.getUdp().getPort());
        } catch (Exception e) {
            log.error("❌ EW11 UDP 송신 초기화 실패", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void send(byte[] payload) {
        if (socket == null || socket.isClosed()) {
            log.warn("⚠️ EW11 UDP 송신 소켓이 준비되지 않았습니다.");
            return;
        }
        try {
            DatagramPacket packet = new DatagramPacket(payload, payload.length, address, ew11Properties.getUdp().getPort());
            socket.send(packet);
        } catch (Exception e) {
            log.error("❌ EW11 UDP 송신 실패", e);
        }
    }
}

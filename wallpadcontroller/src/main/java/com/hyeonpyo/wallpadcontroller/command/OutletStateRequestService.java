package com.hyeonpyo.wallpadcontroller.command;

import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.ew11.Ew11Transport;

import lombok.RequiredArgsConstructor;

/** Sends the two Commax Outlet state request packets without ACK tracking. */
@Service
@RequiredArgsConstructor
public class OutletStateRequestService {
    private final Ew11Transport ew11Transport;

    public void requestAll(int deviceIndex) {
        send(deviceIndex, 0x01);
        send(deviceIndex, 0x02);
    }

    private void send(int deviceIndex, int requestType) {
        byte[] packet = new byte[] {(byte) 0x79, (byte) deviceIndex, (byte) requestType, 0, 0, 0, 0, 0};
        int sum = 0;
        for (int i = 0; i < packet.length - 1; i++) sum += packet[i] & 0xFF;
        packet[7] = (byte) sum;
        ew11Transport.send(packet);
    }
}

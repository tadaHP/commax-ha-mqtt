package com.hyeonpyo.wallpadcontroller.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hyeonpyo.wallpadcontroller.ew11.Ew11Transport;

class PacketTestSendServiceTest {

    @Test
    void sendsEightBytes() {
        Ew11Transport transport = mock(Ew11Transport.class);
        SeenPacketMemoryStore seenPacketMemoryStore = mock(SeenPacketMemoryStore.class);
        PacketTestSendService svc = new PacketTestSendService(transport, seenPacketMemoryStore);
        List<String> in = Arrays.asList("A2", "01", "01", "00", "00", "15", "00", "B9");
        byte[] out = svc.parseAndSend(in);
        assertArrayEquals(new byte[] {(byte) 0xA2, 1, 1, 0, 0, 0x15, 0, (byte) 0xB9}, out);
        verify(transport).send(out);
        verify(seenPacketMemoryStore).recordOutboundPacket(org.mockito.Mockito.eq("A2 01 01 00 00 15 00 B9"), org.mockito.Mockito.any());
    }

    @Test
    void rejectsWrongCount() {
        Ew11Transport transport = mock(Ew11Transport.class);
        SeenPacketMemoryStore seenPacketMemoryStore = mock(SeenPacketMemoryStore.class);
        PacketTestSendService svc = new PacketTestSendService(transport, seenPacketMemoryStore);
        assertThrows(IllegalArgumentException.class, () -> svc.parseAndSend(Arrays.asList("A2", "01")));
    }

    @Test
    void rejectsInvalidHex() {
        Ew11Transport transport = mock(Ew11Transport.class);
        SeenPacketMemoryStore seenPacketMemoryStore = mock(SeenPacketMemoryStore.class);
        PacketTestSendService svc = new PacketTestSendService(transport, seenPacketMemoryStore);
        assertThrows(
                IllegalArgumentException.class,
                () -> svc.parseAndSend(Arrays.asList("A2", "01", "01", "00", "00", "GG", "00", "B9")));
    }
}

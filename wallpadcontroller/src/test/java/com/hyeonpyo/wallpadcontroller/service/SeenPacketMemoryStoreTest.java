package com.hyeonpyo.wallpadcontroller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacket;
import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacketDirection;
import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacketRepository;

@ExtendWith(MockitoExtension.class)
class SeenPacketMemoryStoreTest {

    @Mock
    private SeenPacketRepository seenPacketRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private SeenPacketMemoryStore store;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> consumer =
                    invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(org.mockito.ArgumentMatchers.any());
        when(seenPacketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recordSuccessPacket_insertsNewPacketOnce() {
        LocalDateTime seenAt = LocalDateTime.of(2026, 6, 12, 10, 0);
        boolean first = store.recordSuccessPacket("31 03 00 00 00 00 00 34", seenAt);
        boolean second = store.recordSuccessPacket("31 03 00 00 00 00 00 34", seenAt.plusSeconds(1));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(store.count()).isEqualTo(1);

        ArgumentCaptor<SeenPacket> captor = ArgumentCaptor.forClass(SeenPacket.class);
        verify(seenPacketRepository).save(captor.capture());
        assertThat(captor.getValue().getRawData()).isEqualTo("31 03 00 00 00 00 00 34");
        assertThat(captor.getValue().getDirection()).isEqualTo(SeenPacketDirection.INBOUND);
    }

    @Test
    void buildReceivedData_parsesStoredPacket() {
        LocalDateTime seenAt = LocalDateTime.of(2026, 6, 12, 10, 0);
        store.recordSuccessPacket("31 03 00 00 00 00 00 34", seenAt);

        var received = store.buildReceivedData();

        assertThat(received.get("31-2")).contains("03");
    }

    @Test
    void outboundPacketWithSameRawData_isTrackedSeparately() {
        LocalDateTime seenAt = LocalDateTime.of(2026, 6, 12, 10, 0);

        boolean inbound = store.recordSuccessPacket("31 03 00 00 00 00 00 34", seenAt);
        boolean outbound = store.recordOutboundPacket("31 03 00 00 00 00 00 34", seenAt.plusSeconds(1));

        assertThat(inbound).isTrue();
        assertThat(outbound).isTrue();
        assertThat(store.count()).isEqualTo(2);
        assertThat(store.list(null, SeenPacketDirection.INBOUND, 0, 20).totalElements()).isEqualTo(1);
        assertThat(store.list(null, SeenPacketDirection.OUTBOUND, 0, 20).totalElements()).isEqualTo(1);
    }
}

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
    }

    @Test
    void buildReceivedData_parsesStoredPacket() {
        LocalDateTime seenAt = LocalDateTime.of(2026, 6, 12, 10, 0);
        store.recordSuccessPacket("31 03 00 00 00 00 00 34", seenAt);

        var received = store.buildReceivedData();

        assertThat(received.get("31-2")).contains("03");
    }
}

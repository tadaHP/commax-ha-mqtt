package com.hyeonpyo.wallpadcontroller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacketDirection;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceKey;
import com.hyeonpyo.wallpadcontroller.dto.DevicePacketCoverageDeviceType;

@ExtendWith(MockitoExtension.class)
class PacketCoverageServiceTest {

    @Mock
    private DeviceTypeRepository deviceTypeRepository;

    @Mock
    private DeviceEntityRepository deviceEntityRepository;

    @Mock
    private SeenPacketMemoryStore seenPacketMemoryStore;

    @InjectMocks
    private PacketCoverageService packetCoverageService;

    @Test
    void getCoverageStatus_marksInboundStatePacketForRegisteredDevice() {
        LocalDateTime seenAt = LocalDateTime.of(2026, 6, 14, 10, 0);
        when(deviceTypeRepository.findAllWithFullStructure()).thenReturn(List.of(lightDeviceType()));
        when(deviceEntityRepository.findAll()).thenReturn(List.of(device(DeviceKey.Light, 1)));
        when(seenPacketMemoryStore.snapshot()).thenReturn(List.of(
                new SeenPacketEntry("B0 01 01 00 00 00 00 B2", "B0", SeenPacketDirection.INBOUND, seenAt, seenAt)));

        List<DevicePacketCoverageDeviceType> result = packetCoverageService.getCoverageStatus();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstances()).hasSize(1);
        assertThat(result.get(0).getInstances().get(0).getIndex()).isEqualTo(1);
        var stateKind = result.get(0).getInstances().get(0).getPacketKinds().stream()
                .filter(kind -> kind.getName().equals("state"))
                .findFirst()
                .orElseThrow();
        assertThat(stateKind.isInboundSeen()).isTrue();
        assertThat(stateKind.isOutboundSeen()).isFalse();
        assertThat(stateKind.getInboundPackets()).extracting("rawData")
                .containsExactly("B0 01 01 00 00 00 00 B2");
    }

    @Test
    void getCoverageStatus_marksOutboundCommandPacketSeparately() {
        LocalDateTime seenAt = LocalDateTime.of(2026, 6, 14, 10, 0);
        when(deviceTypeRepository.findAllWithFullStructure()).thenReturn(List.of(lightDeviceType()));
        when(deviceEntityRepository.findAll()).thenReturn(List.of(device(DeviceKey.Light, 2)));
        when(seenPacketMemoryStore.snapshot()).thenReturn(List.of(
                new SeenPacketEntry("31 02 01 00 00 00 00 34", "31", SeenPacketDirection.OUTBOUND, seenAt, seenAt)));

        List<DevicePacketCoverageDeviceType> result = packetCoverageService.getCoverageStatus();

        var commandKind = result.get(0).getInstances().get(0).getPacketKinds().stream()
                .filter(kind -> kind.getName().equals("command"))
                .findFirst()
                .orElseThrow();
        assertThat(commandKind.isInboundSeen()).isFalse();
        assertThat(commandKind.isOutboundSeen()).isTrue();
        assertThat(commandKind.getOutboundPackets()).extracting("rawData")
                .containsExactly("31 02 01 00 00 00 00 34");
    }

    @Test
    void getCoverageStatus_keepsRegisteredDeviceWhenNoPacketSeen() {
        when(deviceTypeRepository.findAllWithFullStructure()).thenReturn(List.of(lightDeviceType()));
        when(deviceEntityRepository.findAll()).thenReturn(List.of(device(DeviceKey.Light, 3)));
        when(seenPacketMemoryStore.snapshot()).thenReturn(List.of());

        List<DevicePacketCoverageDeviceType> result = packetCoverageService.getCoverageStatus();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstances()).extracting("index").containsExactly(3);
        assertThat(result.get(0).getInstances().get(0).getPacketKinds())
                .allSatisfy(kind -> {
                    assertThat(kind.isInboundSeen()).isFalse();
                    assertThat(kind.isOutboundSeen()).isFalse();
                });
    }

    @Test
    void getCoverageStatus_createsDeviceInstanceFromObservedPacketEvenIfNotRegistered() {
        LocalDateTime seenAt = LocalDateTime.of(2026, 6, 14, 10, 0);
        when(deviceTypeRepository.findAllWithFullStructure()).thenReturn(List.of(lightDeviceType()));
        when(deviceEntityRepository.findAll()).thenReturn(List.of());
        when(seenPacketMemoryStore.snapshot()).thenReturn(List.of(
                new SeenPacketEntry("B0 00 04 00 00 00 00 B4", "B0", SeenPacketDirection.INBOUND, seenAt, seenAt)));

        List<DevicePacketCoverageDeviceType> result = packetCoverageService.getCoverageStatus();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstances()).extracting("index").containsExactly(4);
    }

    private DeviceType lightDeviceType() {
        PacketType command = packetType("command", "31", 1);
        PacketType stateRequest = packetType("state_request", "30", 1);
        PacketType state = packetType("state", "B0", 2);
        return DeviceType.builder()
                .name("Light")
                .type("light")
                .packetTypes(new LinkedHashSet<>(Set.of(command, stateRequest, state)))
                .build();
    }

    private PacketType packetType(String kind, String header, int deviceIdPosition) {
        ParsingField deviceId = ParsingField.builder()
                .name("deviceId")
                .position(deviceIdPosition)
                .build();
        return PacketType.builder()
                .kind(kind)
                .header(header)
                .fields(new LinkedHashSet<>(Set.of(deviceId)))
                .build();
    }

    private DeviceEntity device(DeviceKey key, int index) {
        return DeviceEntity.builder()
                .uniqueId("commax_" + key.name() + "_" + index)
                .objectId(key.name().toLowerCase() + "_" + index)
                .type(key)
                .index(index)
                .build();
    }
}

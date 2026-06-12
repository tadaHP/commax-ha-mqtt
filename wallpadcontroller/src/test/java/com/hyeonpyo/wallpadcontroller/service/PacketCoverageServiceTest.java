package com.hyeonpyo.wallpadcontroller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingFieldValue;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
import com.hyeonpyo.wallpadcontroller.dto.CoverageStatus;
import com.hyeonpyo.wallpadcontroller.dto.PacketCoverageDevice;

@ExtendWith(MockitoExtension.class)
class PacketCoverageServiceTest {

    @Mock
    private DeviceTypeRepository deviceTypeRepository;

    @Mock
    private SeenPacketMemoryStore seenPacketMemoryStore;

    @InjectMocks
    private PacketCoverageService packetCoverageService;

    @Test
    void getCoverageStatus_returnsMissingWhenNoObservations() {
        DeviceType deviceType = deviceType("Light", "31", "state", 2, "03", "ON");
        when(deviceTypeRepository.findAllWithFullStructure()).thenReturn(List.of(deviceType));
        when(seenPacketMemoryStore.buildReceivedData()).thenReturn(Collections.emptyMap());

        List<PacketCoverageDevice> result = packetCoverageService.getCoverageStatus();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKinds().get(0).getStatus()).isEqualTo(CoverageStatus.MISSING);
    }

    @Test
    void getCoverageStatus_returnsCompleteWhenAllDefinedValuesSeen() {
        DeviceType deviceType = deviceType("Light", "31", "state", 2, "03", "ON");
        when(deviceTypeRepository.findAllWithFullStructure()).thenReturn(List.of(deviceType));

        Map<String, Set<String>> received = new HashMap<>();
        received.put("31-2", new HashSet<>(Set.of("03")));
        when(seenPacketMemoryStore.buildReceivedData()).thenReturn(received);

        List<PacketCoverageDevice> result = packetCoverageService.getCoverageStatus();

        assertThat(result.get(0).getKinds().get(0).getStatus()).isEqualTo(CoverageStatus.COMPLETE);
    }

    @Test
    void getCoverageStatus_returnsNewDetectedForUnknownHex() {
        DeviceType deviceType = deviceType("Light", "31", "state", 2, "03", "ON");
        when(deviceTypeRepository.findAllWithFullStructure()).thenReturn(List.of(deviceType));

        Map<String, Set<String>> received = new HashMap<>();
        received.put("31-2", new HashSet<>(Set.of("03", "FF")));
        when(seenPacketMemoryStore.buildReceivedData()).thenReturn(received);

        List<PacketCoverageDevice> result = packetCoverageService.getCoverageStatus();

        assertThat(result.get(0).getKinds().get(0).getStatus()).isEqualTo(CoverageStatus.NEW_DETECTED);
    }

    private DeviceType deviceType(String name, String header, String fieldName, int position, String hex, String rawKey) {
        ParsingFieldValue value = ParsingFieldValue.builder()
                .hex(hex)
                .rawKey(rawKey)
                .build();
        ParsingField field = ParsingField.builder()
                .name(fieldName)
                .position(position)
                .valueMappings(new LinkedHashSet<>(Set.of(value)))
                .build();
        PacketType packetType = PacketType.builder()
                .kind("power")
                .header(header)
                .fields(new LinkedHashSet<>(Set.of(field)))
                .build();
        return DeviceType.builder()
                .name(name)
                .packetTypes(List.of(packetType))
                .build();
    }
}

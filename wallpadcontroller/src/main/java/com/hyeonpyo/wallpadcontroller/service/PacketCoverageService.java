package com.hyeonpyo.wallpadcontroller.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacketDirection;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.dto.DevicePacketCoverageDeviceType;
import com.hyeonpyo.wallpadcontroller.dto.DevicePacketCoverageInstance;
import com.hyeonpyo.wallpadcontroller.dto.DevicePacketCoverageKind;
import com.hyeonpyo.wallpadcontroller.dto.DevicePacketObservation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacketCoverageService {

    private static final List<String> KIND_ORDER = List.of("command", "state_request", "state", "ack");

    private final DeviceTypeRepository deviceTypeRepository;
    private final DeviceEntityRepository deviceEntityRepository;
    private final SeenPacketMemoryStore seenPacketMemoryStore;

    public List<DevicePacketCoverageDeviceType> getCoverageStatus() {
        List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();
        Map<String, DeviceTypeDefinition> definitionsByName = buildDefinitionsByName(deviceTypes);
        Map<String, PacketDefinition> definitionsByHeader = buildDefinitionsByHeader(definitionsByName);
        Map<DeviceKindKey, List<ObservedPacket>> observedPackets = groupObservedPackets(definitionsByHeader);
        Map<String, Set<Integer>> indexesByDeviceName = buildRegisteredIndexesByDeviceName();
        mergeObservedIndexes(indexesByDeviceName, observedPackets);

        List<DevicePacketCoverageDeviceType> result = new ArrayList<>();
        for (DeviceType deviceType : deviceTypes) {
            DeviceTypeDefinition definition = definitionsByName.get(deviceType.getName());
            if (definition == null || definition.packetDefinitions().isEmpty()) {
                continue;
            }

            Set<Integer> indexes = indexesByDeviceName.getOrDefault(deviceType.getName(), Set.of());
            if (indexes.isEmpty()) {
                continue;
            }

            List<DevicePacketCoverageInstance> instances = indexes.stream()
                    .sorted()
                    .map(index -> buildInstance(definition, index, observedPackets))
                    .toList();
            result.add(new DevicePacketCoverageDeviceType(deviceType.getName(), deviceType.getType(), instances));
        }
        return result;
    }

    private Map<String, DeviceTypeDefinition> buildDefinitionsByName(List<DeviceType> deviceTypes) {
        Map<String, DeviceTypeDefinition> definitions = new LinkedHashMap<>();
        for (DeviceType deviceType : deviceTypes) {
            List<PacketDefinition> packetDefinitions = deviceType.getPacketTypes().stream()
                    .filter(packetType -> packetType.getHeader() != null && !packetType.getHeader().isBlank())
                    .sorted(Comparator.comparingInt(packetType -> kindOrder(packetType.getKind())))
                    .map(packetType -> new PacketDefinition(
                            deviceType.getName(),
                            packetType.getKind(),
                            packetType.getHeader().toUpperCase(Locale.ROOT),
                            findDeviceIdPosition(packetType).orElse(1)))
                    .toList();
            definitions.put(deviceType.getName(), new DeviceTypeDefinition(deviceType.getName(), deviceType.getType(), packetDefinitions));
        }
        return definitions;
    }

    private Map<String, PacketDefinition> buildDefinitionsByHeader(Map<String, DeviceTypeDefinition> definitionsByName) {
        Map<String, PacketDefinition> definitionsByHeader = new HashMap<>();
        for (DeviceTypeDefinition deviceDefinition : definitionsByName.values()) {
            for (PacketDefinition packetDefinition : deviceDefinition.packetDefinitions()) {
                definitionsByHeader.put(packetDefinition.header(), packetDefinition);
            }
        }
        return definitionsByHeader;
    }

    private Map<DeviceKindKey, List<ObservedPacket>> groupObservedPackets(Map<String, PacketDefinition> definitionsByHeader) {
        Map<DeviceKindKey, List<ObservedPacket>> observedPackets = new HashMap<>();
        for (SeenPacketEntry entry : seenPacketMemoryStore.snapshot()) {
            PacketDefinition packetDefinition = definitionsByHeader.get(entry.getHeader());
            if (packetDefinition == null) {
                continue;
            }
            int deviceIndex = extractDeviceIndex(entry.getRawData(), packetDefinition.deviceIdPosition());
            DeviceKindKey key = new DeviceKindKey(packetDefinition.deviceName(), deviceIndex, packetDefinition.kind());
            observedPackets.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new ObservedPacket(entry.getRawData(), entry.getDirection(), entry.getFirstSeenAt(), entry.getLastSeenAt()));
        }
        return observedPackets;
    }

    private Map<String, Set<Integer>> buildRegisteredIndexesByDeviceName() {
        Map<String, Set<Integer>> indexesByDeviceName = new HashMap<>();
        for (DeviceEntity device : deviceEntityRepository.findAll()) {
            indexesByDeviceName.computeIfAbsent(device.getType().name(), ignored -> new TreeSet<>())
                    .add(device.getIndex());
        }
        return indexesByDeviceName;
    }

    private void mergeObservedIndexes(Map<String, Set<Integer>> indexesByDeviceName, Map<DeviceKindKey, List<ObservedPacket>> observedPackets) {
        for (DeviceKindKey key : observedPackets.keySet()) {
            indexesByDeviceName.computeIfAbsent(key.deviceName(), ignored -> new TreeSet<>())
                    .add(key.deviceIndex());
        }
    }

    private DevicePacketCoverageInstance buildInstance(
            DeviceTypeDefinition definition,
            int index,
            Map<DeviceKindKey, List<ObservedPacket>> observedPackets) {
        List<DevicePacketCoverageKind> packetKinds = definition.packetDefinitions().stream()
                .map(packetDefinition -> buildKind(packetDefinition, index, observedPackets))
                .toList();
        return new DevicePacketCoverageInstance(index, definition.deviceName() + " " + index, packetKinds);
    }

    private DevicePacketCoverageKind buildKind(
            PacketDefinition packetDefinition,
            int index,
            Map<DeviceKindKey, List<ObservedPacket>> observedPackets) {
        List<ObservedPacket> observations = observedPackets.getOrDefault(
                new DeviceKindKey(packetDefinition.deviceName(), index, packetDefinition.kind()),
                List.of());

        List<DevicePacketObservation> inboundPackets = toObservations(observations, SeenPacketDirection.INBOUND);
        List<DevicePacketObservation> outboundPackets = toObservations(observations, SeenPacketDirection.OUTBOUND);

        LocalDateTime firstSeenAt = observations.stream()
                .map(ObservedPacket::firstSeenAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime lastSeenAt = observations.stream()
                .map(ObservedPacket::lastSeenAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new DevicePacketCoverageKind(
                packetDefinition.kind(),
                packetDefinition.header(),
                !inboundPackets.isEmpty(),
                !outboundPackets.isEmpty(),
                firstSeenAt,
                lastSeenAt,
                inboundPackets,
                outboundPackets);
    }

    private List<DevicePacketObservation> toObservations(List<ObservedPacket> observations, SeenPacketDirection direction) {
        return observations.stream()
                .filter(packet -> packet.direction() == direction)
                .sorted(Comparator.comparing(ObservedPacket::lastSeenAt).reversed())
                .map(packet -> new DevicePacketObservation(packet.rawData(), packet.firstSeenAt(), packet.lastSeenAt()))
                .toList();
    }

    private Optional<Integer> findDeviceIdPosition(PacketType packetType) {
        return packetType.getFields().stream()
                .filter(field -> field.getName() != null && "deviceId".equalsIgnoreCase(field.getName()))
                .map(ParsingField::getPosition)
                .filter(position -> position != null && position > 0)
                .findFirst();
    }

    private int extractDeviceIndex(String rawData, int deviceIdPosition) {
        String[] parts = rawData.split(" ");
        if (parts.length > deviceIdPosition) {
            try {
                return Integer.parseInt(parts[deviceIdPosition], 16);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private int kindOrder(String kind) {
        int index = KIND_ORDER.indexOf(kind);
        return index >= 0 ? index : KIND_ORDER.size();
    }

    private record DeviceTypeDefinition(String deviceName, String type, List<PacketDefinition> packetDefinitions) {
    }

    private record PacketDefinition(String deviceName, String kind, String header, int deviceIdPosition) {
    }

    private record DeviceKindKey(String deviceName, int deviceIndex, String kind) {
    }

    private record ObservedPacket(String rawData, SeenPacketDirection direction, LocalDateTime firstSeenAt, LocalDateTime lastSeenAt) {
    }
}

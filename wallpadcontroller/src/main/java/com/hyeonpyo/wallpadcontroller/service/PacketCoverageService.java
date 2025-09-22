package com.hyeonpyo.wallpadcontroller.service;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingFieldValue;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.LogStatus;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLog;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLogRepository;
import com.hyeonpyo.wallpadcontroller.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PacketCoverageService {

    private final PacketLogRepository packetLogRepository;
    private final DeviceTypeRepository deviceTypeRepository;

    public List<PacketCoverageDevice> getCoverageStatus() throws Exception {
        List<DeviceType> allDevices = deviceTypeRepository.findAllWithFullStructure();
        List<PacketLog> successLogs = packetLogRepository.findByStatus(LogStatus.SUCCESS);

        Map<String, Set<String>> receivedData = new HashMap<>();
        for (PacketLog log : successLogs) {
            String[] hexValues = log.getRawData().split(" ");
            if (hexValues.length < 2) continue;
            String header = hexValues[0];
            for (int i = 1; i < hexValues.length; i++) {
                String key = header + "-" + (i + 1);
                receivedData.computeIfAbsent(key, k -> new HashSet<>()).add(hexValues[i]);
            }
        }

        List<PacketCoverageDevice> devices = new ArrayList<>();
        for (DeviceType deviceType : allDevices) {
            Map<String, PacketCoverageKind> kindsMap = new LinkedHashMap<>();
            for (PacketType packetType : deviceType.getPacketTypes()) {
                String header = packetType.getHeader();
                if (header != null) {
                    List<PacketCoverageField> fields = buildFields(packetType, receivedData);
                    boolean receivedOverall = receivedData.keySet().stream().anyMatch(k -> k.startsWith(header + "-"));
                    CoverageStatus summaryStatus = calculateSummaryStatus(fields, receivedOverall);
                    kindsMap.put(packetType.getKind(), new PacketCoverageKind(packetType.getKind(), header, summaryStatus, fields));
                }
            }
            if (!kindsMap.isEmpty()) {
                devices.add(new PacketCoverageDevice(deviceType.getName(), new ArrayList<>(kindsMap.values())));
            }
        }

        return devices;
    }

    private List<PacketCoverageField> buildFields(PacketType packetType, Map<String, Set<String>> receivedData) {
        List<PacketCoverageField> fields = new ArrayList<>();
        for (ParsingField field : packetType.getFields()) {
            if (field.getName() == null || field.getName().equals("empty") || field.getName().equals("checksum") || field.getName().equals("deviceId")) {
                continue;
            }

            String key = packetType.getHeader() + "-" + field.getPosition();
            Set<String> receivedValues = receivedData.getOrDefault(key, Collections.emptySet());

            List<PacketCoverageValue> definedValues = new ArrayList<>();
            Set<String> definedHexValues = new HashSet<>();
            for (ParsingFieldValue value : field.getValueMappings()) {
                boolean received = receivedValues.contains(value.getHex().toUpperCase());
                definedValues.add(new PacketCoverageValue(value.getRawKey(), value.getHex(), received));
                definedHexValues.add(value.getHex().toUpperCase());
            }

            List<PacketCoverageValue> newValues = receivedValues.stream()
                    .filter(hex -> !definedHexValues.contains(hex))
                    .map(hex -> new PacketCoverageValue("NEW_VALUE", hex, true))
                    .collect(Collectors.toList());

            if (!definedValues.isEmpty() || !newValues.isEmpty()) {
                fields.add(new PacketCoverageField(field.getName(), definedValues, newValues));
            }
        }
        return fields;
    }

    private CoverageStatus calculateSummaryStatus(List<PacketCoverageField> fields, boolean receivedOverall) {
        if (!receivedOverall) {
            return CoverageStatus.MISSING;
        }

        boolean hasNew = fields.stream().anyMatch(f -> !f.getNewValues().isEmpty());
        if (hasNew) {
            return CoverageStatus.NEW_DETECTED;
        }

        boolean hasMissing = fields.stream()
                .flatMap(f -> f.getDefinedValues().stream())
                .anyMatch(v -> !v.isReceived());
        if (hasMissing) {
            return CoverageStatus.PARTIAL;
        }

        return CoverageStatus.COMPLETE;
    }
}

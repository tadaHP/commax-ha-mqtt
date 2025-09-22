package com.hyeonpyo.wallpadcontroller.service;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingFieldValue;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLogRepository;
import com.hyeonpyo.wallpadcontroller.dto.PacketCoverageDevice;
import com.hyeonpyo.wallpadcontroller.dto.PacketCoverageKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PacketCoverageService {

    private final PacketLogRepository packetLogRepository;
    private final DeviceTypeRepository deviceTypeRepository;

    public List<PacketCoverageDevice> getCoverageStatus() throws Exception {
        List<DeviceType> allDevices = deviceTypeRepository.findAllWithFullStructure();
        Set<String> receivedHeaders = packetLogRepository.findDistinctSuccessHeaders();

        List<PacketCoverageDevice> devices = new ArrayList<>();
        for (DeviceType deviceType : allDevices) {
            Map<String, PacketCoverageKind> kindsMap = new LinkedHashMap<>();
            for (PacketType packetType : deviceType.getPacketTypes()) {
                String header = packetType.getHeader();
                if (header != null) {
                    boolean received = receivedHeaders.contains(header.toUpperCase());
                    String description = buildDescription(packetType);
                    kindsMap.put(packetType.getKind(), new PacketCoverageKind(packetType.getKind(), header, received, description));
                }
            }
            if (!kindsMap.isEmpty()) {
                devices.add(new PacketCoverageDevice(deviceType.getName(), new ArrayList<>(kindsMap.values())));
            }
        }

        return devices;
    }

    private String buildDescription(PacketType packetType) {
        StringJoiner description = new StringJoiner(", ");
        for (ParsingField field : packetType.getFields()) {
            if (field.getName() == null || field.getName().equals("empty") || field.getName().equals("checksum") || field.getName().equals("deviceId")) {
                continue;
            }

            Set<ParsingFieldValue> values = field.getValueMappings();
            if (values.isEmpty() || (values.size() == 1 && values.iterator().next().getRawKey().equals("id"))) {
                continue;
            }

            String valueString = values.stream()
                                       .map(ParsingFieldValue::getRawKey)
                                       .filter(rawKey -> !rawKey.equals("id"))
                                       .collect(Collectors.joining("/"));
            
            if (!valueString.isEmpty()) {
                description.add(field.getName() + ": [" + valueString + "]");
            }
        }
        return description.toString();
    }
}

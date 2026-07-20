package com.hyeonpyo.wallpadcontroller.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacketTypeCatalogService {

    private final DeviceTypeRepository deviceTypeRepository;

    public Map<String, List<PacketFilterTypeDto>> getGroupedPacketTypes() {
        Map<String, List<PacketFilterTypeDto>> groupedPacketTypes = new TreeMap<>();
        List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();

        for (DeviceType deviceType : deviceTypes) {
            Map<String, PacketType> uniquePacketTypesByHeader = new LinkedHashMap<>();
            for (PacketType packetType : deviceType.getPacketTypes()) {
                uniquePacketTypesByHeader.put(packetType.getHeader(), packetType);
            }

            List<PacketFilterTypeDto> dtos = new ArrayList<>();
            for (PacketType packetType : uniquePacketTypesByHeader.values()) {
                dtos.add(new PacketFilterTypeDto(packetType));
            }

            if (!dtos.isEmpty()) {
                groupedPacketTypes.put(deviceType.getName(), dtos);
            }
        }
        return groupedPacketTypes;
    }

    @Getter
    public static class PacketFilterTypeDto {
        private final String header;
        private final String displayName;

        public PacketFilterTypeDto(PacketType packetType) {
            this.header = packetType.getHeader();
            this.displayName = String.format("%s-%s", packetType.getDeviceType().getName(), packetType.getKind());
        }
    }
}

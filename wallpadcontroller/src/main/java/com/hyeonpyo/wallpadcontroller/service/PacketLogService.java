package com.hyeonpyo.wallpadcontroller.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLog;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLogRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacketLogService {

    private final PacketLogRepository packetLogRepository;
    private final DeviceTypeRepository deviceTypeRepository;

    public Page<PacketLog> findAll(Pageable pageable) {
        return packetLogRepository.findAll(pageable);
    }

    public Map<String, List<PacketFilterTypeDto>> getGroupedPacketTypes() {
        Map<String, List<PacketFilterTypeDto>> groupedPacketTypes = new TreeMap<>();
        List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();

        for (DeviceType deviceType : deviceTypes) {
            // header를 기준으로 중복을 제거하기 위해 Map을 사용 (PacketParser와 동일한 방식)
            Map<String, PacketType> uniquePacketTypesByHeader = new LinkedHashMap<>();
            for (PacketType packetType : deviceType.getPacketTypes()) {
                uniquePacketTypesByHeader.put(packetType.getHeader(), packetType);
            }

            // 중복이 제거된 PacketType으로 DTO 리스트 생성
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

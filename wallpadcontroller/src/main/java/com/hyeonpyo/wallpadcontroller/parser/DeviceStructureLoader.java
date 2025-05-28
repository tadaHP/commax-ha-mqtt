package com.hyeonpyo.wallpadcontroller.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DeviceStructureLoader {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private final Map<String, Object> deviceStructure = new HashMap<>();

    public void loadDeviceStructure() {
        try {
            InputStream input = getClass().getClassLoader().getResourceAsStream("commax-packet-struct.yml");
            // File customFile = new File("share/packet_structures_custom.yaml");

            // File targetFile;
            // if ("custom".equalsIgnoreCase(vendor) && customFile.exists()) {
            //     targetFile = customFile;
            // } else {
            //     targetFile = defaultFile;
            // }

            if (input == null) {
                throw new FileNotFoundException("리소스 파일이 없습니다: commax-packet-struct.yml");
            }

            Map<String, Object> structure = yamlMapper.readValue(input, new TypeReference<>() {});
            deviceStructure.clear();
            deviceStructure.putAll(structure);
            log.info("✅ '{}' 벤더의 패킷 구조 로드 완료", "commax");

            addFieldPositions();

        } catch (IOException e) {
            log.error("패킷 구조 파일 로드 실패", e);
        }
    }

    private void addFieldPositions() {
        for (Map.Entry<String, Object> entry : deviceStructure.entrySet()) {
            Map<String, Object> device = cast(entry.getValue());
            for (String packetType : List.of("command", "state")) {
                if (device.containsKey(packetType)) {
                    Map<String, Object> packet = cast(device.get(packetType));
                    Map<String, Object> structure = cast(packet.get("structure"));
                    Map<String, String> fieldPositions = new HashMap<>();

                    for (Map.Entry<String, Object> posEntry : structure.entrySet()) {
                        String pos = posEntry.getKey();
                        Map<String, Object> field = cast(posEntry.getValue());
                        String name = (String) field.get("name");
                        if (!"empty".equals(name)) {
                            if (fieldPositions.containsKey(name)) {
                                log.error("❗ 중복된 필드: {}.{} - {}", entry.getKey(), packetType, name);
                            } else {
                                fieldPositions.put(name, pos);
                            }
                        }
                    }

                    packet.put("fieldPositions", fieldPositions);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object obj) {
        return (Map<String, Object>) obj;
    }

    public Map<String, Object> getDeviceStructure() {
        return deviceStructure;
    }
}
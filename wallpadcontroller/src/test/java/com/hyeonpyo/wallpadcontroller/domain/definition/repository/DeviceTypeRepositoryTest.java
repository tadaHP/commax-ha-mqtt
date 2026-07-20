package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingFieldValue;

@DataJpaTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeviceTypeRepositoryTest {

    @Autowired
    private DeviceTypeRepository deviceTypeRepository;

    @Test
    void findAllWithFullStructure_doesNotDuplicatePacketTypes() {
        DeviceType light = DeviceType.builder()
                .name("Light")
                .type("light")
                .build();

        PacketType command = createPacketType(light, "command", "31", 7, true);
        PacketType stateRequest = createPacketType(light, "state_request", "30", 7, false);
        PacketType state = createPacketType(light, "state", "B0", 7, true);

        light.getPacketTypes().addAll(Set.of(command, stateRequest, state));
        deviceTypeRepository.saveAndFlush(light);

        List<DeviceType> loaded = deviceTypeRepository.findAllWithFullStructure();

        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getPacketTypes()).hasSize(3);
        assertThat(loaded.get(0).getPacketTypes())
                .extracting(PacketType::getKind)
                .containsExactlyInAnyOrder("command", "state_request", "state");

        PacketType loadedCommand = loaded.get(0).getPacketTypes().stream()
                .filter(packetType -> "command".equals(packetType.getKind()))
                .findFirst()
                .orElseThrow();
        assertThat(loadedCommand.getFields()).hasSize(7);
        assertThat(loadedCommand.getFields().stream()
                .filter(field -> "power".equals(field.getName()))
                .findFirst()
                .orElseThrow()
                .getValueMappings()).hasSize(2);
    }

    private static PacketType createPacketType(
            DeviceType deviceType,
            String kind,
            String header,
            int fieldCount,
            boolean powerFieldWithValues) {
        PacketType packetType = PacketType.builder()
                .deviceType(deviceType)
                .kind(kind)
                .header(header)
                .build();

        for (int position = 1; position <= fieldCount; position++) {
            String name;
            if (position == 1) {
                name = "deviceId";
            } else if (position == 2 && powerFieldWithValues) {
                name = "power";
            } else if (position == fieldCount) {
                name = "checksum";
            } else {
                name = "empty";
            }
            ParsingField field = ParsingField.builder()
                    .packetType(packetType)
                    .position(position)
                    .name(name)
                    .build();
            packetType.getFields().add(field);

            if ("power".equals(name)) {
                field.getValueMappings().add(valueMapping(field, "ON", "01"));
                field.getValueMappings().add(valueMapping(field, "OFF", "00"));
            }
        }

        return packetType;
    }

    private static ParsingFieldValue valueMapping(ParsingField field, String rawKey, String hex) {
        return ParsingFieldValue.builder()
                .parsingField(field)
                .rawKey(rawKey)
                .hex(hex)
                .build();
    }
}

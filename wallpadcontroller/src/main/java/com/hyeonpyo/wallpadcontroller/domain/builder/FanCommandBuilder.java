package com.hyeonpyo.wallpadcontroller.domain.builder;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FanCommandBuilder implements CommandBuilder {

    private final CommandPacketBuilder baseBuilder;

    @Override
    public Optional<byte[]> build(String type, int index, String field, String payload) {
        switch (field.toLowerCase()) {
            case "power":
            case "mode":
                return baseBuilder.build(type, index, Map.of(
                    "commandType", "power",
                    "value", payload
                ));
            case "speed":
                return baseBuilder.build(type, index, Map.of(
                    "commandType", "setSpeed",
                    "value", payload.toUpperCase()
                ));
            default:
                return Optional.empty();
        }
    }
}
package com.hyeonpyo.wallpadcontroller.domain.builder;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SimpleCommandBuilder implements CommandBuilder {
    
    private final CommandPacketBuilder baseBuilder;

    public Optional<byte[]> build(String type, int index, String field, String payload) {
        return baseBuilder.build(type, index, Map.of(field, payload));
    }
}

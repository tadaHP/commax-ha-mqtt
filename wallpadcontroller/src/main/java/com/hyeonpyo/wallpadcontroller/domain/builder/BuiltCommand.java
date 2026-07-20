package com.hyeonpyo.wallpadcontroller.domain.builder;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.CommandMappingRule;

/** Packet plus the rule that defines its ACK and retry behaviour. */
public record BuiltCommand(byte[] packet, CommandMappingRule rule) {
    public BuiltCommand {
        packet = packet.clone();
    }
}

package com.hyeonpyo.wallpadcontroller.command;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.CommandMappingRule;

/** DB-backed ACK matcher equivalent to H2M data + mask schemas. */
@Component
public class CommandAckProfiles {
    public Optional<AckMatcher> forRule(CommandMappingRule rule, int deviceIndex) {
        if (rule.getAckPattern() == null || rule.getAckPattern().isBlank()) return Optional.empty();
        byte[] pattern = parse(rule.getAckPattern(), deviceIndex);
        byte[] mask = rule.getAckMask() == null || rule.getAckMask().isBlank()
                ? exactMask(pattern.length) : parse(rule.getAckMask(), deviceIndex);
        return Optional.of(new MaskedAckMatcher(pattern, mask));
    }

    private byte[] parse(String value, int deviceIndex) {
        String[] tokens = value.trim().split("\\s+");
        byte[] bytes = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            bytes[i] = (byte) ("{index}".equals(tokens[i]) ? deviceIndex : Integer.parseInt(tokens[i], 16));
        }
        return bytes;
    }

    private byte[] exactMask(int length) {
        byte[] mask = new byte[length];
        Arrays.fill(mask, (byte) 0xFF);
        return mask;
    }
}

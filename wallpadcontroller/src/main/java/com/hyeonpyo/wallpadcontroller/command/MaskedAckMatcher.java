package com.hyeonpyo.wallpadcontroller.command;

import java.util.Arrays;

/** Byte-pattern matcher equivalent to H2M's data + mask ACK schema. */
public final class MaskedAckMatcher implements AckMatcher {
    private final byte[] pattern;
    private final byte[] mask;

    public MaskedAckMatcher(byte[] pattern, byte[] mask) {
        if (pattern.length != mask.length) {
            throw new IllegalArgumentException("ACK pattern and mask lengths must match");
        }
        this.pattern = Arrays.copyOf(pattern, pattern.length);
        this.mask = Arrays.copyOf(mask, mask.length);
    }

    @Override
    public boolean matches(byte[] packet) {
        if (packet.length < pattern.length) return false;
        for (int i = 0; i < pattern.length; i++) {
            if ((packet[i] & mask[i]) != (pattern[i] & mask[i])) return false;
        }
        return true;
    }
}

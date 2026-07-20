package com.hyeonpyo.wallpadcontroller.command;

/** Matches an EW11/RS485 packet that confirms a command was processed. */
@FunctionalInterface
public interface AckMatcher {
    boolean matches(byte[] packet);
}

package com.hyeonpyo.wallpadcontroller.command;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Temporary Commax ACK profiles. They deliberately use a mask for fields that
 * vary by model; the next database migration will move these profiles into the
 * packet-rule tables.
 */
@Component
public class CommandAckProfiles {
    private static final byte ANY = 0x00;
    private static final byte EXACT = (byte) 0xFF;

    public Optional<AckMatcher> forCommand(String deviceType, int deviceIndex, String field, String payload) {
        String value = payload.toUpperCase(Locale.ROOT);
        return switch (deviceType) {
            case "Light" -> Optional.of(new MaskedAckMatcher(
                    new byte[] {(byte) 0xB1, onOff(value), (byte) deviceIndex},
                    new byte[] {EXACT, EXACT, EXACT}));
            case "LightBreaker" -> Optional.of(new MaskedAckMatcher(
                    new byte[] {(byte) 0xA2, onOff(value), (byte) deviceIndex},
                    new byte[] {EXACT, EXACT, EXACT}));
            case "Thermo" -> Optional.of(new MaskedAckMatcher(
                    new byte[] {(byte) 0x84, thermoPower(value), (byte) deviceIndex},
                    new byte[] {EXACT, "mode".equals(field) ? EXACT : ANY, EXACT}));
            case "Fan" -> Optional.of(new MaskedAckMatcher(
                    new byte[] {(byte) 0xF8, 0x00, (byte) deviceIndex},
                    new byte[] {EXACT, ANY, EXACT}));
            case "Gas" -> Optional.of(new MaskedAckMatcher(
                    new byte[] {(byte) 0x91}, new byte[] {EXACT}));
            case "EV" -> Optional.of(new MaskedAckMatcher(
                    new byte[] {0x23}, new byte[] {EXACT}));
            // Outlet remains state-confirmed until its ACK packet is measured.
            default -> Optional.empty();
        };
    }

    private byte onOff(String value) {
        return "ON".equals(value) ? (byte) 0x01 : 0x00;
    }

    private byte thermoPower(String value) {
        return "HEAT".equals(value) ? (byte) 0x81 : (byte) 0x80;
    }
}

package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class OutletStateTest {
    @Test
    void marksUnsupportedStateTypeAsUnknown() {
        OutletState state = OutletState.fromPacketFields(Map.of(
                "power", "on_with_eco", "stateType", "future_type",
                "data1", "00", "data2", "00", "data3", "00"));

        assertThat(state.toMap()).containsEntry("power", "ON")
                .containsEntry("ecomode", "ON")
                .containsEntry("watt", "unknown")
                .containsEntry("cutoff", "unknown");
    }

    @Test
    void marksInvalidBcdAsUnknown() {
        OutletState state = OutletState.fromPacketFields(Map.of(
                "power", "on", "stateType", "wattage",
                "data1", "1A", "data2", "00", "data3", "00"));

        assertThat(state.toMap()).containsEntry("watt", "unknown");
    }
}

package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ThermoStateTest {
    @Test
    void handlesObservedAwayValueWithBothExistingAndFreshDatabaseMappings() {
        for (String raw : new String[] {"84", "away"}) {
            var state = new ThermoState(raw, "28", "23");
            assertThat(state.toMap()).containsEntry("mode", "heat")
                    .containsEntry("away", "ON").containsEntry("action", "None")
                    .containsEntry("curTemp", "28").containsEntry("setTemp", "23");
            assertThat(state.toJson()).contains("\"away\": \"ON\"");
        }
    }

    @Test
    void normalAndOffStatesExplicitlyClearAway() {
        assertThat(new ThermoState("idle", "28", "23").toMap())
                .containsEntry("away", "OFF").containsEntry("mode", "heat").containsEntry("action", "idle");
        assertThat(new ThermoState("heating", "28", "29").toMap())
                .containsEntry("away", "OFF").containsEntry("action", "heating");
        assertThat(new ThermoState("off", "28", "23").toMap())
                .containsEntry("away", "OFF").containsEntry("mode", "off");
    }

    @Test
    void derivesNormalHeatingActionFromTemperatureIncludingExistingIdleMapping() {
        for (String raw : new String[] {"idle", "heating", "81", "83"}) {
            assertThat(new ThermoState(raw, "27", "29").toMap())
                    .containsEntry("mode", "heat").containsEntry("action", "heating");
            assertThat(new ThermoState(raw, "27", "27").toMap()).containsEntry("action", "idle");
            assertThat(new ThermoState(raw, "27", "25").toMap()).containsEntry("action", "idle");
        }
        assertThat(new ThermoState("idle", "9", "10").toMap()).containsEntry("action", "heating");
    }

    @Test
    void doesNotInferHeatingForOffAwayOrInvalidTemperatures() {
        assertThat(new ThermoState("off", "20", "29").toMap()).containsEntry("action", "off");
        assertThat(new ThermoState("84", "20", "29").toMap()).containsEntry("action", "None");
        assertThat(new ThermoState("idle", null, "29").toMap()).containsEntry("action", "None");
        assertThat(new ThermoState("idle", "unknown", "29").toMap()).containsEntry("action", "None");
    }
}

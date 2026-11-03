package com.hyeonpyo.wallpadcontroller.mqtt.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceKey;
import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;
import com.hyeonpyo.wallpadcontroller.settings.RuntimeConnectionManager;

class ThermoAwayDiscoveryTest {
    @Test
    void publishesReadOnlyAwaySensorAndRemovesItWhenDeviceIsDisabled() throws Exception {
        var connections = mock(RuntimeConnectionManager.class);
        var properties = new MqttProperties(); properties.setHaTopic("commax");
        var publisher = new MqttDiscoveryPublisher(mock(DeviceEntityRepository.class), connections, properties);
        var device = DeviceEntity.builder().type(DeviceKey.Thermo).index(3)
                .uniqueId("commax_Thermo_3").objectId("commax_Thermo_3").used(true).build();
        publisher.syncDeviceDiscovery(device);
        var json = ArgumentCaptor.forClass(byte[].class);
        verify(connections).publish(eq("homeassistant/binary_sensor/Thermo3_away/config"), json.capture(), eq(1), eq(true));
        var payload = new ObjectMapper().readTree(json.getValue());
        assertThat(payload.path("state_topic").asText()).isEqualTo("commax/state/Thermo3/away");
        assertThat(payload.has("command_topic")).isFalse();
        assertThat(payload.has("mode_command_topic")).isFalse();
        assertThat(payload.path("payload_on").asText()).isEqualTo("ON");
        device.setUsed(false);
        publisher.syncDeviceDiscovery(device);
        verify(connections).publish(eq("homeassistant/binary_sensor/Thermo3_away/config"), eq(new byte[0]), eq(1), eq(true));
    }
}

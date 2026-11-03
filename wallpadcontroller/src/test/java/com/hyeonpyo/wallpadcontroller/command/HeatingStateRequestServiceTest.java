package com.hyeonpyo.wallpadcontroller.command;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceKey;
import com.hyeonpyo.wallpadcontroller.ew11.Ew11Transport;
import com.hyeonpyo.wallpadcontroller.properties.HeatingPollingProperties;

class HeatingStateRequestServiceTest {
    private final DeviceEntityRepository devices = mock(DeviceEntityRepository.class);
    private final Ew11Transport transport = mock(Ew11Transport.class);
    private final HeatingPollingProperties properties = new HeatingPollingProperties();
    private final HeatingStateRequestService service = new HeatingStateRequestService(devices, transport, properties);

    @Test
    void pollsOnlyEnabledThermostatsWithChecksumInIndexOrder() throws Exception {
        properties.setDeviceGap(Duration.ofMillis(1));
        when(devices.findAllByUsedIsNullOrUsedTrue(any(Sort.class))).thenReturn(List.of(
                device(DeviceKey.Thermo, 3, null), device(DeviceKey.Light, 1, true),
                device(DeviceKey.Thermo, 1, true), device(DeviceKey.Thermo, 2, false),
                device(DeviceKey.Thermo, 256, true)));

        service.poll();

        var order = inOrder(transport);
        order.verify(transport).send(new byte[] {2, 1, 0, 0, 0, 0, 0, 3});
        order.verify(transport).send(new byte[] {2, 3, 0, 0, 0, 0, 0, 5});
        verifyNoMoreInteractions(transport);
    }

    @Test
    void disabledPollingDoesNotReadDevicesOrSendPackets() throws Exception {
        properties.setEnabled(false);
        service.poll();
        verifyNoInteractions(devices, transport);
    }

    @Test
    void transportFailureDoesNotSkipRemainingHeatersOrFutureCycles() throws Exception {
        properties.setDeviceGap(Duration.ofMillis(1));
        when(devices.findAllByUsedIsNullOrUsedTrue(any(Sort.class))).thenReturn(List.of(
                device(DeviceKey.Thermo, 1, true), device(DeviceKey.Thermo, 2, true)));
        doThrow(new IllegalStateException("Disconnected")).doNothing().when(transport).send(any(byte[].class));
        service.poll();
        service.poll();
        verify(transport, times(4)).send(any(byte[].class));
    }

    @Test
    void rejectsNonPositivePollingIntervals() {
        properties.setInterval(Duration.ZERO);
        assertThrows(IllegalArgumentException.class, service::start);
    }

    private DeviceEntity device(DeviceKey type, int index, Boolean used) {
        return DeviceEntity.builder().type(type).index(index).used(used).build();
    }
}

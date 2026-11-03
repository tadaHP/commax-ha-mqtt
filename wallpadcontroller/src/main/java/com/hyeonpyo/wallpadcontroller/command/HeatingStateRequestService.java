package com.hyeonpyo.wallpadcontroller.command;

import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceKey;
import com.hyeonpyo.wallpadcontroller.ew11.Ew11Transport;
import com.hyeonpyo.wallpadcontroller.properties.HeatingPollingProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Requests actual thermostat state; replies flow through the existing receive path. */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeatingStateRequestService {
    private final DeviceEntityRepository devices;
    private final Ew11Transport transport;
    private final HeatingPollingProperties properties;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    void start() {
        if (!properties.isEnabled()) return;
        if (properties.getInterval().toMillis() <= 0 || properties.getDeviceGap().toMillis() <= 0) {
            throw new IllegalArgumentException("Heating polling interval and device gap must be positive");
        }
        long interval = properties.getInterval().toMillis();
        executor.scheduleWithFixedDelay(this::pollSafely, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void pollSafely() {
        try {
            poll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // A temporary DB/transport failure must not cancel future polling cycles.
            log.warn("난방 상태조회 실패", e);
        }
    }

    void poll() throws InterruptedException {
        if (!properties.isEnabled()) return;
        var heaters = devices.findAllByUsedIsNullOrUsedTrue(Sort.by("index")).stream()
                .filter(device -> device.getType() == DeviceKey.Thermo && device.isMqttPublished())
                .filter(device -> device.getIndex() > 0 && device.getIndex() <= 255)
                .sorted(Comparator.comparingInt(DeviceEntity::getIndex))
                .toList();
        for (int i = 0; i < heaters.size(); i++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            int index = heaters.get(i).getIndex();
            byte[] packet = {0x02, (byte) index, 0, 0, 0, 0, 0, (byte) ((0x02 + index) & 0xFF)};
            try {
                transport.send(packet);
            } catch (Exception e) {
                log.warn("난방 {} 상태조회 전송 실패", index, e);
            }
            if (i + 1 < heaters.size()) Thread.sleep(properties.getDeviceGap().toMillis());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}

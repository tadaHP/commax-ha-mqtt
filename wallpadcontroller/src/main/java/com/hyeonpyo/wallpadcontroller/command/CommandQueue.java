package com.hyeonpyo.wallpadcontroller.command;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.ew11.Ew11Transport;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serializes command confirmation independently from MQTT. ACK matching is the
 * default; expected state is retained as the legacy fallback for incomplete
 * packet definitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandQueue {
    private final Ew11Transport ew11Transport;
    private final CommandAckProfiles ackProfiles;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, PendingCommand> pending = new ConcurrentHashMap<>();

    public void submit(String deviceType, int deviceIndex, String field, String payload, byte[] packet) {
        String key = key(deviceType, deviceIndex, field);
        PendingCommand command = new PendingCommand(
                UUID.randomUUID().toString(), key, payload, packet,
                ackProfiles.forCommand(deviceType, deviceIndex, field, payload).orElse(null),
                CommandRetryPolicy.DEFAULT);
        PendingCommand previous = pending.put(key, command);
        if (previous != null) {
            previous.cancelled = true;
            log.info("↪️ 최신 명령으로 교체: {}", key);
        }
        dispatch(command);
    }

    public void onPacketReceived(byte[] packet) {
        pending.values().forEach(command -> {
            if (!command.cancelled && command.ackMatcher != null && command.ackMatcher.matches(packet)) {
                complete(command, "ACKNOWLEDGED");
            }
        });
    }

    public void onStateUpdated(String deviceType, int deviceIndex, Map<String, String> state) {
        state.forEach((field, value) -> {
            PendingCommand command = pending.get(key(deviceType, deviceIndex, field));
            if (command != null && !command.cancelled && command.ackMatcher == null
                    && command.payload.equalsIgnoreCase(value)) {
                complete(command, "STATE_CONFIRMED");
            }
        });
    }

    private void dispatch(PendingCommand command) {
        if (command.cancelled || pending.get(command.key) != command) return;
        command.attempts++;
        ew11Transport.send(command.packet);
        log.info("📤 명령 전송 [{}] {} ({}/{})", command.id, command.key,
                command.attempts, command.policy.maxRetries() + 1);
        executor.schedule(() -> onTimeout(command), command.policy.ackTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    private void onTimeout(PendingCommand command) {
        if (command.cancelled || pending.get(command.key) != command) return;
        if (command.attempts > command.policy.maxRetries()) {
            pending.remove(command.key, command);
            log.warn("❌ 명령 확인 실패 [{}] {}: {}회 전송", command.id, command.key, command.attempts);
            return;
        }
        executor.schedule(() -> dispatch(command), command.policy.retryDelayMs(), TimeUnit.MILLISECONDS);
    }

    private void complete(PendingCommand command, String result) {
        if (pending.remove(command.key, command)) {
            command.cancelled = true;
            log.info("✅ 명령 {} [{}] {}", result, command.id, command.key);
        }
    }

    private String key(String deviceType, int deviceIndex, String field) {
        return deviceType + deviceIndex + "/" + field;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static final class PendingCommand {
        private final String id;
        private final String key;
        private final String payload;
        private final byte[] packet;
        private final AckMatcher ackMatcher;
        private final CommandRetryPolicy policy;
        private volatile boolean cancelled;
        private int attempts;

        private PendingCommand(String id, String key, String payload, byte[] packet,
                AckMatcher ackMatcher, CommandRetryPolicy policy) {
            this.id = id;
            this.key = key;
            this.payload = payload;
            this.packet = packet.clone();
            this.ackMatcher = ackMatcher;
            this.policy = policy;
        }
    }
}

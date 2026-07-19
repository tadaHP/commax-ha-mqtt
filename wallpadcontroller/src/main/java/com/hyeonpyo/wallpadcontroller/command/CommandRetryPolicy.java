package com.hyeonpyo.wallpadcontroller.command;

/** H2M-compatible defaults: ACK wait, retry delay, and retry count. */
public record CommandRetryPolicy(long ackTimeoutMs, long retryDelayMs, int maxRetries) {
    public static final CommandRetryPolicy DEFAULT = new CommandRetryPolicy(200, 10, 5);

    public CommandRetryPolicy {
        if (ackTimeoutMs < 1 || retryDelayMs < 0 || maxRetries < 0) {
            throw new IllegalArgumentException("Invalid command retry policy");
        }
    }
}

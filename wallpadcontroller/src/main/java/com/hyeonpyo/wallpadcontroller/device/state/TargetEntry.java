package com.hyeonpyo.wallpadcontroller.device.state;

import lombok.Getter;

@Getter
public class TargetEntry {
    private final String targetValue;
    private int retryCount;

    public TargetEntry(String targetValue) {
        this.targetValue = targetValue;
        this.retryCount = 0;
    }
    public void incrementRetry() { retryCount++; }
}

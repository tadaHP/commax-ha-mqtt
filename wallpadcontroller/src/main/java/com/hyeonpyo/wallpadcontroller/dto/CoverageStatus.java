package com.hyeonpyo.wallpadcontroller.dto;

public enum CoverageStatus {
    COMPLETE,   // 모든 정의된 값이 수신됨
    PARTIAL,    // 일부 값만 수신됨
    NEW_DETECTED, // 새로운 값이 감지됨
    MISSING     // 한번도 수신되지 않음
}

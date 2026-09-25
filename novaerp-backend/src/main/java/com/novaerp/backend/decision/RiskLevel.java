package com.novaerp.backend.decision;

public enum RiskLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INACTIVE,

    // Legacy values preserved for backward-compatible API queries
    OUT_OF_STOCK,
    WARNING,
    NORMAL
}

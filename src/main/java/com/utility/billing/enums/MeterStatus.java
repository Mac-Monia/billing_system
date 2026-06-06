package com.utility.billing.enums;

public enum MeterStatus {
    ACTIVE,
    INACTIVE,
    DISCONNECTED;

    public boolean allowsReadings() {
        return this == ACTIVE;
    }
}

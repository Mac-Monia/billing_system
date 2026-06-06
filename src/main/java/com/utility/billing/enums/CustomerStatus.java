package com.utility.billing.enums;

public enum CustomerStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED;

    public boolean canReceiveBills() {
        return this == ACTIVE;
    }
}

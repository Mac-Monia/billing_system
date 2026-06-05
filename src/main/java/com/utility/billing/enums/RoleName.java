package com.utility.billing.enums;

public enum RoleName {
    ADMIN,
    OPERATOR,
    FINANCE,
    CUSTOMER;

    public String authority() {
        return "ROLE_" + name();
    }
}

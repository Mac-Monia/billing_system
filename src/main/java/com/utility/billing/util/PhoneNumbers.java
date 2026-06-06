package com.utility.billing.util;

import com.utility.billing.validation.ValidationPatterns;

public final class PhoneNumbers {

    public static final String DEFAULT_COUNTRY_CODE = "+250";

    private PhoneNumbers() {
    }

    public static String normalizeLocalNumber(String localPhone) {
        if (localPhone == null) {
            return "";
        }
        String digits = localPhone.trim().replaceAll("\\s+", "");
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return digits;
    }

    public static String resolveCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return DEFAULT_COUNTRY_CODE;
        }
        return countryCode.trim();
    }

    public static String toE164(String countryCode, String localPhone) {
        String code = resolveCountryCode(countryCode);
        String local = normalizeLocalNumber(localPhone);
        String full = code + local;
        if (!full.matches(ValidationPatterns.RWANDA_PHONE)) {
            throw new IllegalArgumentException(ValidationPatterns.RWANDA_PHONE_MESSAGE);
        }
        return full;
    }
}

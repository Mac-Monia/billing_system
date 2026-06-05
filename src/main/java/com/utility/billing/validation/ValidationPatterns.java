package com.utility.billing.validation;

public final class ValidationPatterns {

    public static final String NATIONAL_ID = "^\\d{16}$";
    public static final String NATIONAL_ID_MESSAGE = "National ID must contain exactly 16 digits";

    public static final String RWANDA_PHONE = "^\\+2507[2389][0-9]{7}$";
    public static final String RWANDA_PHONE_MESSAGE = "Phone number must be in format +250788310922";

    public static final String METER_NUMBER = "^(WM|EM)-\\d{5}$";
    public static final String METER_NUMBER_MESSAGE = "Meter number must be in format WM-10001 or EM-10001";

    private ValidationPatterns() {
    }
}

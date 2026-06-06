package com.utility.billing.validation;

public final class ValidationPatterns {

    public static final String NATIONAL_ID = "^\\d{16}$";
    public static final String NATIONAL_ID_MESSAGE = "National ID must contain exactly 16 digits";

    public static final String RWANDA_PHONE = "^\\+2507[2389][0-9]{7}$";
    public static final String RWANDA_PHONE_MESSAGE = "Phone number must be in format +250788310922";

    public static final String RWANDA_LOCAL_PHONE = "^7[2389][0-9]{7}$";
    public static final String RWANDA_LOCAL_PHONE_MESSAGE = "Phone number must be 9 digits starting with 7 (e.g. 788310922)";

    public static final String COUNTRY_CODE = "^\\+[1-9][0-9]{0,3}$";
    public static final String COUNTRY_CODE_MESSAGE = "Country code must start with + (default +250 for Rwanda)";

    public static final String STRONG_PASSWORD =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!_.\\-])[A-Za-z\\d@#$%^&+=!_.\\-]{8,}$";
    public static final String STRONG_PASSWORD_MESSAGE =
            "Password must be at least 8 characters and include uppercase, lowercase, a digit, and a symbol";

    public static final String METER_NUMBER = "^(WM|EM)-\\d{5}$";
    public static final String METER_NUMBER_MESSAGE = "Meter number must be in format WM-10001 or EM-10001";

    private ValidationPatterns() {
    }
}

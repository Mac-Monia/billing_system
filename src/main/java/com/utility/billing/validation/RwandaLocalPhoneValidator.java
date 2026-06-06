package com.utility.billing.validation;

import com.utility.billing.util.PhoneNumbers;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RwandaLocalPhoneValidator implements ConstraintValidator<RwandaLocalPhone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = PhoneNumbers.normalizeLocalNumber(value);
        return normalized.matches(ValidationPatterns.RWANDA_LOCAL_PHONE);
    }
}

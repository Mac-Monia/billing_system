package com.utility.billing.validation;

import com.utility.billing.dto.request.ChangePasswordRequest;
import com.utility.billing.dto.request.CustomerRequest;
import com.utility.billing.dto.request.MeterReadingRequest;
import com.utility.billing.dto.request.MeterRequest;
import com.utility.billing.dto.request.PaymentRequest;
import com.utility.billing.dto.request.RegisterRequest;
import com.utility.billing.enums.PaymentMethod;
import com.utility.billing.enums.MeterType;
import com.utility.billing.util.PhoneNumbers;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validNationalId_passes() {
        CustomerRequest request = validCustomer();
        request.setNationalId("1234567890123456");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void invalidNationalId_tooShort_fails() {
        CustomerRequest request = validCustomer();
        request.setNationalId("123456789");
        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nationalId")));
    }

    @Test
    void invalidNationalId_withLetters_fails() {
        CustomerRequest request = validCustomer();
        request.setNationalId("ABC1234567890123");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void validSplitPhone_passes() {
        CustomerRequest request = validCustomer();
        request.setCountryCode("+250");
        request.setPhone("788310922");
        assertTrue(validator.validate(request).isEmpty());
        assertEquals("+250788310922", request.getPhoneNumber());
    }

    @Test
    void splitPhone_defaultCountryCode_combinesToE164() {
        CustomerRequest request = validCustomer();
        request.setCountryCode(null);
        request.setPhone("722123456");
        assertEquals("+250722123456", request.getPhoneNumber());
    }

    @Test
    void invalidPhone_localFormatWithLeadingZero_normalizedAndPasses() {
        CustomerRequest request = validCustomer();
        request.setPhone("0788310922");
        assertTrue(validator.validate(request).isEmpty());
        assertEquals("+250788310922", request.getPhoneNumber());
    }

    @Test
    void invalidPhone_badLocalPrefix_fails() {
        CustomerRequest request = validCustomer();
        request.setPhone("688310922");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void underageCustomer_fails() {
        CustomerRequest request = validCustomer();
        request.setDateOfBirth(LocalDate.now().minusYears(17));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void registerRequest_validPhoneAndPassword_passes() {
        RegisterRequest request = validRegister();
        assertTrue(validator.validate(request).isEmpty());
        assertEquals("+250722123456", request.getPhoneNumber());
    }

    @Test
    void registerRequest_weakPassword_fails() {
        RegisterRequest request = validRegister();
        request.setPassword("password");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void changePasswordRequest_strongPassword_passes() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass1!");
        request.setNewPassword("NewPass1!");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void changePasswordRequest_missingSymbol_fails() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass1!");
        request.setNewPassword("NewPass12");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void meterRequest_valid_passes() {
        MeterRequest request = validMeter();
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void meterRequest_futureInstallationDate_fails() {
        MeterRequest request = validMeter();
        request.setInstallationDate(LocalDate.now().plusDays(1));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void meterRequest_invalidMeterNumber_fails() {
        MeterRequest request = validMeter();
        request.setMeterNumber("INVALID");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void meterReadingRequest_valid_passes() {
        MeterReadingRequest request = validMeterReading();
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void meterReadingRequest_futureDate_fails() {
        MeterReadingRequest request = validMeterReading();
        request.setReadingDate(LocalDate.now().plusDays(1));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void meterReadingRequest_negativeReading_fails() {
        MeterReadingRequest request = validMeterReading();
        request.setPreviousReading(new BigDecimal("-1"));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void paymentRequest_valid_passes() {
        PaymentRequest request = validPayment();
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void paymentRequest_futureDate_fails() {
        PaymentRequest request = validPayment();
        request.setPaymentDate(LocalDate.now().plusDays(1));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void paymentRequest_zeroAmount_fails() {
        PaymentRequest request = validPayment();
        request.setAmount(BigDecimal.ZERO);
        assertFalse(validator.validate(request).isEmpty());
    }

    private PaymentRequest validPayment() {
        PaymentRequest request = new PaymentRequest();
        request.setBillReference("BILL-A1B2C3D4");
        request.setAmount(new BigDecimal("15000.00"));
        request.setPaymentMethod(PaymentMethod.MOMO);
        request.setPaymentDate(LocalDate.of(2026, 6, 5));
        return request;
    }

    private MeterReadingRequest validMeterReading() {
        MeterReadingRequest request = new MeterReadingRequest();
        request.setMeterId(1L);
        request.setPreviousReading(new BigDecimal("500.00"));
        request.setCurrentReading(new BigDecimal("650.00"));
        request.setReadingDate(LocalDate.of(2026, 5, 1));
        return request;
    }

    private MeterRequest validMeter() {
        MeterRequest request = new MeterRequest();
        request.setCustomerId(1L);
        request.setMeterNumber("WM-10001");
        request.setMeterType(MeterType.WATER);
        request.setInstallationDate(LocalDate.of(2024, 1, 15));
        return request;
    }

    private CustomerRequest validCustomer() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Jean");
        request.setLastName("Uwase");
        request.setNationalId("1234567890123456");
        request.setEmail("jean@example.com");
        request.setCountryCode(PhoneNumbers.DEFAULT_COUNTRY_CODE);
        request.setPhone("788310922");
        request.setAddress("Kigali");
        request.setDateOfBirth(LocalDate.of(1995, 6, 1));
        return request;
    }

    private RegisterRequest validRegister() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Jean");
        request.setLastName("Uwase");
        request.setEmail("jean@example.com");
        request.setCountryCode(PhoneNumbers.DEFAULT_COUNTRY_CODE);
        request.setPhone("722123456");
        request.setPassword("Password1!");
        request.setNationalId("1199780034567890");
        request.setAddress("Kigali");
        request.setDateOfBirth(LocalDate.of(1995, 1, 1));
        return request;
    }
}

package com.utility.billing.validation;

import com.utility.billing.dto.request.CustomerRequest;
import com.utility.billing.dto.request.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void validPhone_passes() {
        CustomerRequest request = validCustomer();
        request.setPhoneNumber("+250788310922");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void invalidPhone_localFormat_fails() {
        CustomerRequest request = validCustomer();
        request.setPhoneNumber("0788310922");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void invalidPhone_missingPlus_fails() {
        CustomerRequest request = validCustomer();
        request.setPhoneNumber("250788310922");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void underageCustomer_fails() {
        CustomerRequest request = validCustomer();
        request.setDateOfBirth(LocalDate.now().minusYears(17));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void registerRequest_validPhoneAndNationalId_passes() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Jean");
        request.setLastName("Uwase");
        request.setEmail("jean@example.com");
        request.setPhoneNumber("+250722123456");
        request.setPassword("password123");
        request.setNationalId("1199780034567890");
        request.setAddress("Kigali");
        request.setDateOfBirth(LocalDate.of(1995, 1, 1));
        assertTrue(validator.validate(request).isEmpty());
    }

    private CustomerRequest validCustomer() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Jean");
        request.setLastName("Uwase");
        request.setNationalId("1234567890123456");
        request.setEmail("jean@example.com");
        request.setPhoneNumber("+250788310922");
        request.setAddress("Kigali");
        request.setDateOfBirth(LocalDate.of(1995, 6, 1));
        return request;
    }
}

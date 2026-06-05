package com.utility.billing.service;

import com.utility.billing.dto.request.CustomerRequest;
import com.utility.billing.entity.Customer;
import com.utility.billing.exception.DuplicateResourceException;
import com.utility.billing.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceValidationTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private MeterRepository meterRepository;
    @Mock private MeterReadingRepository meterReadingRepository;
    @Mock private BillRepository billRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private DuplicateCheckService duplicateCheckService;
    @Mock private AuditService auditService;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CustomerRequest();
        validRequest.setFirstName("Jean");
        validRequest.setLastName("Uwase");
        validRequest.setNationalId("1234567890123456");
        validRequest.setEmail("jean@example.com");
        validRequest.setPhoneNumber("+250788310922");
        validRequest.setAddress("Kigali");
        validRequest.setDateOfBirth(LocalDate.of(1995, 1, 1));
    }

    @Test
    void create_duplicateNationalId_throws() {
        doThrow(new DuplicateResourceException("National ID already exists"))
                .when(duplicateCheckService).assertUniqueCustomerNationalId("1234567890123456", null);

        assertThrows(DuplicateResourceException.class, () -> customerService.create(validRequest));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void create_duplicatePhone_throws() {
        doNothing().when(duplicateCheckService).assertUniqueCustomerNationalId(any(), any());
        doNothing().when(duplicateCheckService).assertUniqueCustomerEmail(any(), any());
        doThrow(new DuplicateResourceException("Phone number already exists"))
                .when(duplicateCheckService).assertUniqueCustomerPhone("+250788310922", null);

        assertThrows(DuplicateResourceException.class, () -> customerService.create(validRequest));
    }

    @Test
    void update_duplicateEmailOnOtherCustomer_throws() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(Customer.builder().id(1L).email("old@example.com").build()));
        doNothing().when(duplicateCheckService).assertUniqueCustomerNationalId(any(), eq(1L));
        doThrow(new DuplicateResourceException("Email already exists"))
                .when(duplicateCheckService).assertUniqueCustomerEmail("jean@example.com", 1L);

        assertThrows(DuplicateResourceException.class, () -> customerService.update(1L, validRequest));
    }
}

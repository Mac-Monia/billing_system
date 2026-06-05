package com.utility.billing.service;

import com.utility.billing.dto.request.CustomerRequest;
import com.utility.billing.entity.Customer;
import com.utility.billing.enums.AuditActionType;
import com.utility.billing.enums.CustomerStatus;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.exception.ResourceNotFoundException;
import com.utility.billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DuplicateCheckService duplicateCheckService;
    private final AuditService auditService;

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        validateAdult(request.getDateOfBirth());
        duplicateCheckService.assertUniqueCustomerNationalId(request.getNationalId(), null);
        duplicateCheckService.assertUniqueCustomerEmail(request.getEmail(), null);
        duplicateCheckService.assertUniqueCustomerPhone(request.getPhoneNumber(), null);

        Customer customer = mapToEntity(request, new Customer());
        customer = customerRepository.save(customer);
        auditService.log(AuditActionType.CREATE, "Customer", customer.getId(), null, customer.getEmail());
        return customer;
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = findById(id);
        validateAdult(request.getDateOfBirth());
        duplicateCheckService.assertUniqueCustomerNationalId(request.getNationalId(), id);
        duplicateCheckService.assertUniqueCustomerEmail(request.getEmail(), id);
        duplicateCheckService.assertUniqueCustomerPhone(request.getPhoneNumber(), id);

        String oldEmail = customer.getEmail();
        mapToEntity(request, customer);
        customer = customerRepository.save(customer);
        auditService.log(AuditActionType.UPDATE, "Customer", customer.getId(), oldEmail, customer.getEmail());
        return customer;
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findById(id);
        userRepository.findByCustomerId(id).forEach(user -> {
            user.setCustomer(null);
            userRepository.save(user);
        });
        notificationRepository.deleteByCustomerId(id);
        billRepository.findByCustomerId(id).forEach(bill -> paymentRepository.deleteByBillId(bill.getId()));
        billRepository.deleteByCustomerId(id);
        meterRepository.findByCustomerId(id).forEach(meter -> meterReadingRepository.deleteByMeterId(meter.getId()));
        meterRepository.deleteByCustomerId(id);
        customerRepository.delete(customer);
        auditService.log(AuditActionType.DELETE, "Customer", id, customer.getEmail(), null);
    }

    private Customer mapToEntity(CustomerRequest request, Customer customer) {
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setNationalId(request.getNationalId());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        } else if (customer.getStatus() == null) {
            customer.setStatus(CustomerStatus.ACTIVE);
        }
        return customer;
    }

    private void validateAdult(LocalDate dateOfBirth) {
        if (dateOfBirth != null && Period.between(dateOfBirth, LocalDate.now()).getYears() < 18) {
            throw new BusinessRuleException("Customer must be at least 18 years old");
        }
    }
}

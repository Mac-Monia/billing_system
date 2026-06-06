package com.utility.billing.service;

import com.utility.billing.exception.DuplicateResourceException;
import com.utility.billing.repository.CustomerRepository;
import com.utility.billing.repository.MeterRepository;
import com.utility.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DuplicateCheckService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final MeterRepository meterRepository;

    public void assertUniqueCustomerNationalId(String nationalId, Long excludeId) {
        if (excludeId == null) {
            if (customerRepository.existsByNationalId(nationalId)) {
                throw new DuplicateResourceException("National ID already exists: " + nationalId);
            }
        } else if (customerRepository.existsByNationalIdAndIdNot(nationalId, excludeId)) {
            throw new DuplicateResourceException("National ID already exists: " + nationalId);
        }
    }

    public void assertUniqueCustomerEmail(String email, Long excludeCustomerId) {
        if (excludeCustomerId == null) {
            if (customerRepository.existsByEmail(email)) {
                throw new DuplicateResourceException("Email already exists: " + email);
            }
        } else if (customerRepository.existsByEmailAndIdNot(email, excludeCustomerId)) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        userRepository.findByEmail(email).ifPresent(user -> {
            if (excludeCustomerId == null
                    || user.getCustomer() == null
                    || !user.getCustomer().getId().equals(excludeCustomerId)) {
                throw new DuplicateResourceException("Email already registered: " + email);
            }
        });
    }

    public void assertUniqueCustomerPhone(String phoneNumber, Long excludeId) {
        if (excludeId == null) {
            if (customerRepository.existsByPhoneNumber(phoneNumber)) {
                throw new DuplicateResourceException("Phone number already exists: " + phoneNumber);
            }
        } else if (customerRepository.existsByPhoneNumberAndIdNot(phoneNumber, excludeId)) {
            throw new DuplicateResourceException("Phone number already exists: " + phoneNumber);
        }
    }

    public void assertUniqueUserEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }
    }

    public void assertUniqueMeterNumber(String meterNumber, Long excludeId) {
        if (excludeId == null) {
            if (meterRepository.existsByMeterNumber(meterNumber)) {
                throw new DuplicateResourceException("Meter number already exists: " + meterNumber);
            }
        } else if (meterRepository.existsByMeterNumberAndIdNot(meterNumber, excludeId)) {
            throw new DuplicateResourceException("Meter number already exists: " + meterNumber);
        }
    }
}

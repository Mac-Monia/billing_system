package com.utility.billing.controller;

import com.utility.billing.entity.Bill;
import com.utility.billing.service.BillService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bills", description = "Bill generation, approval, and customer views")
public class BillController {

    private final BillService billService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    public Bill findById(@PathVariable Long id) {
        return billService.findById(id);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    public List<Bill> findByCustomer(@PathVariable Long customerId) {
        return billService.findByCustomerId(customerId);
    }

    @PostMapping("/generate/{readingId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public Bill generate(@PathVariable Long readingId) {
        return billService.generateFromReading(readingId);
    }

    @PutMapping("/{billId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public Bill approve(@PathVariable Long billId) {
        return billService.approve(billId);
    }
}

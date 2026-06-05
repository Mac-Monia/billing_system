package com.utility.billing.controller;

import com.utility.billing.dto.request.MeterRequest;
import com.utility.billing.entity.Meter;
import com.utility.billing.service.MeterService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Meters", description = "Meter management")
public class MeterController {

    private final MeterService meterService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Meter> findAll() {
        return meterService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Meter findById(@PathVariable Long id) {
        return meterService.findById(id);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    public List<Meter> findByCustomer(@PathVariable Long customerId) {
        return meterService.findByCustomerId(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Meter create(@Valid @RequestBody MeterRequest request) {
        return meterService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Meter update(@PathVariable Long id, @Valid @RequestBody MeterRequest request) {
        return meterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        meterService.delete(id);
    }
}

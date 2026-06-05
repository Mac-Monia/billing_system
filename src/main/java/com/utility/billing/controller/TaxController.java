package com.utility.billing.controller;

import com.utility.billing.dto.request.TaxConfigurationRequest;
import com.utility.billing.entity.TaxConfiguration;
import com.utility.billing.service.TaxService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taxes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Taxes", description = "Tax configuration (Admin)")
public class TaxController {

    private final TaxService taxService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public List<TaxConfiguration> findAll() {
        return taxService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TaxConfiguration create(@Valid @RequestBody TaxConfigurationRequest request) {
        return taxService.create(request);
    }
}

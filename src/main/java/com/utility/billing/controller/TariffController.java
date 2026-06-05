package com.utility.billing.controller;

import com.utility.billing.dto.request.TariffRequest;
import com.utility.billing.entity.Tariff;
import com.utility.billing.enums.MeterType;
import com.utility.billing.service.TariffService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tariffs", description = "Tariff configuration with VAT and late penalty")
public class TariffController {

    private final TariffService tariffService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Tariff> findAll() {
        return tariffService.findAll();
    }

    @GetMapping("/active/{meterType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    public Tariff findActive(@PathVariable MeterType meterType) {
        return tariffService.findActiveTariff(meterType);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Tariff findById(@PathVariable Long id) {
        return tariffService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Tariff create(@Valid @RequestBody TariffRequest request) {
        return tariffService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Tariff update(@PathVariable Long id, @Valid @RequestBody TariffRequest request) {
        return tariffService.update(id, request);
    }
}

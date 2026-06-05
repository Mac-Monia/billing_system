package com.utility.billing.controller;

import com.utility.billing.dto.request.PenaltyConfigurationRequest;
import com.utility.billing.entity.PenaltyConfiguration;
import com.utility.billing.service.PenaltyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Penalties", description = "Penalty configuration (Admin)")
public class PenaltyController {

    private final PenaltyService penaltyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public List<PenaltyConfiguration> findAll() {
        return penaltyService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public PenaltyConfiguration create(@Valid @RequestBody PenaltyConfigurationRequest request) {
        return penaltyService.create(request);
    }
}

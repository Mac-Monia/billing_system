package com.utility.billing.controller;

import com.utility.billing.dto.request.MeterReadingRequest;
import com.utility.billing.entity.MeterReading;
import com.utility.billing.service.MeterReadingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/readings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Meter Readings", description = "Operator captures readings; bill auto-generated")
public class ReadingController {

    private final MeterReadingService meterReadingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public List<MeterReading> findAll() {
        return meterReadingService.findAll();
    }

    @GetMapping("/meter/{meterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public List<MeterReading> findByMeter(@PathVariable Long meterId) {
        return meterReadingService.findByMeterId(meterId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPERATOR')")
    public MeterReading capture(@Valid @RequestBody MeterReadingRequest request) {
        return meterReadingService.capture(request);
    }
}

package com.utility.billing.entity;

import com.utility.billing.enums.MeterType;
import com.utility.billing.enums.TariffType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tariffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false)
    private MeterType meterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tariff_type", nullable = false)
    private TariffType tariffType;

    @Column(name = "flat_rate", precision = 12, scale = 4)
    private BigDecimal flatRate;

    @Column(name = "fixed_charge", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal fixedCharge = BigDecimal.ZERO;

    @Column(name = "vat_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatPercent = BigDecimal.ZERO;

    @Column(name = "late_penalty_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal latePenaltyPercent = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "tariff", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TariffTier> tiers = new ArrayList<>();
}

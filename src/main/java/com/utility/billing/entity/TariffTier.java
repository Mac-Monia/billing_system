package com.utility.billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tariff_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    @Column(name = "min_consumption", nullable = false, precision = 12, scale = 2)
    private BigDecimal minConsumption;

    @Column(name = "max_consumption", precision = 12, scale = 2)
    private BigDecimal maxConsumption;

    @Column(name = "rate_per_unit", nullable = false, precision = 12, scale = 4)
    private BigDecimal ratePerUnit;
}

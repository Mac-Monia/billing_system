package com.utility.billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailProperties {
    private String from = "noreply@wasac.rw";
    private String fromName = "WASAC/REG Utility Billing";
    private String baseUrl = "http://localhost:8080";
}

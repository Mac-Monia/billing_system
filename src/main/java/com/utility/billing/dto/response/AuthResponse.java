package com.utility.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String type;
    private Long userId;
    private String email;
    private String fullNames;
    private Set<String> roles;
    private boolean forcePasswordChange;
    private boolean passwordExpired;
    private boolean emailVerified;
    private String message;
}

package com.utility.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateResponse {
    private Long userId;
    private String email;
    private boolean passwordExpired;
    private boolean forcePasswordChange;
    private String message;
}

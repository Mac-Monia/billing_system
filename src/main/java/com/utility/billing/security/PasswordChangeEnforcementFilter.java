package com.utility.billing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utility.billing.entity.User;
import com.utility.billing.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PasswordChangeEnforcementFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_PREFIXES = List.of(
            "/api/auth/change-password",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/verify-otp",
            "/api/auth/resend-otp",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs"
    );

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal() instanceof UserPrincipal principal
                && !isAllowedPath(request.getRequestURI())) {

            User user = userRepository.findById(principal.getId()).orElse(null);
            if (user != null && Boolean.TRUE.equals(user.getForcePasswordChange())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Map<String, Object> body = new HashMap<>();
                body.put("success", false);
                body.put("message", "Password change required. Use POST /api/auth/change-password.");
                body.put("forcePasswordChange", true);
                objectMapper.writeValue(response.getOutputStream(), body);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedPath(String uri) {
        return ALLOWED_PREFIXES.stream().anyMatch(uri::startsWith);
    }
}

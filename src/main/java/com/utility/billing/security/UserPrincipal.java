package com.utility.billing.security;

import com.utility.billing.entity.User;
import com.utility.billing.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final boolean accountLocked;
    private final boolean passwordExpired;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.status = user.getStatus();
        this.accountLocked = Boolean.TRUE.equals(user.getAccountLocked());
        this.passwordExpired = Boolean.TRUE.equals(user.getPasswordExpired());
        this.authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().authority()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked && status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Temporary passwords remain valid for login; forcePasswordChange filter enforces change.
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}

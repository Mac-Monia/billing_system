package com.utility.billing.config;

import com.utility.billing.entity.Role;
import com.utility.billing.entity.User;
import com.utility.billing.enums.RoleName;
import com.utility.billing.enums.UserStatus;
import com.utility.billing.repository.RoleRepository;
import com.utility.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        runSqlScript("db/schema-migration.sql", "Schema migration");
        migrateLegacyRoles();

        Arrays.stream(RoleName.values()).forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
            }
        });

        if (userRepository.findByEmail("admin@wasac.com").isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
            User admin = User.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .email("admin@wasac.com")
                    .phoneNumber("+250788000001")
                    .password(passwordEncoder.encode("admin123"))
                    .status(UserStatus.ACTIVE)
                    .seededAdmin(true)
                    .emailVerified(true)
                    .passwordExpired(false)
                    .forcePasswordChange(false)
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(admin);
            log.info("Seeded default admin user: admin@wasac.com");
        }
    }

    private void migrateLegacyRoles() {
        runSqlScript("db/role-migration.sql", "Legacy role migration");
    }

    private void runSqlScript(String classpath, String label) {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource(classpath));
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            log.info("{} applied", label);
        } catch (Exception ex) {
            log.warn("{} skipped: {}", label, ex.getMessage());
        }
    }
}

package com.utility.billing.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DatabaseRoutineInitializer {

    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void installRoutines() {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/schema-migration.sql"));
            populator.addScript(new ClassPathResource("db/role-migration.sql"));
            populator.addScript(new ClassPathResource("db/constraints.sql"));
            populator.addScript(new ClassPathResource("db/routines.sql"));
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            log.info("Database routines (triggers and stored procedures) installed");
        } catch (Exception ex) {
            log.warn("Could not install database routines: {}", ex.getMessage());
        }
    }
}

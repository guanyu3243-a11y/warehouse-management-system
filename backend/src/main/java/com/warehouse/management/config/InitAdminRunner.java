package com.warehouse.management.config;

import com.warehouse.management.service.InitAdminService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitAdminRunner implements ApplicationRunner {

    private final InitAdminProperties properties;

    private final InitAdminService initAdminService;

    public InitAdminRunner(
            InitAdminProperties properties,
            InitAdminService initAdminService
    ) {
        this.properties = properties;
        this.initAdminService = initAdminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        initAdminService.initializeIfNecessary();
    }
}

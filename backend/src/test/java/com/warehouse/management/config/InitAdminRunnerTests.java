package com.warehouse.management.config;

import com.warehouse.management.service.InitAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InitAdminRunnerTests {

    @Mock
    private InitAdminService initAdminService;

    private InitAdminProperties properties;

    private InitAdminRunner runner;

    @BeforeEach
    void setUp() {
        properties = new InitAdminProperties();
        runner = new InitAdminRunner(properties, initAdminService);
    }

    @Test
    void runDoesNothingWhenDisabled() {
        properties.setEnabled(false);

        runner.run(new DefaultApplicationArguments());

        verify(initAdminService, never()).initializeIfNecessary();
    }

    @Test
    void runDelegatesWhenEnabled() {
        properties.setEnabled(true);

        runner.run(new DefaultApplicationArguments());

        verify(initAdminService).initializeIfNecessary();
    }
}

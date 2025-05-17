package jaeik.growfarm.integration;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Configuration class for integration tests.
 * This class ensures that integration tests use the real database and services
 * instead of mocks.
 */
@TestConfiguration
public class IntegrationTestConfig {
    
    // This configuration doesn't need to define any beans as it will use the real beans
    // from the application context. The purpose of this class is to document the
    // integration test configuration and potentially add custom beans if needed in the future.
    
}
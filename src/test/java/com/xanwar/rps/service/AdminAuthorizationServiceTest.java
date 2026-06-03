package com.xanwar.rps.service;

import com.xanwar.rps.config.AdminProperties;
import com.xanwar.rps.config.TornApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthorizationServiceTest {

    private AdminAuthorizationService service;

    @BeforeEach
    void setUp() {
        AdminProperties adminProperties = new AdminProperties();
        adminProperties.setSecretKey("admin-secret-123");

        TornApiProperties tornApiProperties = new TornApiProperties();
        tornApiProperties.setMyKey("torn-api-key-456");

        service = new AdminAuthorizationService(adminProperties, tornApiProperties);
    }

    @Test
    void authorizedWithAdminSecretKey() {
        assertThat(service.isAuthorized("admin-secret-123")).isTrue();
    }

    @Test
    void authorizedWithTornApiKey() {
        assertThat(service.isAuthorized("torn-api-key-456")).isTrue();
    }

    @Test
    void rejectedWithWrongKey() {
        assertThat(service.isAuthorized("wrong-key")).isFalse();
    }

    @Test
    void rejectedWithNull() {
        assertThat(service.isAuthorized(null)).isFalse();
    }

    @Test
    void rejectedWithBlankString() {
        assertThat(service.isAuthorized("")).isFalse();
        assertThat(service.isAuthorized("   ")).isFalse();
    }

    @Test
    void rejectedWhenBothKeysAreNull() {
        AdminProperties adminProperties = new AdminProperties();
        TornApiProperties tornApiProperties = new TornApiProperties();
        AdminAuthorizationService noKeysService = new AdminAuthorizationService(adminProperties, tornApiProperties);

        assertThat(noKeysService.isAuthorized("anything")).isFalse();
    }
}

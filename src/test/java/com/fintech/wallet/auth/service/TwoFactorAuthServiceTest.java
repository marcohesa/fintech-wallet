package com.fintech.wallet.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TwoFactorAuthServiceTest {

    private TwoFactorAuthService twoFactorAuthService;

    @BeforeEach
    void setUp() {
        twoFactorAuthService = new TwoFactorAuthService();
    }

    @Test
    void shouldGenerateValidSecret() {
        String secret = twoFactorAuthService.generateNewSecret();
        assertThat(secret).isNotNull().isNotEmpty();
    }

    @Test
    void shouldGenerateDataUriForQrCode() {
        String secret = twoFactorAuthService.generateNewSecret();
        String qrDataUri = twoFactorAuthService.generateQrCodeDataUri(secret, "test@wallet.com");

        assertThat(qrDataUri).isNotNull();
        assertThat(qrDataUri).startsWith("data:image/png;base64,");
    }
}
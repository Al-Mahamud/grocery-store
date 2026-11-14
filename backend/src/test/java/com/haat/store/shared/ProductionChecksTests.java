package com.haat.store.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionChecksTests {

    static MockEnvironment good() {
        return new MockEnvironment()
                .withProperty("store.web-url", "https://haat.example")
                .withProperty("spring.datasource.password", "long-random-password")
                .withProperty("server.servlet.session.cookie.secure", "true")
                .withProperty("store.sms.driver", "sslwireless")
                .withProperty("store.payment.sslcommerz.store-id", "haat01live")
                .withProperty("store.payment.sslcommerz.base-url", "https://securepay.sslcommerz.com")
                .withProperty("store.payment.sslcommerz.callback-url", "https://haat.example");
    }

    @Test
    void goodSettingsStart() {
        ProductionChecks checks = new ProductionChecks(good());
        checks.afterPropertiesSet();
        assertThat(checks.warnings()).isEmpty();
    }

    @Test
    void unsafeSettingsAreRefusedWithReasons() {
        MockEnvironment env = good()
                .withProperty("store.web-url", "http://haat.example")
                .withProperty("spring.datasource.password", "")
                .withProperty("server.servlet.session.cookie.secure", "false")
                .withProperty("store.sms.driver", "log")
                .withProperty("store.payment.sslcommerz.callback-url", "http://haat.example");
        ProductionChecks checks = new ProductionChecks(env);
        assertThat(checks.problems()).hasSize(5);
        assertThatThrownBy(checks::afterPropertiesSet).hasMessageContaining("WEB_URL").hasMessageContaining("SMS_DRIVER");
    }

    @Test
    void aStagingServerMayLogTextsAndUseTheSandbox() {
        MockEnvironment env = good()
                .withProperty("store.staging", "true")
                .withProperty("store.sms.driver", "log")
                .withProperty("store.payment.sslcommerz.base-url", "https://sandbox.sslcommerz.com");
        ProductionChecks checks = new ProductionChecks(env);
        assertThat(checks.problems()).isEmpty();
        assertThat(checks.warnings()).anyMatch(w -> w.contains("sandbox")).anyMatch(w -> w.contains("STAGING"));
    }
}

package com.haat.store.shared;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code store.*} in application.yml. */
@ConfigurationProperties("store")
public record StoreProperties(Path storageRoot, String mediaUrl, String webUrl, Delivery delivery, Slots slots,
                              Payment payment, Admin admin, Sms sms, Codes codes) {

    /** Fees in taka. Delivery is free at or above {@code freeOver} after discounts. */
    public record Delivery(int fee, int freeOver, int minimumOrder) {
    }

    /** Delivery windows: start hours, how long each lasts, how many days ahead and how many orders fit. */
    public record Slots(ZoneId zone, List<Integer> starts, int lengthHours, int daysAhead, int capacity,
                        Duration cutoff) {
    }

    /**
     * Online payment: how long an unpaid order keeps its stock and slot, and how many times the
     * customer may open the payment page for one order.
     */
    public record Payment(Duration expiry, int maxAttempts) {
    }

    /** The admin area: when stock counts as low, and the first staff account (from the environment). */
    public record Admin(int lowStock, String firstStaffName, String firstStaffPhone, String firstStaffPassword) {
    }

    /** Text messages: which sender to use. Provider credentials are read by the sender itself. */
    public record Sms(String driver) {
    }

    /** One-time codes that prove a phone number. */
    public record Codes(Duration validFor, int maxAttempts, Duration resendAfter, int perPhonePerHour) {
    }
}

package com.haat.store.shared;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code store.*} in application.yml. */
@ConfigurationProperties("store")
public record StoreProperties(Path storageRoot, String mediaUrl, Delivery delivery, Slots slots) {

    /** Fees in taka. Delivery is free at or above {@code freeOver} after discounts. */
    public record Delivery(int fee, int freeOver, int minimumOrder) {
    }

    /** Delivery windows: start hours, how long each lasts, how many days ahead and how many orders fit. */
    public record Slots(ZoneId zone, List<Integer> starts, int lengthHours, int daysAhead, int capacity,
                        Duration cutoff) {
    }
}

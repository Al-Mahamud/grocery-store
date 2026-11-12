package com.haat.store.notify;

import com.haat.store.shared.StoreProperties;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The texts customers get about an order. Plain English and "Tk" rather than "৳", so each fits in
 * one standard 160-character SMS.
 */
@Component
public class OrderTexts {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private record Order(long id, String number, String phone, LocalDate date, LocalTime start, LocalTime end,
                         int total, String method, String paymentStatus) {

        boolean paid() {
            return paymentStatus.equals("PAID");
        }
    }

    private final JdbcClient jdbc;
    private final SmsOutbox outbox;
    private final String shop;

    OrderTexts(JdbcClient jdbc, SmsOutbox outbox, StoreProperties properties) {
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.shop = properties.webUrl().replaceFirst("^https?://", "").replaceAll("/$", "");
    }

    public void placed(long orderId) {
        order(orderId).ifPresent(o -> outbox.queue(o.phone(), "ORDER_PLACED",
                "Haat: Order " + o.number() + " is confirmed for " + slot(o) + ". Total Tk" + o.total()
                        + (o.paid() ? ", paid." : ", cash on delivery.") + " Track: " + shop + "/track",
                o.id(), false));
    }

    public void onTheWay(long orderId) {
        order(orderId).ifPresent(o -> outbox.queue(o.phone(), "ORDER_ON_THE_WAY",
                "Haat: Your order " + o.number() + " is on its way and arrives by " + time(o.end()) + ". "
                        + (o.paid() ? "It's paid, nothing to pay at the door." : "Please keep Tk" + o.total() + " ready."),
                o.id(), false));
    }

    /** {@code why}: STAFF, or EXPIRED when an online order wasn't paid in time. */
    public void cancelled(long orderId, String why) {
        order(orderId).ifPresent(o -> {
            String tail = o.paid() ? " Your payment of Tk" + o.total() + " will be refunded within 7 days."
                    : why.equals("EXPIRED") ? " It wasn't paid in time, so nothing was charged."
                    : " Call us if this is a surprise.";
            outbox.queue(o.phone(), "ORDER_CANCELLED", "Haat: Order " + o.number() + " was cancelled." + tail, o.id(), false);
        });
    }

    private Optional<Order> order(long id) {
        return jdbc.sql("""
                        select id, number, phone, slot_date, slot_start, slot_end, total, payment_method, payment_status
                          from orders where id = :id""")
                .param("id", id)
                .query((rs, n) -> new Order(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getDate(4).toLocalDate(),
                        rs.getTime(5).toLocalTime(), rs.getTime(6).toLocalTime(), rs.getInt(7), rs.getString(8),
                        rs.getString(9)))
                .optional();
    }

    private static String slot(Order o) {
        return o.date().format(DAY) + ", " + time(o.start()) + "-" + time(o.end());
    }

    private static String time(LocalTime t) {
        int h = t.getHour() % 12;
        return (h == 0 ? 12 : h) + (t.getHour() < 12 ? "am" : "pm");
    }
}

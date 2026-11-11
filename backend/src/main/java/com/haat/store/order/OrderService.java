package com.haat.store.order;

import com.haat.store.shared.ApiException;
import com.haat.store.shared.BangladeshPhone;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Reads orders for the people allowed to see them. */
@Service
public class OrderService {

    public record Item(String slug, String name, String packLabel, String image, int unitPrice, int quantity,
                       int lineTotal) {
    }

    public record Event(String status, String note, Instant at) {
    }

    public record Slot(LocalDate date, LocalTime start, LocalTime end) {
    }

    public record OrderView(String number, String status, String paymentMethod, String paymentStatus,
                            Instant placedAt, String name, String phone, String email, String address, String area,
                            String note, Slot slot, String substitution, List<Item> items, int subtotal,
                            int discount, int deliveryFee, int total, String couponCode, List<Event> events) {
    }

    private record Row(long id, String token, OrderView view) {
    }

    private final JdbcClient jdbc;

    OrderService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** The thank-you page: whoever placed the order holds its access token. */
    public OrderView byToken(String number, String token) {
        Row row = row("o.number = :v", number).orElseThrow(() -> ApiException.notFound("order"));
        if (token == null || !MessageDigest.isEqual(row.token().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.notFound("order");
        }
        return row.view();
    }

    /** Guest tracking: the order number plus the phone number it was placed with. */
    public OrderView track(String number, String phone) {
        String normalised = BangladeshPhone.normalise(phone).orElseThrow(() -> ApiException.notFound("order"));
        Row row = row("o.number = :v", number == null ? "" : number.trim().toUpperCase())
                .filter(r -> r.view().phone().equals(normalised))
                .orElseThrow(() -> ApiException.notFound("order"));
        return row.view();
    }

    public Optional<OrderView> byId(long id) {
        return row("o.id = :v", id).map(Row::view);
    }

    private Optional<Row> row(String where, Object value) {
        Optional<long[]> id = jdbc.sql("select o.id from orders o where " + where).param("v", value)
                .query((rs, n) -> new long[] {rs.getLong(1)}).optional();
        if (id.isEmpty()) {
            return Optional.empty();
        }
        long orderId = id.get()[0];
        List<Item> items = jdbc.sql("""
                        select product_slug, product_name, pack_label, image_url, unit_price, quantity
                          from order_items where order_id = :id order by id""")
                .param("id", orderId)
                .query((rs, n) -> new Item(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), rs.getInt(6), rs.getInt(5) * rs.getInt(6)))
                .list();
        List<Event> events = jdbc.sql("select status, note, created_at from order_events where order_id = :id order by created_at, id")
                .param("id", orderId)
                .query((rs, n) -> new Event(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant()))
                .list();
        return jdbc.sql("""
                        select access_token, number, status, payment_method, payment_status, created_at, name, phone,
                               email, address, area_name, note, slot_date, slot_start, slot_end, substitution,
                               subtotal, discount, delivery_fee, total, coupon_code
                          from orders where id = :id""")
                .param("id", orderId)
                .query((rs, n) -> new Row(orderId, rs.getString(1), new OrderView(rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getTimestamp(6).toInstant(), rs.getString(7),
                        rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12),
                        new Slot(rs.getDate(13).toLocalDate(), rs.getTime(14).toLocalTime(), rs.getTime(15).toLocalTime()),
                        rs.getString(16), items, rs.getInt(17), rs.getInt(18), rs.getInt(19), rs.getInt(20),
                        rs.getString(21), events)))
                .optional();
    }
}

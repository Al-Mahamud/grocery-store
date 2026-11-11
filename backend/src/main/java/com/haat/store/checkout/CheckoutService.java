package com.haat.store.checkout;

import com.haat.store.checkout.CartService.Line;
import com.haat.store.delivery.DeliveryService;
import com.haat.store.shared.ApiException;
import com.haat.store.shared.StoreProperties;
import java.security.SecureRandom;
import java.sql.Date;
import java.sql.Time;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns a bag into an order: checks stock, the minimum order and the slot, then takes the stock. */
@Service
public class CheckoutService {

    public enum PaymentMethod {
        COD, ONLINE
    }

    /** What to do if an item runs out while the order is being packed. */
    public enum Substitution {
        CALL, REPLACE, REMOVE
    }

    public record Customer(String name, String phone, String email, String address, long areaId, String note) {
    }

    public record Quote(int subtotal, int discount, int deliveryFee, int total, String couponCode, String problem) {
    }

    public record Placed(long orderId, String number, String accessToken, int total) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcClient jdbc;
    private final CartService carts;
    private final Coupons coupons;
    private final DeliveryService delivery;
    private final StoreProperties.Delivery fees;
    private final Clock clock;

    CheckoutService(JdbcClient jdbc, CartService carts, Coupons coupons, DeliveryService delivery,
                    StoreProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.carts = carts;
        this.coupons = coupons;
        this.delivery = delivery;
        this.fees = properties.delivery();
        this.clock = clock;
    }

    public Quote quote(UUID cartId) {
        List<Line> lines = carts.lines(cartId);
        int subtotal = lines.stream().filter(l -> l.problem() == null).mapToInt(Line::lineTotal).sum();
        String code = couponCode(cartId);
        Coupons.Applied coupon = null;
        if (code != null && subtotal > 0) {
            try {
                coupon = coupons.apply(code, subtotal, clock.instant());
            } catch (ApiException ignored) {
                // The bag shows the coupon problem; the quote leaves it out.
            }
        }
        return price(subtotal, coupon);
    }

    private Quote price(int subtotal, Coupons.Applied coupon) {
        int discount = coupon == null ? 0 : coupon.discount();
        int afterDiscount = subtotal - discount;
        boolean free = (coupon != null && coupon.freeShipping()) || afterDiscount >= fees.freeOver();
        int fee = subtotal == 0 || free ? 0 : fees.fee();
        String problem = subtotal < fees.minimumOrder()
                ? "The minimum order is ৳" + fees.minimumOrder() + ". Add ৳" + (fees.minimumOrder() - subtotal) + " more."
                : null;
        return new Quote(subtotal, discount, fee, afterDiscount + fee, coupon == null ? null : coupon.code(), problem);
    }

    @Transactional
    public Placed place(UUID cartId, Customer customer, LocalDate slotDate, LocalTime slotStart,
                        Substitution substitution, PaymentMethod method, Long customerId) {
        List<Line> lines = carts.lines(cartId);
        if (lines.isEmpty()) {
            throw CartService.emptyCart();
        }
        DeliveryService.Area area = delivery.area(customer.areaId());

        Map<Long, Integer> stock = new HashMap<>();
        jdbc.sql("select id, stock from packs where id in (:ids) order by id for update")
                .param("ids", lines.stream().map(Line::packId).toList())
                .query(rs -> {
                    stock.put(rs.getLong(1), rs.getInt(2));
                });
        List<Line> short_ = lines.stream()
                .filter(l -> l.problem() == CartService.Problem.UNAVAILABLE || stock.getOrDefault(l.packId(), 0) < l.quantity())
                .toList();
        if (!short_.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "checkout.stock_changed",
                    "Some items are no longer available in the quantity you chose: "
                            + short_.stream().map(l -> l.name() + " (" + l.packLabel() + ")").collect(Collectors.joining(", "))
                            + ". Please review your bag.");
        }

        int subtotal = lines.stream().mapToInt(Line::lineTotal).sum();
        String code = couponCode(cartId);
        Coupons.Applied coupon = code == null ? null : coupons.apply(code, subtotal, clock.instant());
        Quote quote = price(subtotal, coupon);
        if (quote.problem() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "checkout.minimum_order", quote.problem());
        }
        DeliveryService.Slot slot = delivery.reserve(slotDate, slotStart);
        if (coupon != null) {
            coupons.recordUse(coupon.code());
        }

        String number = "HT" + jdbc.sql("select nextval('order_number_seq')").query(Long.class).single();
        String token = token();
        String status = method == PaymentMethod.COD ? "PLACED" : "PENDING_PAYMENT";
        long orderId = jdbc.sql("""
                        insert into orders (number, access_token, customer_id, name, phone, email, address, area_id,
                                            area_name, note, slot_date, slot_start, slot_end, substitution, subtotal,
                                            discount, delivery_fee, total, coupon_code, payment_method, status)
                        values (:number, :token, :customerId, :name, :phone, :email, :address, :areaId, :areaName,
                                :note, :slotDate, :slotStart, :slotEnd, :substitution, :subtotal, :discount, :fee,
                                :total, :coupon, :method, :status)
                        returning id""")
                .param("number", number).param("token", token).param("customerId", customerId)
                .param("name", customer.name()).param("phone", customer.phone()).param("email", customer.email())
                .param("address", customer.address()).param("areaId", area.id()).param("areaName", area.name())
                .param("note", customer.note()).param("slotDate", Date.valueOf(slotDate))
                .param("slotStart", Time.valueOf(slot.start())).param("slotEnd", Time.valueOf(slot.end()))
                .param("substitution", substitution.name()).param("subtotal", quote.subtotal())
                .param("discount", quote.discount()).param("fee", quote.deliveryFee()).param("total", quote.total())
                .param("coupon", quote.couponCode()).param("method", method.name()).param("status", status)
                .query(Long.class).single();
        for (Line line : lines) {
            jdbc.sql("""
                            insert into order_items (order_id, pack_id, product_id, product_slug, product_name, pack_label,
                                                     sku, image_url, unit_price, quantity)
                            values (:o, :k, :p, :slug, :name, :label, :sku, :image, :price, :qty)""")
                    .param("o", orderId).param("k", line.packId()).param("p", line.productId())
                    .param("slug", line.slug()).param("name", line.name()).param("label", line.packLabel())
                    .param("sku", line.sku()).param("image", line.image()).param("price", line.unitPrice())
                    .param("qty", line.quantity())
                    .update();
            jdbc.sql("update packs set stock = stock - :q where id = :k").param("q", line.quantity())
                    .param("k", line.packId()).update();
            jdbc.sql("update products set sold_count = sold_count + :q where id = :p").param("q", line.quantity())
                    .param("p", line.productId()).update();
        }
        jdbc.sql("insert into order_events (order_id, status, note, actor) values (:o, :s, :n, 'customer')")
                .param("o", orderId).param("s", status)
                .param("n", "Delivery " + slot.label() + ", " + slotDate)
                .update();
        jdbc.sql("delete from carts where id = :id").param("id", cartId).update();
        return new Placed(orderId, number, token, quote.total());
    }

    private String couponCode(UUID cartId) {
        return jdbc.sql("select coupon_code from carts where id = :id").param("id", cartId)
                .query(String.class).optional().orElse(null);
    }

    private static String token() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

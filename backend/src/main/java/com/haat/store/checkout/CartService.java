package com.haat.store.checkout;

import com.haat.store.shared.ApiException;
import com.haat.store.shared.StoreProperties;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shopping bags. A bag is identified by a random id kept in a cookie. */
@Service
public class CartService {

    public enum Problem {
        UNAVAILABLE, OUT_OF_STOCK, NOT_ENOUGH_STOCK
    }

    public record Line(long packId, long productId, String slug, String name, String packLabel, String sku,
                       String image, int unitPrice, Integer compareAtPrice, int quantity, int stock, int maxPerOrder,
                       int lineTotal, Problem problem) {
    }

    public record CouponView(String code, String description, int discount, boolean freeShipping) {
    }

    public record CartView(List<Line> items, int itemCount, int subtotal, int savings, int discount,
                           CouponView coupon, String couponProblem, int total, int deliveryFee, int freeDeliveryOver,
                           int remainingForFreeDelivery, int minimumOrder, int remainingForMinimum,
                           boolean canCheckout) {
    }

    private final JdbcClient jdbc;
    private final Coupons coupons;
    private final StoreProperties.Delivery delivery;
    private final Clock clock;

    CartService(JdbcClient jdbc, Coupons coupons, StoreProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.coupons = coupons;
        this.delivery = properties.delivery();
        this.clock = clock;
    }

    public CartView view(Optional<UUID> cartId) {
        if (cartId.isEmpty()) {
            return summarise(List.of(), null);
        }
        String code = jdbc.sql("select coupon_code from carts where id = :id").param("id", cartId.get())
                .query(String.class).optional().orElse(null);
        return summarise(lines(cartId.get()), code);
    }

    /**
     * Sets the quantity of a pack, creating the bag if needed (owned by {@code customerId} when
     * signed in). 0 removes it. Returns the bag id.
     */
    @Transactional
    public UUID set(Optional<UUID> cartId, Long customerId, long packId, int quantity) {
        record PackRow(int stock, int max) {
        }
        PackRow pack = jdbc.sql("""
                        select k.stock, k.max_per_order from packs k join products p on p.id = k.product_id
                         where k.id = :id and p.status = 'ACTIVE'""")
                .param("id", packId).query((rs, n) -> new PackRow(rs.getInt(1), rs.getInt(2))).optional()
                .orElseThrow(() -> ApiException.notFound("pack"));
        UUID id = cartId.filter(c -> belongsTo(c, customerId)).orElseGet(() -> create(customerId));
        if (quantity <= 0) {
            jdbc.sql("delete from cart_items where cart_id = :c and pack_id = :k").param("c", id).param("k", packId).update();
        } else {
            if (quantity > pack.max()) {
                throw ApiException.conflict("cart.limit", "You can buy up to " + pack.max() + " of this item per order.");
            }
            if (pack.stock() == 0) {
                throw ApiException.conflict("cart.out_of_stock", "Sorry, this item has just sold out.");
            }
            if (quantity > pack.stock()) {
                throw ApiException.conflict("cart.not_enough_stock", "Only " + pack.stock() + " left in stock.");
            }
            jdbc.sql("""
                            insert into cart_items (cart_id, pack_id, quantity) values (:c, :k, :q)
                            on conflict (cart_id, pack_id) do update set quantity = :q""")
                    .param("c", id).param("k", packId).param("q", quantity).update();
        }
        jdbc.sql("update carts set updated_at = now() where id = :id").param("id", id).update();
        return id;
    }

    /** Adds to whatever is already in the bag. */
    @Transactional
    public UUID add(Optional<UUID> cartId, Long customerId, long packId, int quantity) {
        return set(cartId, customerId, packId, quantityOf(cartId, packId) + quantity);
    }

    int quantityOf(Optional<UUID> cartId, long packId) {
        return cartId.map(c -> jdbc.sql("select quantity from cart_items where cart_id = :c and pack_id = :k")
                .param("c", c).param("k", packId).query(Integer.class).optional().orElse(0)).orElse(0);
    }

    /** The bag of a signed-in customer. */
    public Optional<UUID> ofCustomer(long customerId) {
        return jdbc.sql("select id from carts where customer_id = :c").param("c", customerId)
                .query(UUID.class).optional();
    }

    /** True if the bag exists and belongs to this customer, or to no one when {@code customerId} is null. */
    public boolean belongsTo(UUID cartId, Long customerId) {
        return jdbc.sql("select count(*) from carts where id = :id and customer_id is not distinct from cast(:c as bigint)")
                .param("id", cartId).param("c", customerId).query(Long.class).single() > 0;
    }

    /**
     * At sign-in, the bag filled as a guest joins the customer's own bag. If both hold the same
     * pack, the larger quantity wins (never above the per-order limit); a coupon carries over if the
     * customer's bag has none.
     */
    @Transactional
    public void claim(long customerId, Optional<UUID> guestCart) {
        Optional<UUID> guest = guestCart.filter(c -> belongsTo(c, null));
        if (guest.isEmpty()) {
            return;
        }
        Optional<UUID> own = ofCustomer(customerId);
        if (own.isEmpty()) {
            jdbc.sql("update carts set customer_id = :c, updated_at = now() where id = :id")
                    .param("c", customerId).param("id", guest.get()).update();
            return;
        }
        UUID target = own.get();
        jdbc.sql("""
                        insert into cart_items (cart_id, pack_id, quantity, added_at)
                        select :target, pack_id, quantity, added_at from cart_items where cart_id = :guest
                        on conflict (cart_id, pack_id) do update
                           set quantity = greatest(cart_items.quantity, excluded.quantity)""")
                .param("target", target).param("guest", guest.get()).update();
        jdbc.sql("""
                        update cart_items ci set quantity = k.max_per_order
                          from packs k where k.id = ci.pack_id and ci.cart_id = :target and ci.quantity > k.max_per_order""")
                .param("target", target).update();
        jdbc.sql("""
                        update carts set coupon_code = coalesce(coupon_code, (select coupon_code from carts where id = :guest)),
                                         updated_at = now()
                         where id = :target""")
                .param("target", target).param("guest", guest.get()).update();
        jdbc.sql("delete from carts where id = :guest").param("guest", guest.get()).update();
    }

    @Transactional
    public void applyCoupon(UUID cartId, String code) {
        int subtotal = lines(cartId).stream().filter(l -> l.problem() == null).mapToInt(Line::lineTotal).sum();
        Coupons.Applied applied = coupons.apply(code, subtotal, clock.instant());
        jdbc.sql("update carts set coupon_code = :code, updated_at = now() where id = :id")
                .param("code", applied.code()).param("id", cartId).update();
    }

    @Transactional
    public void removeCoupon(UUID cartId) {
        jdbc.sql("update carts set coupon_code = null, updated_at = now() where id = :id").param("id", cartId).update();
    }

    public List<Line> lines(UUID cartId) {
        return jdbc.sql("""
                        select k.id, p.id, p.slug, p.name, k.label, k.sku, p.image_url, k.price, k.compare_at_price,
                               ci.quantity, k.stock, k.max_per_order, p.status
                          from cart_items ci
                          join packs k on k.id = ci.pack_id
                          join products p on p.id = k.product_id
                         where ci.cart_id = :cart
                         order by ci.added_at, k.id""")
                .param("cart", cartId)
                .query((rs, n) -> {
                    int price = rs.getInt(8);
                    int quantity = rs.getInt(10);
                    int stock = rs.getInt(11);
                    Problem problem = !rs.getString(13).equals("ACTIVE") ? Problem.UNAVAILABLE
                            : stock == 0 ? Problem.OUT_OF_STOCK
                            : quantity > stock ? Problem.NOT_ENOUGH_STOCK : null;
                    return new Line(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getString(7), price, (Integer) rs.getObject(9), quantity, stock,
                            rs.getInt(12), price * quantity, problem);
                })
                .list();
    }

    private CartView summarise(List<Line> lines, String couponCode) {
        List<Line> buyable = lines.stream().filter(l -> l.problem() == null).toList();
        int subtotal = buyable.stream().mapToInt(Line::lineTotal).sum();
        int savings = buyable.stream()
                .mapToInt(l -> l.compareAtPrice() == null ? 0 : (l.compareAtPrice() - l.unitPrice()) * l.quantity())
                .sum();
        int count = lines.stream().mapToInt(Line::quantity).sum();
        CouponView coupon = null;
        String couponProblem = null;
        if (couponCode != null && subtotal > 0) {
            try {
                Coupons.Applied applied = coupons.apply(couponCode, subtotal, clock.instant());
                coupon = new CouponView(applied.code(), applied.description(), applied.discount(), applied.freeShipping());
            } catch (ApiException e) {
                couponProblem = couponCode + ": " + e.getMessage();
            }
        }
        int discount = coupon == null ? 0 : coupon.discount();
        int total = subtotal - discount;
        boolean free = (coupon != null && coupon.freeShipping()) || total >= delivery.freeOver();
        int fee = subtotal == 0 || free ? 0 : delivery.fee();
        int remainingFree = free ? 0 : delivery.freeOver() - total;
        int remainingMinimum = Math.max(0, delivery.minimumOrder() - subtotal);
        boolean canCheckout = !lines.isEmpty() && lines.stream().allMatch(l -> l.problem() == null)
                && remainingMinimum == 0;
        return new CartView(lines, count, subtotal, savings, discount, coupon, couponProblem, total, fee,
                delivery.freeOver(), remainingFree, delivery.minimumOrder(), remainingMinimum, canCheckout);
    }

    private UUID create(Long customerId) {
        if (customerId == null) {
            UUID id = UUID.randomUUID();
            jdbc.sql("insert into carts (id) values (:id)").param("id", id).update();
            return id;
        }
        // One bag per customer, even if two devices add their first item at the same moment.
        return jdbc.sql("""
                        insert into carts (id, customer_id) values (:id, :c)
                        on conflict (customer_id) where customer_id is not null do update set updated_at = now()
                        returning id""")
                .param("id", UUID.randomUUID()).param("c", customerId).query(UUID.class).single();
    }

    static ApiException emptyCart() {
        return new ApiException(HttpStatus.CONFLICT, "cart.empty", "Your bag is empty.");
    }
}

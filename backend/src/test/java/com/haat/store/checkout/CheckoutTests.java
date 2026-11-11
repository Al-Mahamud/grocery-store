package com.haat.store.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.haat.store.support.ApiClient;
import com.haat.store.support.ApiClient.Response;
import com.haat.store.support.IntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class CheckoutTests {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    ApiClient shopper;

    @BeforeEach
    void setUp() {
        shopper = new ApiClient(port);
    }

    long pack(String slug, String label) {
        return jdbc.sql("select k.id from packs k join products p on p.id = k.product_id where p.slug = :s and k.label = :l")
                .param("s", slug).param("l", label).query(Long.class).single();
    }

    void stock(long packId, int stock) {
        jdbc.sql("update packs set stock = :s where id = :id").param("s", stock).param("id", packId).update();
    }

    int stockOf(long packId) {
        return jdbc.sql("select stock from packs where id = :id").param("id", packId).query(Integer.class).single();
    }

    long area(String name) {
        return jdbc.sql("select id from delivery_areas where name = :n").param("n", name).query(Long.class).single();
    }

    /** Tomorrow's slot at the given position (0 = 8–10 am), always open at test time. */
    JsonNode tomorrowSlot(ApiClient client, int index) {
        return client.get("/api/v1/delivery/slots").body().get(1).get("slots").get(index);
    }

    Map<String, Object> order(String date, String start) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Tania Akter");
        body.put("phone", "01912345678");
        body.put("address", "House 21, Road 8A");
        body.put("areaId", area("Dhanmondi"));
        body.put("slotDate", date);
        body.put("slotStart", start);
        body.put("substitution", "REPLACE");
        body.put("paymentMethod", "COD");
        return body;
    }

    String tomorrow() {
        return shopper.get("/api/v1/delivery/slots").body().get(1).get("date").asString();
    }

    @Test
    void quickAddAndExactQuantities() {
        long rice = pack("miniket-rice", "5 kg");
        stock(rice, 50);
        JsonNode cart = shopper.post("/api/v1/cart/items", Map.of("packId", rice, "quantity", 1)).body();
        assertThat(cart.get("itemCount").asInt()).isEqualTo(1);
        cart = shopper.put("/api/v1/cart/items/" + rice, Map.of("quantity", 3)).body();
        assertThat(cart.get("items").get(0).get("quantity").asInt()).isEqualTo(3);
        assertThat(cart.get("subtotal").asInt()).isEqualTo(3 * 365);
        assertThat(cart.get("savings").asInt()).isEqualTo(3 * 25);
        assertThat(cart.get("deliveryFee").asInt()).isEqualTo(49);
        assertThat(cart.get("remainingForFreeDelivery").asInt()).isEqualTo(1500 - 3 * 365);
        cart = shopper.put("/api/v1/cart/items/" + rice, Map.of("quantity", 5)).body();
        assertThat(cart.get("deliveryFee").asInt()).isZero();
        cart = shopper.put("/api/v1/cart/items/" + rice, Map.of("quantity", 0)).body();
        assertThat(cart.get("items")).isEmpty();
    }

    @Test
    void perOrderLimitsAreEnforced() {
        long oil = pack("soybean-oil", "5 L");
        stock(oil, 50);
        assertThat(shopper.put("/api/v1/cart/items/" + oil, Map.of("quantity", 3)).status()).isEqualTo(200);
        Response tooMany = shopper.put("/api/v1/cart/items/" + oil, Map.of("quantity", 4));
        assertThat(tooMany.status()).isEqualTo(409);
        assertThat(tooMany.body().get("code").asString()).isEqualTo("cart.limit");
    }

    @Test
    void minimumOrderAndDeliveryFee() {
        long chilli = pack("green-chilli", "250 g");
        stock(chilli, 50);
        JsonNode cart = shopper.post("/api/v1/cart/items", Map.of("packId", chilli, "quantity", 2)).body();
        assertThat(cart.get("canCheckout").asBoolean()).isFalse();
        assertThat(cart.get("remainingForMinimum").asInt()).isEqualTo(300 - 60);
        assertThat(cart.get("deliveryFee").asInt()).isEqualTo(49);
        assertThat(shopper.post("/api/v1/checkout/quote", Map.of()).body().get("problem").asString()).contains("৳300");
        JsonNode slot = tomorrowSlot(shopper, 2);
        Response refused = shopper.post("/api/v1/checkout", order(tomorrow(), slot.get("start").asString()));
        assertThat(refused.status()).isEqualTo(409);
        assertThat(refused.body().get("code").asString()).isEqualTo("checkout.minimum_order");
    }

    @Test
    void placingAnOrderBooksTheSlot() {
        long eggs = pack("farm-eggs-brown", "12 pcs");
        long milk = pack("pasteurised-milk", "1 L");
        stock(eggs, 40);
        stock(milk, 40);
        shopper.post("/api/v1/cart/items", Map.of("packId", eggs, "quantity", 2));
        shopper.post("/api/v1/cart/items", Map.of("packId", milk, "quantity", 1));
        JsonNode before = tomorrowSlot(shopper, 6);
        Response placed = shopper.post("/api/v1/checkout", order(tomorrow(), before.get("start").asString()));
        assertThat(placed.status()).isEqualTo(201);
        assertThat(placed.body().get("number").asString()).startsWith("HT");
        assertThat(placed.body().get("total").asInt()).isEqualTo(2 * 150 + 95 + 49);
        assertThat(stockOf(eggs)).isEqualTo(38);
        assertThat(tomorrowSlot(shopper, 6).get("remaining").asInt()).isEqualTo(before.get("remaining").asInt() - 1);

        JsonNode order = shopper.get("/api/v1/orders/" + placed.body().get("number").asString() + "?token="
                + placed.body().get("token").asString()).body();
        assertThat(order.get("area").asString()).isEqualTo("Dhanmondi");
        assertThat(order.get("slot").get("start").asString()).startsWith("20:00");
        assertThat(order.get("substitution").asString()).isEqualTo("REPLACE");
        assertThat(order.get("items").get(0).get("packLabel").asString()).isEqualTo("12 pcs");
        assertThat(new ApiClient(port).get("/api/v1/orders/track?number=" + order.get("number").asString()
                + "&phone=01912345678").status()).isEqualTo(200);
    }

    @Test
    void fullSlotsCannotBeBooked() {
        long rice = pack("miniket-rice", "25 kg");
        stock(rice, 40);
        shopper.post("/api/v1/cart/items", Map.of("packId", rice, "quantity", 1));
        JsonNode slot = tomorrowSlot(shopper, 3);
        String start = slot.get("start").asString();
        // Fill every remaining place in the slot with copies of an order.
        long template = jdbc.sql("select id from orders order by id limit 1").query(Long.class).optional()
                .orElseGet(() -> {
                    ApiClient other = new ApiClient(port);
                    other.post("/api/v1/cart/items", Map.of("packId", rice, "quantity", 1));
                    other.post("/api/v1/checkout", order(tomorrow(), tomorrowSlot(other, 0).get("start").asString()));
                    return jdbc.sql("select id from orders order by id limit 1").query(Long.class).single();
                });
        int remaining = tomorrowSlot(shopper, 3).get("remaining").asInt();
        for (int i = 0; i < remaining; i++) {
            jdbc.sql("""
                    insert into orders (number, access_token, name, phone, address, area_id, area_name, slot_date,
                                        slot_start, slot_end, substitution, subtotal, delivery_fee, total,
                                        payment_method, status)
                    select 'FILL' || :i || '-' || :start, access_token, name, phone, address, area_id, area_name,
                           cast(:date as date), cast(:start as time), cast(:start as time) + interval '2 hours',
                           substitution, subtotal, delivery_fee, total, payment_method, status
                      from orders where id = :id""")
                    .param("i", i).param("start", start).param("date", tomorrow()).param("id", template).update();
        }
        assertThat(tomorrowSlot(shopper, 3).get("available").asBoolean()).isFalse();
        Response refused = shopper.post("/api/v1/checkout", order(tomorrow(), start));
        assertThat(refused.status()).isEqualTo(409);
        assertThat(refused.body().get("code").asString()).isEqualTo("delivery.slot");
    }

    @Test
    void pastSlotsAndUnknownAreasAreRefused() {
        long rice = pack("miniket-rice", "5 kg");
        stock(rice, 40);
        shopper.post("/api/v1/cart/items", Map.of("packId", rice, "quantity", 1));
        Response past = shopper.post("/api/v1/checkout", order("2020-01-01", "08:00"));
        assertThat(past.body().get("code").asString()).isEqualTo("delivery.slot");
        Map<String, Object> body = order(tomorrow(), "18:00");
        body.put("areaId", 999999);
        assertThat(shopper.post("/api/v1/checkout", body).body().get("code").asString()).isEqualTo("delivery.area");
    }

    @Test
    void couponsAndCsrf() {
        long ghee = pack("pure-ghee", "400 g");
        stock(ghee, 40);
        shopper.post("/api/v1/cart/items", Map.of("packId", ghee, "quantity", 1));
        JsonNode cart = shopper.post("/api/v1/cart/coupon", Map.of("code", "welcome15")).body();
        assertThat(cart.get("discount").asInt()).isEqualTo(108);
        assertThat(shopper.post("/api/v1/cart/coupon", Map.of("code", "OLD10")).body().get("code").asString())
                .isEqualTo("coupon.expired");
        assertThat(new ApiClient(port).postWithoutCsrf("/api/v1/cart/items", Map.of("packId", ghee, "quantity", 1)).status())
                .isEqualTo(403);
    }
}

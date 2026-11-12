package com.haat.store.notify;

import static org.assertj.core.api.Assertions.assertThat;

import com.haat.store.support.ApiClient;
import com.haat.store.support.ApiClient.Response;
import com.haat.store.support.DemoData;
import com.haat.store.support.IntegrationTest;
import com.haat.store.support.TextsSent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class TextsAndCodesTests {

    private static final AtomicInteger NUMBERS = new AtomicInteger(4_000_000);

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    SmsOutbox outbox;

    static String newPhone() {
        return "0171" + NUMBERS.incrementAndGet();
    }

    JsonNode placeGuestOrder(ApiClient shopper, String phone, int slotIndex) {
        long rice = jdbc.sql("select k.id from packs k join products p on p.id = k.product_id where p.slug = 'miniket-rice' and k.label = '5 kg'")
                .query(Long.class).single();
        jdbc.sql("update packs set stock = 80 where id = :id").param("id", rice).update();
        shopper.post("/api/v1/cart/items", Map.of("packId", rice, "quantity", 1));
        JsonNode day = shopper.get("/api/v1/delivery/slots").body().get(1);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Shirin Akter");
        body.put("phone", phone);
        body.put("address", "House 1, Road 1");
        body.put("areaId", jdbc.sql("select id from delivery_areas where name = 'Khilgaon'").query(Long.class).single());
        body.put("slotDate", day.get("date").asString());
        body.put("slotStart", day.get("slots").get(slotIndex).get("start").asString());
        body.put("substitution", "CALL");
        body.put("paymentMethod", "COD");
        Response r = shopper.post("/api/v1/checkout", body);
        assertThat(r.status()).isEqualTo(201);
        return r.body();
    }

    @Test
    void signingUpNeedsTheTextedCodeAndBringsGuestOrdersAlong() {
        String phone = newPhone();
        ApiClient shopper = new ApiClient(port);
        String number = placeGuestOrder(shopper, phone, 6).get("number").asString();
        assertThat(TextsSent.bodies(jdbc, phone, "ORDER_PLACED").get(0))
                .contains(number, "cash on delivery", "Tk").hasSizeLessThanOrEqualTo(160);

        JsonNode sent = shopper.post("/api/v1/auth/codes", Map.of("phone", phone, "purpose", "REGISTER")).body();
        assertThat(sent.get("sentTo").asString()).isEqualTo(phone.substring(0, 3) + "•••••" + phone.substring(8));
        Response tooSoon = shopper.post("/api/v1/auth/codes", Map.of("phone", phone, "purpose", "REGISTER"));
        assertThat(tooSoon.status()).isEqualTo(429);
        assertThat(tooSoon.body().get("code").asString()).isEqualTo("code.too_soon");

        String code = TextsSent.lastCode(jdbc, phone);
        Map<String, Object> form = new HashMap<>(Map.of("name", "Shirin Akter", "phone", phone, "password", "pass-word-1",
                "code", code.equals("000000") ? "111111" : "000000"));
        Response wrong = shopper.post("/api/v1/auth/register", form);
        assertThat(wrong.status()).isEqualTo(400);
        assertThat(wrong.body().get("fields").has("code")).isTrue();

        form.put("code", code);
        JsonNode me = shopper.post("/api/v1/auth/register", form).body();
        assertThat(me.get("linkedOrders").asInt()).isEqualTo(1);
        assertThat(shopper.get("/api/v1/account/orders").body().get("items").get(0).get("number").asString()).isEqualTo(number);
        // A code works once.
        Response reuse = new ApiClient(port).post("/api/v1/auth/register", form);
        assertThat(reuse.status()).isEqualTo(409);
    }

    @Test
    void wrongCodesRunOut() {
        String phone = newPhone();
        ApiClient shopper = new ApiClient(port);
        shopper.post("/api/v1/auth/codes", Map.of("phone", phone, "purpose", "REGISTER"));
        String code = TextsSent.lastCode(jdbc, phone);
        String bad = code.equals("123456") ? "654321" : "123456";
        Map<String, Object> form = new HashMap<>(Map.of("name", "X Y", "phone", phone, "password", "pass-word-1", "code", bad));
        for (int i = 0; i < 5; i++) {
            assertThat(shopper.post("/api/v1/auth/register", form).status()).isEqualTo(400);
        }
        form.put("code", code);
        assertThat(shopper.post("/api/v1/auth/register", form).status()).isEqualTo(400);
    }

    @Test
    void forgottenPasswordsAreResetWithACode() {
        // No account: the same answer, but nothing is texted.
        String stranger = newPhone();
        ApiClient someone = new ApiClient(port);
        Response unknown = someone.post("/api/v1/auth/codes", Map.of("phone", stranger, "purpose", "RESET"));
        assertThat(unknown.status()).isEqualTo(200);
        assertThat(TextsSent.bodies(jdbc, stranger, "CODE")).isEmpty();

        ApiClient laptop = new ApiClient(port);
        laptop.post("/api/v1/auth/login", Map.of("phone", DemoData.DEMO_PHONE, "password", DemoData.DEMO_PASSWORD));
        ApiClient phone = new ApiClient(port);
        phone.post("/api/v1/auth/codes", Map.of("phone", DemoData.DEMO_PHONE, "purpose", "RESET"));
        String code = TextsSent.lastCode(jdbc, DemoData.DEMO_PHONE);
        JsonNode me = phone.post("/api/v1/auth/password-reset",
                Map.of("phone", DemoData.DEMO_PHONE, "code", code, "newPassword", "brand-new-pass")).body();
        assertThat(me.get("customer").get("phone").asString()).isEqualTo(DemoData.DEMO_PHONE);
        assertThat(laptop.get("/api/v1/account/profile").status()).isEqualTo(401);
        assertThat(new ApiClient(port).post("/api/v1/auth/login",
                Map.of("phone", DemoData.DEMO_PHONE, "password", DemoData.DEMO_PASSWORD)).status()).isEqualTo(401);
        // Put the demo password back for other tests.
        phone.post("/api/v1/account/password", Map.of("currentPassword", "brand-new-pass", "newPassword", DemoData.DEMO_PASSWORD));
    }

    @Test
    void customersAreToldWhenTheOrderIsOnItsWayOrCancelled() {
        String phone = newPhone();
        ApiClient shopper = new ApiClient(port);
        String first = placeGuestOrder(shopper, phone, 6).get("number").asString();
        String second = placeGuestOrder(new ApiClient(port), phone, 6).get("number").asString();

        ApiClient admin = new ApiClient(port);
        admin.post("/api/v1/staff/login", Map.of("phone", DemoData.STAFF_PHONE, "password", DemoData.STAFF_PASSWORD));
        admin.post("/api/v1/admin/orders/" + first + "/status", Map.of("status", "PACKING"));
        admin.post("/api/v1/admin/orders/" + first + "/status", Map.of("status", "OUT_FOR_DELIVERY"));
        admin.post("/api/v1/admin/orders/" + second + "/status", Map.of("status", "CANCELLED"));

        List<String> onTheWay = TextsSent.bodies(jdbc, phone, "ORDER_ON_THE_WAY");
        assertThat(onTheWay).singleElement().asString().contains(first, "keep Tk", "arrives by 10pm");
        assertThat(TextsSent.bodies(jdbc, phone, "ORDER_CANCELLED")).singleElement().asString().contains(second, "cancelled");
        assertThat(TextsSent.bodies(jdbc, phone, "ORDER_PLACED")).hasSize(2);

        // Sending: everything waiting goes out, and sent codes can't be read back.
        shopper.post("/api/v1/auth/codes", Map.of("phone", phone, "purpose", "REGISTER"));
        outbox.sendQueued();
        while (outbox.sendQueued() > 0) {
            // Drain the queue, which other tests also fill.
        }
        assertThat(jdbc.sql("select count(*) from sms_messages where phone = :p and status <> 'SENT'").param("p", phone)
                .query(Integer.class).single()).isZero();
        assertThat(TextsSent.bodies(jdbc, phone, "CODE").get(0)).contains("••••••").doesNotContainPattern("\\d{6}");

        JsonNode texts = admin.get("/api/v1/admin/texts?phone=" + phone).body();
        assertThat(texts.get("items")).hasSize(5);
        assertThat(texts.get("items").toString()).contains(first, second);
        long one = texts.get("items").get(0).get("id").asLong();
        assertThat(admin.post("/api/v1/admin/texts/" + one + "/retry", null).status()).isEqualTo(409);
        jdbc.sql("update sms_messages set status = 'FAILED', error = 'Operator down' where id = :id").param("id", one).update();
        admin.post("/api/v1/admin/texts/" + one + "/retry", null);
        assertThat(jdbc.sql("select status from sms_messages where id = :id").param("id", one).query(String.class).single())
                .isEqualTo("QUEUED");
    }

    @Test
    void theSitemapListsWhatIsOnSale() {
        JsonNode sitemap = new ApiClient(port).get("/api/v1/sitemap").body();
        assertThat(sitemap.get("categories").toString()).contains("vegetables");
        assertThat(sitemap.get("products").toString()).contains("miniket-rice");
    }
}

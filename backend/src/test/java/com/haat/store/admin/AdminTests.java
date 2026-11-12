package com.haat.store.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.haat.store.support.ApiClient;
import com.haat.store.support.ApiClient.Response;
import com.haat.store.support.DemoData;
import com.haat.store.support.FakeSslCommerz;
import com.haat.store.support.IntegrationTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class AdminTests {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    FakeSslCommerz gateway;

    ApiClient admin;

    @BeforeEach
    void signIn() {
        admin = new ApiClient(port);
        assertThat(admin.post("/api/v1/staff/login",
                Map.of("phone", DemoData.STAFF_PHONE, "password", DemoData.STAFF_PASSWORD)).status()).isEqualTo(200);
    }

    long pack(String slug, String label) {
        long id = jdbc.sql("select k.id from packs k join products p on p.id = k.product_id where p.slug = :s and k.label = :l")
                .param("s", slug).param("l", label).query(Long.class).single();
        jdbc.sql("update packs set stock = 50 where id = :id").param("id", id).update();
        return id;
    }

    int stock(long pack) {
        return jdbc.sql("select stock from packs where id = :id").param("id", pack).query(Integer.class).single();
    }

    /** A shopper places an order for tomorrow 8–10 pm. */
    JsonNode place(ApiClient shopper, String method) {
        JsonNode day = shopper.get("/api/v1/delivery/slots").body().get(1);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Mitu Rahman");
        body.put("phone", "01555123456");
        body.put("address", "Flat 2C, House 8, Road 4");
        body.put("areaId", jdbc.sql("select id from delivery_areas where name = 'Uttara'").query(Long.class).single());
        body.put("slotDate", day.get("date").asString());
        body.put("slotStart", day.get("slots").get(6).get("start").asString());
        body.put("substitution", "REMOVE");
        body.put("paymentMethod", method);
        Response r = shopper.post("/api/v1/checkout", body);
        assertThat(r.status()).isEqualTo(201);
        return r.body();
    }

    @Test
    void onlyActiveStaffGetIn() {
        ApiClient anonymous = new ApiClient(port);
        assertThat(anonymous.get("/api/v1/admin/dashboard").status()).isEqualTo(401);
        ApiClient customer = new ApiClient(port);
        customer.post("/api/v1/auth/login", Map.of("phone", DemoData.DEMO_PHONE, "password", DemoData.DEMO_PASSWORD));
        assertThat(customer.get("/api/v1/admin/dashboard").status()).isEqualTo(403);
        Response wrong = anonymous.post("/api/v1/staff/login", Map.of("phone", DemoData.STAFF_PHONE, "password", "nope-nope"));
        assertThat(wrong.status()).isEqualTo(401);
        // A customer's password doesn't open the admin area.
        Response asCustomer = anonymous.post("/api/v1/staff/login",
                Map.of("phone", DemoData.DEMO_PHONE, "password", DemoData.DEMO_PASSWORD));
        assertThat(asCustomer.status()).isEqualTo(401);

        assertThat(admin.get("/api/v1/staff/me").body().get("staff").get("name").asString()).startsWith("Rina");
        JsonNode dashboard = admin.get("/api/v1/admin/dashboard").body();
        assertThat(dashboard.get("week")).hasSize(7);
        assertThat(dashboard.get("open").get("PLACED").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(dashboard.get("slots")).hasSize(2);
        // Staff aren't customers: no account pages.
        assertThat(admin.get("/api/v1/account/profile").status()).isEqualTo(401);

        long id = jdbc.sql("insert into staff (name, phone, password_hash) values ('Temp', '01300000009', 'x') returning id")
                .query(Long.class).single();
        jdbc.sql("update staff set active = false where id = :id").param("id", id).update();
        jdbc.sql("update staff set active = false where phone = :p").param("p", DemoData.STAFF_PHONE).update();
        try {
            assertThat(admin.get("/api/v1/admin/dashboard").status()).isEqualTo(401);
        } finally {
            jdbc.sql("update staff set active = true where phone = :p").param("p", DemoData.STAFF_PHONE).update();
        }
    }

    @Test
    void anOrderMovesFromPlacedToDelivered() {
        long eggs = pack("farm-eggs-brown", "12 pcs");
        ApiClient shopper = new ApiClient(port);
        shopper.post("/api/v1/cart/items", Map.of("packId", eggs, "quantity", 3));
        String number = place(shopper, "COD").get("number").asString();

        JsonNode open = admin.get("/api/v1/admin/orders?filter=OPEN&q=" + number).body();
        assertThat(open.get("items").get(0).get("number").asString()).isEqualTo(number);
        JsonNode detail = admin.get("/api/v1/admin/orders/" + number).body();
        assertThat(detail.get("next").toString()).contains("PACKING", "CANCELLED");

        Response skip = admin.post("/api/v1/admin/orders/" + number + "/status", Map.of("status", "DELIVERED"));
        assertThat(skip.status()).isEqualTo(409);
        admin.post("/api/v1/admin/orders/" + number + "/status", Map.of("status", "PACKING"));
        admin.post("/api/v1/admin/orders/" + number + "/notes", Map.of("note", "Customer asked for brown eggs only"));
        admin.post("/api/v1/admin/orders/" + number + "/status", Map.of("status", "OUT_FOR_DELIVERY"));
        JsonNode done = admin.post("/api/v1/admin/orders/" + number + "/status", Map.of("status", "DELIVERED")).body();
        assertThat(done.get("order").get("status").asString()).isEqualTo("DELIVERED");
        assertThat(done.get("order").get("paymentStatus").asString()).isEqualTo("PAID");
        assertThat(done.get("order").get("events").toString()).contains("Cash collected", "brown eggs");
        assertThat(jdbc.sql("select count(*) from order_events e join orders o on o.id = e.order_id where o.number = :n and e.actor like 'staff:Rina%'")
                .param("n", number).query(Integer.class).single()).isEqualTo(4);
        assertThat(done.get("next")).isEmpty();
    }

    @Test
    void cancellingAPaidOrderGivesStockBackAndShowsARefundDue() throws Exception {
        long ghee = pack("pure-ghee", "400 g");
        ApiClient shopper = new ApiClient(port);
        shopper.post("/api/v1/cart/items", Map.of("packId", ghee, "quantity", 1));
        JsonNode placed = place(shopper, "ONLINE");
        String number = placed.get("number").asString();
        String tran = number + "-1";
        String val = gateway.pay(tran, gateway.session(tran).get("total_amount"), "BDT", "bKash");
        java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                        "http://localhost:" + port + "/api/v1/payments/sslcommerz/ipn"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("tran_id=" + tran + "&status=VALID&val_id=" + val)).build(),
                java.net.http.HttpResponse.BodyHandlers.discarding());
        assertThat(stock(ghee)).isEqualTo(49);

        JsonNode cancelled = admin.post("/api/v1/admin/orders/" + number + "/status",
                Map.of("status", "CANCELLED", "note", "Out of ghee at the hub")).body();
        assertThat(cancelled.get("refundDue").asBoolean()).isTrue();
        assertThat(cancelled.get("attempts").get(0).get("cardType").asString()).isEqualTo("bKash");
        assertThat(stock(ghee)).isEqualTo(50);
        assertThat(admin.get("/api/v1/admin/orders?filter=REFUND_DUE").body().get("items").toString()).contains(number);

        Response missing = admin.post("/api/v1/admin/orders/" + number + "/refund", Map.of("reference", " "));
        assertThat(missing.status()).isEqualTo(400);
        JsonNode refunded = admin.post("/api/v1/admin/orders/" + number + "/refund", Map.of("reference", "RF-88231")).body();
        assertThat(refunded.get("order").get("paymentStatus").asString()).isEqualTo("REFUNDED");
        assertThat(refunded.get("refundReference").asString()).isEqualTo("RF-88231");
        assertThat(admin.post("/api/v1/admin/orders/" + number + "/refund", Map.of("reference", "again")).status()).isEqualTo(409);
    }

    @Test
    void thePickingListAddsUpASlot() {
        JsonNode day = admin.get("/api/v1/delivery/slots").body().get(1);
        String date = day.get("date").asString();
        JsonNode picking = admin.get("/api/v1/admin/picking?date=" + date + "&start=10:00").body();
        assertThat(picking.get("orders").size()).isGreaterThanOrEqualTo(4);
        JsonNode rice = StreamSupport.stream(picking.get("lines").spliterator(), false)
                .filter(l -> l.get("name").asString().equals("Miniket Rice") && l.get("packLabel").asString().equals("5 kg"))
                .findFirst().orElseThrow();
        assertThat(rice.get("quantity").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(rice.get("category").asString()).isNotBlank();

        int moved = admin.post("/api/v1/admin/picking/start?date=" + date + "&start=10:00", null).body().get("moved").asInt();
        assertThat(moved).isGreaterThanOrEqualTo(3);
        JsonNode after = admin.get("/api/v1/admin/picking?date=" + date + "&start=10:00").body();
        assertThat(StreamSupport.stream(after.get("orders").spliterator(), false)
                .allMatch(o -> o.get("status").asString().equals("PACKING"))).isTrue();
    }

    @Test
    void productsAndPacksAreEdited() {
        long vegetables = jdbc.sql("select id from categories where slug = 'vegetables'").query(Long.class).single();
        Map<String, Object> product = new HashMap<>();
        product.put("name", "Red Amaranth");
        product.put("localName", "লাল শাক");
        product.put("categoryId", vegetables);
        product.put("brand", "Haat Farms");
        product.put("description", "Tender leaves, picked this morning.");
        product.put("fresh", true);
        product.put("status", "ACTIVE");
        List<Map<String, Object>> packs = new ArrayList<>();
        packs.add(new HashMap<>(Map.of("label", "1 bunch", "amount", 1, "unit", "PCS", "sku", "VEG-AMA-1", "price", 20, "stock", 40)));
        packs.add(new HashMap<>(Map.of("label", "3 bunches", "amount", 3, "unit", "PCS", "sku", "VEG-AMA-3", "price", 55,
                "compareAtPrice", 50, "stock", 10)));
        product.put("packs", packs);
        Response invalid = admin.post("/api/v1/admin/products", product);
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.body().get("fields").has("packs.1.compareAtPrice")).isTrue();

        packs.get(1).put("compareAtPrice", 60);
        JsonNode created = admin.post("/api/v1/admin/products", product).body();
        long id = created.get("id").asLong();
        assertThat(created.get("slug").asString()).isEqualTo("red-amaranth");
        assertThat(new ApiClient(port).get("/api/v1/products/red-amaranth").body().get("packs")).hasSize(2);

        // Rename one pack, drop the other, add a new one.
        Map<String, Object> keep = new HashMap<>(packs.get(0));
        keep.put("id", created.get("packs").get(0).get("id").asLong());
        keep.put("label", "1 bunch (250 g)");
        keep.put("price", 22);
        List<Map<String, Object>> next = new ArrayList<>(List.of(keep,
                new HashMap<>(Map.of("label", "Family pack", "amount", 1, "unit", "KG", "sku", "VEG-AMA-FAM", "price", 80, "stock", 5))));
        product.put("packs", next);
        JsonNode updated = admin.put("/api/v1/admin/products/" + id, product).body();
        assertThat(updated.get("packs")).hasSize(2);
        assertThat(updated.get("packs").get(0).get("label").asString()).isEqualTo("1 bunch (250 g)");
        assertThat(updated.get("packs").get(1).get("sku").asString()).isEqualTo("VEG-AMA-FAM");

        next.get(1).put("sku", jdbc.sql("select sku from packs k join products p on p.id = k.product_id where p.slug = 'miniket-rice' limit 1")
                .query(String.class).single());
        assertThat(admin.put("/api/v1/admin/products/" + id, product).status()).isEqualTo(409);

        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
        JsonNode withImage = admin.upload("/api/v1/admin/products/" + id + "/image", "file", "amaranth.png", "image/png", png).body();
        String url = withImage.get("image").asString();
        assertThat(url).startsWith("/media/products/").endsWith(".png");
        assertThat(admin.status(url)).isEqualTo(200);
        Response svg = admin.upload("/api/v1/admin/products/" + id + "/image", "file", "x.svg", "image/svg+xml",
                "<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>".getBytes());
        assertThat(svg.status()).isEqualTo(400);

        next.get(1).put("sku", "VEG-AMA-FAM");
        product.put("status", "ARCHIVED");
        assertThat(admin.put("/api/v1/admin/products/" + id, product).status()).isEqualTo(200);
        assertThat(new ApiClient(port).get("/api/v1/products/red-amaranth").status()).isEqualTo(404);
        assertThat(new ApiClient(port).post("/api/v1/admin/products", product).status()).isEqualTo(401);
    }

    @Test
    void stockCanBeCountedAndSet() {
        long milk = pack("pasteurised-milk", "1 L");
        jdbc.sql("update packs set stock = 2 where id = :id").param("id", milk).update();
        JsonNode low = admin.get("/api/v1/admin/stock?low=true").body();
        assertThat(low.toString()).contains("\"packId\":" + milk);
        JsonNode set = admin.patch("/api/v1/admin/packs/" + milk + "/stock", Map.of("stock", 120)).body();
        assertThat(set.get("stock").asInt()).isEqualTo(120);
        assertThat(admin.patch("/api/v1/admin/packs/" + milk + "/stock", Map.of("stock", -1)).status()).isEqualTo(400);
    }

    @Test
    void couponsAreaAndSlots() {
        Map<String, Object> coupon = new HashMap<>(Map.of("code", "eid200", "description", "৳200 off for Eid", "kind", "FIXED",
                "value", 200, "minSubtotal", 1000));
        JsonNode created = admin.post("/api/v1/admin/coupons", coupon).body();
        assertThat(created.get("code").asString()).isEqualTo("EID200");
        assertThat(admin.post("/api/v1/admin/coupons", coupon).status()).isEqualTo(409);

        ApiClient shopper = new ApiClient(port);
        shopper.post("/api/v1/cart/items", Map.of("packId", pack("miniket-rice", "5 kg"), "quantity", 3));
        assertThat(shopper.post("/api/v1/cart/coupon", Map.of("code", "EID200")).body().get("discount").asInt()).isEqualTo(200);
        coupon.put("active", false);
        admin.put("/api/v1/admin/coupons/" + created.get("id").asLong(), coupon);
        assertThat(shopper.get("/api/v1/cart").body().get("couponProblem").asString()).contains("isn't active");

        JsonNode area = admin.post("/api/v1/admin/areas", Map.of("name", "Wari")).body();
        assertThat(new ApiClient(port).get("/api/v1/delivery/areas").body().toString()).contains("Wari");
        admin.put("/api/v1/admin/areas/" + area.get("id").asLong(), Map.of("name", "Wari", "active", false));
        assertThat(new ApiClient(port).get("/api/v1/delivery/areas").body().toString()).doesNotContain("Wari");
        assertThat(admin.post("/api/v1/admin/areas", Map.of("name", "Gulshan")).status()).isEqualTo(409);

        String date = shopper.get("/api/v1/delivery/slots").body().get(2).get("date").asString();
        JsonNode board = admin.put("/api/v1/admin/slots", Map.of("date", date, "start", "08:00", "capacity", 0, "note", "Eid morning")).body();
        assertThat(board.get("days").get(2).get("slots").get(0).get("custom").asBoolean()).isTrue();
        JsonNode closed = shopper.get("/api/v1/delivery/slots").body().get(2).get("slots").get(0);
        assertThat(closed.get("available").asBoolean()).isFalse();
        admin.put("/api/v1/admin/slots", Map.of("date", date, "start", "08:00", "capacity", 40));
        assertThat(shopper.get("/api/v1/delivery/slots").body().get(2).get("slots").get(0).get("remaining").asInt()).isEqualTo(40);
        Map<String, Object> reset = new HashMap<>(Map.of("date", date, "start", "08:00"));
        reset.put("capacity", null);
        admin.put("/api/v1/admin/slots", reset);
        assertThat(shopper.get("/api/v1/delivery/slots").body().get(2).get("slots").get(0).get("remaining").asInt()).isEqualTo(25);
        assertThat(admin.put("/api/v1/admin/slots", Map.of("date", date, "start", "09:00", "capacity", 5)).status()).isEqualTo(409);
    }
}

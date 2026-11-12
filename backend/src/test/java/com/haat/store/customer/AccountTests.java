package com.haat.store.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.haat.store.support.ApiClient;
import com.haat.store.support.ApiClient.Response;
import com.haat.store.support.DemoData;
import com.haat.store.shared.SignInLimiter;
import com.haat.store.support.IntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class AccountTests {

    private static final AtomicInteger NUMBERS = new AtomicInteger(3_000_000);

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    static String newPhone() {
        return "0181" + NUMBERS.incrementAndGet();
    }

    long pack(String slug, String label) {
        long id = jdbc.sql("select k.id from packs k join products p on p.id = k.product_id where p.slug = :s and k.label = :l")
                .param("s", slug).param("l", label).query(Long.class).single();
        jdbc.sql("update packs set stock = greatest(stock, 60) where id = :id").param("id", id).update();
        return id;
    }

    long area(String name) {
        return jdbc.sql("select id from delivery_areas where name = :n").param("n", name).query(Long.class).single();
    }

    ApiClient register(String phone) {
        ApiClient client = new ApiClient(port);
        Response r = client.post("/api/v1/auth/register",
                Map.of("name", "Nusrat Jahan", "phone", phone, "password", "shobji-123"));
        assertThat(r.status()).isEqualTo(201);
        return client;
    }

    ApiClient signIn(String phone, String password) {
        ApiClient client = new ApiClient(port);
        assertThat(client.post("/api/v1/auth/login", Map.of("phone", phone, "password", password)).status()).isEqualTo(200);
        return client;
    }

    static int quantity(JsonNode cart, long packId) {
        return StreamSupport.stream(cart.get("items").spliterator(), false)
                .filter(i -> i.get("packId").asLong() == packId).mapToInt(i -> i.get("quantity").asInt()).sum();
    }

    Map<String, Object> order(ApiClient client, boolean saveAddress) {
        JsonNode day = client.get("/api/v1/delivery/slots").body().get(1);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Nusrat Jahan");
        body.put("phone", "01711000111");
        body.put("address", "Flat 4B, House 9, Road 2");
        body.put("areaId", area("Mirpur"));
        body.put("slotDate", day.get("date").asString());
        body.put("slotStart", day.get("slots").get(4).get("start").asString());
        body.put("substitution", "CALL");
        body.put("paymentMethod", "COD");
        body.put("saveAddress", saveAddress);
        return body;
    }

    @Test
    void registerSignInAndOut() {
        String phone = newPhone();
        ApiClient client = register("+880 " + phone.substring(1));
        JsonNode me = client.get("/api/v1/auth/me").body().get("customer");
        assertThat(me.get("phone").asString()).isEqualTo(phone);
        assertThat(me.get("name").asString()).isEqualTo("Nusrat Jahan");

        Response again = new ApiClient(port).post("/api/v1/auth/register",
                Map.of("name", "Someone", "phone", phone, "password", "another-pass"));
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.body().get("code").asString()).isEqualTo("auth.phone_taken");

        Response weak = new ApiClient(port).post("/api/v1/auth/register",
                Map.of("name", "Someone", "phone", newPhone(), "password", "short"));
        assertThat(weak.status()).isEqualTo(400);
        assertThat(weak.body().get("fields").has("password")).isTrue();

        assertThat(client.post("/api/v1/auth/logout", null).status()).isEqualTo(204);
        assertThat(client.get("/api/v1/auth/me").body().get("customer").isNull()).isTrue();
        assertThat(client.get("/api/v1/account/profile").status()).isEqualTo(401);

        ApiClient back = signIn(phone, "shobji-123");
        assertThat(back.get("/api/v1/account/profile").body().get("phone").asString()).isEqualTo(phone);
        Response updated = back.put("/api/v1/account/profile", Map.of("name", "Nusrat J.", "email", "nusrat@example.com"));
        assertThat(updated.body().get("email").asString()).isEqualTo("nusrat@example.com");
    }

    @Test
    void wrongPasswordsAreCountedAndPaused() {
        String phone = newPhone();
        register(phone);
        ApiClient guesser = new ApiClient(port);
        for (int i = 0; i < SignInLimiter.PER_PHONE; i++) {
            Response r = guesser.post("/api/v1/auth/login", Map.of("phone", phone, "password", "guess-" + i));
            assertThat(r.status()).isEqualTo(401);
            assertThat(r.body().get("code").asString()).isEqualTo("auth.bad_credentials");
        }
        Response paused = guesser.post("/api/v1/auth/login", Map.of("phone", phone, "password", "shobji-123"));
        assertThat(paused.status()).isEqualTo(429);
        // Unknown numbers get the same answer as wrong passwords.
        Response unknown = new ApiClient(port).post("/api/v1/auth/login", Map.of("phone", newPhone(), "password", "whatever1"));
        assertThat(unknown.status()).isEqualTo(401);
    }

    @Test
    void theGuestBagJoinsTheAccountAtSignIn() {
        long rice = pack("miniket-rice", "5 kg");
        long eggs = pack("farm-eggs-brown", "12 pcs");
        String phone = newPhone();
        ApiClient laptop = register(phone);
        laptop.post("/api/v1/cart/items", Map.of("packId", rice, "quantity", 2));
        laptop.post("/api/v1/cart/coupon", Map.of("code", "WELCOME15"));

        // On a phone, as a guest: more rice and some eggs, then sign in.
        ApiClient phoneBrowser = new ApiClient(port);
        phoneBrowser.post("/api/v1/cart/items", Map.of("packId", rice, "quantity", 3));
        phoneBrowser.post("/api/v1/cart/items", Map.of("packId", eggs, "quantity", 1));
        assertThat(phoneBrowser.post("/api/v1/auth/login", Map.of("phone", phone, "password", "shobji-123")).status())
                .isEqualTo(200);

        JsonNode merged = phoneBrowser.get("/api/v1/cart").body();
        assertThat(quantity(merged, rice)).isEqualTo(3);
        assertThat(quantity(merged, eggs)).isEqualTo(1);
        assertThat(merged.get("coupon").get("code").asString()).isEqualTo("WELCOME15");
        // Both devices now share one bag.
        assertThat(quantity(laptop.get("/api/v1/cart").body(), eggs)).isEqualTo(1);
        assertThat(jdbc.sql("select count(*) from carts where customer_id = (select id from customers where phone = :p)")
                .param("p", phone).query(Integer.class).single()).isEqualTo(1);

        // After signing out the bag stays with the account, not the browser.
        phoneBrowser.post("/api/v1/auth/logout", null);
        assertThat(phoneBrowser.get("/api/v1/cart").body().get("itemCount").asInt()).isZero();
    }

    @Test
    void ordersAddressesAndBuyAgain() {
        long rice = pack("miniket-rice", "5 kg");
        long milk = pack("pasteurised-milk", "1 L");
        ApiClient customer = register(newPhone());
        customer.post("/api/v1/cart/items", Map.of("packId", rice, "quantity", 1));
        customer.post("/api/v1/cart/items", Map.of("packId", milk, "quantity", 2));
        Response placed = customer.post("/api/v1/checkout", order(customer, true));
        assertThat(placed.status()).isEqualTo(201);
        String number = placed.body().get("number").asString();

        JsonNode history = customer.get("/api/v1/account/orders").body();
        assertThat(history.get("total").asInt()).isEqualTo(1);
        assertThat(history.get("items").get(0).get("number").asString()).isEqualTo(number);
        assertThat(history.get("items").get(0).get("itemCount").asInt()).isEqualTo(3);
        assertThat(history.get("items").get(0).get("lineCount").asInt()).isEqualTo(2);
        assertThat(customer.get("/api/v1/account/orders/" + number).body().get("area").asString()).isEqualTo("Mirpur");

        JsonNode saved = customer.get("/api/v1/account/addresses").body();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).get("isDefault").asBoolean()).isTrue();
        assertThat(saved.get(0).get("area").asString()).isEqualTo("Mirpur");

        // Someone else can't see or repeat the order.
        ApiClient stranger = register(newPhone());
        assertThat(stranger.get("/api/v1/account/orders/" + number).status()).isEqualTo(404);
        assertThat(stranger.post("/api/v1/account/orders/" + number + "/reorder", null).status()).isEqualTo(404);
        assertThat(stranger.get("/api/v1/account/buy-again").body()).isEmpty();

        JsonNode again = customer.get("/api/v1/account/buy-again").body();
        List<String> slugs = StreamSupport.stream(again.spliterator(), false)
                .map(i -> i.get("product").get("slug").asString()).toList();
        assertThat(slugs).containsExactlyInAnyOrder("miniket-rice", "pasteurised-milk");
        assertThat(again.get(0).get("packId").asLong()).isIn(rice, milk);

        // Order again: milk has sold out since, rice comes back.
        jdbc.sql("update packs set stock = 0 where id = :id").param("id", milk).update();
        JsonNode reordered = customer.post("/api/v1/account/orders/" + number + "/reorder", null).body();
        assertThat(reordered.get("added").asInt()).isEqualTo(1);
        assertThat(reordered.get("unavailable").get(0).asString()).contains("Pasteurised Milk");
        assertThat(quantity(reordered.get("cart"), rice)).isEqualTo(1);
        jdbc.sql("update packs set stock = 60 where id = :id").param("id", milk).update();
    }

    @Test
    void theDemoCustomerHasHistory() {
        ApiClient demo = signIn(DemoData.DEMO_PHONE, DemoData.DEMO_PASSWORD);
        JsonNode history = demo.get("/api/v1/account/orders").body();
        assertThat(history.get("total").asInt()).isEqualTo(3);
        assertThat(history.get("items").get(0).get("total").asInt()).isPositive();
        JsonNode again = demo.get("/api/v1/account/buy-again").body();
        assertThat(again).hasSize(8);
        // Bought in two orders, so they come first.
        assertThat(List.of(again.get(0).get("product").get("slug").asString(), again.get(1).get("product").get("slug").asString()))
                .containsExactlyInAnyOrder("farm-eggs-brown", "deshi-red-onion");
        assertThat(again.get(0).get("timesBought").asInt()).isEqualTo(2);
    }

    @Test
    void savedAddresses() {
        ApiClient customer = register(newPhone());
        Map<String, Object> home = new HashMap<>(Map.of("label", "Home", "name", "Nusrat Jahan", "phone", "01711000111",
                "address", "House 9, Road 2", "areaId", area("Mirpur")));
        JsonNode first = customer.post("/api/v1/account/addresses", home).body();
        assertThat(first.get("isDefault").asBoolean()).isTrue();

        Map<String, Object> office = new HashMap<>(home);
        office.put("label", "Office");
        office.put("address", "Level 5, Gulshan Avenue");
        office.put("areaId", area("Gulshan"));
        JsonNode second = customer.post("/api/v1/account/addresses", office).body();
        assertThat(second.get("isDefault").asBoolean()).isFalse();

        customer.post("/api/v1/account/addresses/" + second.get("id").asLong() + "/default", null);
        JsonNode list = customer.get("/api/v1/account/addresses").body();
        assertThat(list.get(0).get("label").asString()).isEqualTo("Office");
        assertThat(list.get(1).get("isDefault").asBoolean()).isFalse();

        assertThat(customer.delete("/api/v1/account/addresses/" + second.get("id").asLong()).status()).isEqualTo(204);
        list = customer.get("/api/v1/account/addresses").body();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("isDefault").asBoolean()).isTrue();

        Response bad = customer.post("/api/v1/account/addresses", Map.of("name", "X", "phone", "12345",
                "address", "House 9, Road 2", "areaId", area("Mirpur")));
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.body().get("fields").has("phone")).isTrue();

        ApiClient stranger = register(newPhone());
        assertThat(stranger.delete("/api/v1/account/addresses/" + first.get("id").asLong()).status()).isEqualTo(404);
    }

    @Test
    void changingThePasswordSignsOutOtherDevices() {
        String phone = newPhone();
        ApiClient here = register(phone);
        ApiClient elsewhere = signIn(phone, "shobji-123");

        Response wrong = here.post("/api/v1/account/password",
                Map.of("currentPassword", "nope-nope", "newPassword", "new-pass-456"));
        assertThat(wrong.status()).isEqualTo(400);
        assertThat(wrong.body().get("fields").has("currentPassword")).isTrue();

        assertThat(here.post("/api/v1/account/password",
                Map.of("currentPassword", "shobji-123", "newPassword", "new-pass-456")).status()).isEqualTo(204);
        assertThat(here.get("/api/v1/account/profile").status()).isEqualTo(200);
        assertThat(elsewhere.get("/api/v1/account/profile").status()).isEqualTo(401);
        signIn(phone, "new-pass-456");
    }
}

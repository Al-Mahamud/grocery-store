package com.haat.store.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.haat.store.support.ApiClient;
import com.haat.store.support.ApiClient.Response;
import com.haat.store.support.IntegrationTest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class CatalogApiTests {

    @LocalServerPort
    int port;

    ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
    }

    @Test
    void categoryTreeWithIconsAndCounts() {
        JsonNode tree = api.get("/api/v1/categories").body();
        assertThat(names(tree)).containsExactly("Fresh", "Pantry", "Dairy & breakfast", "Snacks & drinks", "Home & care");
        assertThat(tree.get(0).get("productCount").asInt()).isEqualTo(17);
        assertThat(tree.get(0).get("children").get(0).get("icon").asString()).isEqualTo("carrot");
        assertThat(tree.get(0).get("image").asString()).startsWith("/media/demo/");
    }

    @Test
    void cardsCarryPacksWithUnitPrices() {
        JsonNode page = api.get("/api/v1/products?category=vegetables&pageSize=60").body();
        assertThat(page.get("total").asInt()).isEqualTo(9);
        JsonNode tomato = find(page.get("items"), "Tomato");
        JsonNode half = tomato.get("packs").get(0);
        assertThat(half.get("label").asString()).isEqualTo("500 g");
        assertThat(half.get("unitPrice").get("price").asInt()).isEqualTo(90);
        assertThat(half.get("unitPrice").get("per").asString()).isEqualTo("kg");
        assertThat(tomato.get("packs").get(1).get("unitPrice").isNull()).isTrue();
        assertThat(tomato.get("weighed").asBoolean()).isTrue();
    }

    @Test
    void brandFacetAndFilter() {
        JsonNode page = api.get("/api/v1/products?brand=" + enc("Megh Dairy")).body();
        assertThat(names(page.get("items"))).containsExactlyInAnyOrder("Pasteurised Milk", "Mishti Doi", "Salted Butter", "Pure Ghee");
        List<String> brands = new ArrayList<>();
        page.get("facets").get("brands").forEach(b -> brands.add(b.get("value").asString()));
        assertThat(brands).contains("Haat Select", "Megh Dairy", "Haat Farms");
    }

    @Test
    void dealsAndOrganic() {
        for (JsonNode item : api.get("/api/v1/products?deal=true&pageSize=60").body().get("items")) {
            boolean discounted = false;
            for (JsonNode pack : item.get("packs")) {
                discounted |= !pack.get("compareAtPrice").isNull();
            }
            assertThat(discounted).as(item.get("name").asString()).isTrue();
        }
        assertThat(names(api.get("/api/v1/products?organic=true").body().get("items")))
                .containsExactlyInAnyOrder("Turmeric Powder", "Guava");
    }

    @Test
    void searchAndSuggestions() {
        // Name matches come before products that only mention rice in their description.
        assertThat(names(api.get("/api/v1/products?q=rice").body().get("items")).subList(0, 2))
                .containsExactlyInAnyOrder("Miniket Rice", "Chinigura Rice");
        assertThat(names(api.get("/api/v1/products?q=" + enc("ডিম")).body().get("items")))
                .containsExactlyInAnyOrder("Farm Eggs (brown)", "Duck Eggs");
        JsonNode suggestions = api.get("/api/v1/search/suggest?q=tom").body();
        assertThat(suggestions.get(0).get("name").asString()).isEqualTo("Tomato");
        assertThat(suggestions.get(0).get("price").asInt()).isEqualTo(45);
        assertThat(api.get("/api/v1/search/suggest?q=").body()).isEmpty();
    }

    @Test
    void sortsByCheapestPack() {
        int previous = 0;
        for (JsonNode item : api.get("/api/v1/products?sort=price_asc&pageSize=60").body().get("items")) {
            int cheapest = Integer.MAX_VALUE;
            for (JsonNode pack : item.get("packs")) {
                cheapest = Math.min(cheapest, pack.get("price").asInt());
            }
            assertThat(cheapest).isGreaterThanOrEqualTo(previous);
            previous = cheapest;
        }
        assertThat(api.get("/api/v1/products?sort=random").status()).isEqualTo(400);
    }

    @Test
    void productPage() {
        Response response = api.get("/api/v1/products/soybean-oil");
        assertThat(response.status()).isEqualTo(200);
        JsonNode oil = response.body();
        assertThat(names(oil.get("breadcrumb"))).containsExactly("Pantry", "Oil & ghee");
        assertThat(oil.get("packs")).hasSize(3);
        assertThat(oil.get("packs").get(0).get("maxPerOrder").asInt()).isEqualTo(6);
        assertThat(oil.get("packs").get(1).get("unitPrice").get("price").asInt()).isEqualTo(170);
        assertThat(oil.get("packs").get(1).get("unitPrice").get("per").asString()).isEqualTo("L");
        assertThat(api.get("/api/v1/products/no-such-thing").body().get("code").asString()).isEqualTo("product.not_found");
        assertThat(api.get(oil.get("image").asString()).status()).isEqualTo(200);
    }

    @Test
    void homeRails() {
        JsonNode home = api.get("/api/v1/home").body();
        assertThat(home.get("deals")).isNotEmpty();
        for (JsonNode item : home.get("fresh")) {
            assertThat(item.get("fresh").asBoolean()).isTrue();
        }
        assertThat(home.get("bestSellers").get(0).get("name").asString()).isEqualTo("Miniket Rice");
        assertThat(home.get("pantry")).isNotEmpty();
    }

    @Test
    void deliveryAreasAndSlots() {
        assertThat(api.get("/api/v1/delivery/areas").body()).hasSize(16);
        JsonNode days = api.get("/api/v1/delivery/slots").body();
        assertThat(days).hasSize(3);
        assertThat(days.get(0).get("label").asString()).isEqualTo("Today");
        assertThat(days.get(1).get("label").asString()).isEqualTo("Tomorrow");
        JsonNode tomorrow = days.get(1).get("slots");
        assertThat(tomorrow).hasSize(7);
        assertThat(tomorrow.get(0).get("label").asString()).isEqualTo("8–10 am");
        assertThat(tomorrow.get(1).get("label").asString()).isEqualTo("10 am–12 pm");
        assertThat(tomorrow.get(5).get("label").asString()).isEqualTo("6–8 pm");
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static JsonNode find(JsonNode items, String name) {
        for (JsonNode item : items) {
            if (item.get("name").asString().equals(name)) {
                return item;
            }
        }
        throw new AssertionError("No " + name);
    }

    private static List<String> names(JsonNode nodes) {
        List<String> names = new ArrayList<>();
        nodes.forEach(n -> names.add(n.get("name").asString()));
        return names;
    }

    @Test
    void healthChecksAnswerWithoutSigningIn() {
        ApiClient anyone = new ApiClient(port);
        for (String path : java.util.List.of("/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness")) {
            assertThat(anyone.get(path).status()).as(path).isEqualTo(200);
        }
    }
}

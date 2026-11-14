package com.haat.store.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A new shop starts with the usual grocery categories and the Dhaka areas we deliver to, so the
 * owner can add products straight away. Runs only while there are no categories at all; after
 * that, both are edited in the admin area. Turn off with {@code store.starter-data=false}.
 */
@Component
@Order(0)
public class StarterCatalog implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StarterCatalog.class);

    /** parent slug (null for top level), name, slug, icon, colour. */
    public static final List<String[]> CATEGORIES = List.of(
            new String[] {null, "Fresh", "fresh", "leaf", "#E5F4E3"},
            new String[] {"fresh", "Vegetables", "vegetables", "carrot", "#E5F4E3"},
            new String[] {"fresh", "Fruits", "fruits", "apple", "#FFF0D6"},
            new String[] {"fresh", "Fish & meat", "fish-meat", "fish", "#E2EEF9"},
            new String[] {null, "Pantry", "pantry", "wheat", "#FBEBD3"},
            new String[] {"pantry", "Rice & flour", "rice-flour", "wheat", "#FBEBD3"},
            new String[] {"pantry", "Oil & ghee", "oil-ghee", "droplet", "#FFF4C2"},
            new String[] {"pantry", "Dal & lentils", "dal", "bean", "#FDE4D3"},
            new String[] {"pantry", "Spices, salt & sugar", "spices", "flame", "#FDE0DC"},
            new String[] {null, "Dairy & breakfast", "dairy-breakfast", "milk", "#E5F0FF"},
            new String[] {"dairy-breakfast", "Milk & dairy", "milk-dairy", "milk", "#E5F0FF"},
            new String[] {"dairy-breakfast", "Eggs", "eggs", "egg", "#F8EEDF"},
            new String[] {"dairy-breakfast", "Bread & bakery", "bakery", "croissant", "#F6E7D4"},
            new String[] {null, "Snacks & drinks", "snacks-drinks", "cookie", "#F3E5F5"},
            new String[] {"snacks-drinks", "Snacks & biscuits", "snacks", "cookie", "#F3E5F5"},
            new String[] {"snacks-drinks", "Tea & coffee", "tea-coffee", "coffee", "#F1E4DA"},
            new String[] {"snacks-drinks", "Drinks", "drinks", "cup-soda", "#E0F4F7"},
            new String[] {null, "Home & care", "home-care", "sparkles", "#E6F2EE"},
            new String[] {"home-care", "Cleaning", "cleaning", "spray-can", "#E6F2EE"},
            new String[] {"home-care", "Personal care", "personal-care", "bath", "#FCE8EF"});

    public static final List<String> AREAS = List.of("Dhanmondi", "Gulshan", "Banani", "Baridhara", "Bashundhara R/A",
            "Mohammadpur", "Lalmatia", "Mirpur", "Uttara", "Mohakhali", "Tejgaon", "Badda", "Rampura", "Khilgaon",
            "Motijheel", "Old Dhaka");

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final boolean enabled;

    StarterCatalog(JdbcClient jdbc, TransactionTemplate tx,
                   @org.springframework.beans.factory.annotation.Value("${store.starter-data:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || jdbc.sql("select count(*) from categories").query(Long.class).single() > 0) {
            return;
        }
        tx.executeWithoutResult(status -> {
            Map<String, Long> ids = new HashMap<>();
            for (int i = 0; i < CATEGORIES.size(); i++) {
                String[] c = CATEGORIES.get(i);
                long id = jdbc.sql("""
                                insert into categories (parent_id, name, slug, icon, color, position)
                                values (:parent, :name, :slug, :icon, :color, :position) returning id""")
                        .param("parent", c[0] == null ? null : ids.get(c[0])).param("name", c[1]).param("slug", c[2])
                        .param("icon", c[3]).param("color", c[4]).param("position", i)
                        .query(Long.class).single();
                ids.put(c[2], id);
            }
            if (jdbc.sql("select count(*) from delivery_areas").query(Long.class).single() == 0) {
                for (int i = 0; i < AREAS.size(); i++) {
                    jdbc.sql("insert into delivery_areas (name, slug, position) values (:n, :s, :p)")
                            .param("n", AREAS.get(i)).param("s", slug(AREAS.get(i))).param("p", i).update();
                }
            }
        });
        log.info("Added the starter categories and delivery areas");
    }

    static String slug(String text) {
        return text.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}

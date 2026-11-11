package com.haat.store.support;

import com.haat.store.shared.StoreProperties;
import com.haat.store.support.DemoArt.Item;
import com.haat.store.support.DemoArt.Kind;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A believable grocery shop for local runs and tests: categories, about fifty products with packs,
 * delivery areas and coupons. Brands are made up. Deterministic, so tests can rely on it.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DemoData {

    record Pack(String label, String amount, String unit, int price, Integer compareAt, int max) {
    }

    record Product(String name, String localName, String category, String brand, Kind kind, String color,
                   String accent, String label, String sub, boolean organic, boolean local, boolean fresh,
                   boolean weighed, String storage, String origin, int sold, boolean featured, String description,
                   List<Pack> packs) {
    }

    static Pack p(String label, String amount, String unit, int price) {
        return new Pack(label, amount, unit, price, null, 20);
    }

    static Pack deal(String label, String amount, String unit, int price, int was) {
        return new Pack(label, amount, unit, price, was, 20);
    }

    static Pack limited(String label, String amount, String unit, int price, int max) {
        return new Pack(label, amount, unit, price, null, max);
    }

    static final String FARM = "Haat Farms";
    static final String SELECT = "Haat Select";
    static final String FRIDGE = "Keep refrigerated";
    static final String COOL = "Store in a cool, dry place";

    static Product veg(String name, String bn, Kind kind, String color, String accent, String origin, int sold,
                       String description, Pack... packs) {
        return new Product(name, bn, "vegetables", FARM, kind, color, accent, null, null, false, true, true, true,
                "Keep in a cool, airy basket", origin, sold, false, description, List.of(packs));
    }

    static final List<Product> PRODUCTS = List.of(
            veg("Tomato", "টমেটো", Kind.ROUND, "#E5412F", "#3F8F3A", "Jashore", 920,
                    "Firm, ripe deshi tomatoes picked the day before delivery. Great for salads, bhorta and curries.",
                    p("500 g", "500", "G", 45), p("1 kg", "1", "KG", 80)),
            veg("Diamond Potato", "আলু", Kind.POTATO, "#D3A86A", null, "Munshiganj", 1300,
                    "All-purpose potatoes that hold their shape in curries and fry up golden.",
                    p("1 kg", "1", "KG", 45), deal("3 kg", "3", "KG", 125, 135)),
            veg("Deshi Red Onion", "দেশি পেঁয়াজ", Kind.ONION, "#B8506A", null, "Pabna", 1450,
                    "Small, pungent local onions with the deep flavour Bangladeshi cooking needs.",
                    p("1 kg", "1", "KG", 95), p("2 kg", "2", "KG", 185)),
            veg("Green Chilli", "কাঁচা মরিচ", Kind.CHILLI, "#3C9A3A", "#2F6E2B", "Bogura", 800,
                    "Fresh, glossy green chillies with a lively heat.",
                    p("250 g", "250", "G", 30), p("500 g", "500", "G", 55)),
            veg("Carrot", "গাজর", Kind.CARROT, "#F07F22", "#4F9D3A", "Manikganj", 410,
                    "Sweet, crunchy winter carrots, lovely raw or in halwa.",
                    p("500 g", "500", "G", 40), deal("1 kg", "1", "KG", 70, 80)),
            veg("Cucumber", "শসা", Kind.CUCUMBER, "#4E8F3A", null, "Narsingdi", 380,
                    "Crisp local cucumbers for salads and raita.",
                    p("500 g", "500", "G", 35), p("1 kg", "1", "KG", 65)),
            veg("Brinjal", "বেগুন", Kind.EGGPLANT, "#5B2A6E", "#5E8C3A", "Jashore", 330,
                    "Long, shiny purple brinjal for begun bhaja and bhorta.",
                    p("500 g", "500", "G", 40), p("1 kg", "1", "KG", 75)),
            veg("Spinach", "পালং শাক", Kind.LEAFY, "#3E8E41", "#C9A66B", "Savar", 290,
                    "A generous bunch of tender spinach leaves, washed once at the farm.",
                    p("1 bunch", "1", "PCS", 25), p("3 bunches", "3", "PCS", 70)),
            veg("Lemon", "লেবু", Kind.ROUND, "#B5D334", "#3F8F3A", "Sylhet", 640,
                    "Juicy, thin-skinned kagzi lemons. Squeeze over dal, rice or a glass of water.",
                    p("4 pcs", "4", "PCS", 30), deal("12 pcs", "12", "PCS", 80, 90)),
            new Product("Sagor Banana", "সাগর কলা", "fruits", FARM, Kind.BANANA, "#F4D03F", "#6E8B3D", null, null,
                    false, true, true, false, "Keep at room temperature", "Narsingdi", 760, true,
                    "Sweet, creamy bananas sold by the half or full dozen.",
                    List.of(p("6 pcs", "6", "PCS", 65), p("12 pcs", "12", "PCS", 120))),
            new Product("Malta", "মাল্টা", "fruits", FARM, Kind.ROUND, "#F39C12", "#3F8F3A", null, null,
                    false, true, true, true, "Keep at room temperature", "Khagrachari", 350, false,
                    "Juicy local malta oranges, perfect for fresh juice.",
                    List.of(p("1 kg", "1", "KG", 260))),
            new Product("Green Apple", "সবুজ আপেল", "fruits", FARM, Kind.ROUND, "#9BC53D", "#3F8F3A", null, null,
                    false, false, false, true, FRIDGE, "Imported", 280, false,
                    "Tart, crunchy green apples for snacking and salads.",
                    List.of(deal("1 kg", "1", "KG", 299, 340))),
            new Product("Guava", "পেয়ারা", "fruits", FARM, Kind.ROUND, "#8CC152", "#3F8F3A", null, null,
                    true, true, true, true, "Keep at room temperature", "Pirojpur", 300, false,
                    "Crunchy Swarupkathi guavas grown without chemical sprays.",
                    List.of(p("1 kg", "1", "KG", 90))),
            new Product("Rui Fish", "রুই মাছ", "fish-meat", FARM, Kind.FISH, "#9FB4C7", "#6C8299", null, null,
                    false, true, true, true, FRIDGE, "Rajshahi", 520, true,
                    "Whole river rui, scaled and cut into curry pieces at no extra cost.",
                    List.of(p("1 kg", "1", "KG", 360), p("2 kg", "2", "KG", 700))),
            new Product("Hilsa", "ইলিশ", "fish-meat", FARM, Kind.FISH, "#C9D6DF", "#8FA4B5", null, null,
                    false, true, true, true, FRIDGE, "Chandpur", 300, true,
                    "Padma hilsa, about 1 kg each. Ask us to cut it into pieces in the order note.",
                    List.of(limited("1 pc (~1 kg)", "1", "PCS", 1450, 4))),
            new Product("Broiler Chicken", "ব্রয়লার মুরগি", "fish-meat", FARM, Kind.MEAT, "#E8A0A0", "#F7E1D7",
                    "CHICKEN", null, false, true, true, true, FRIDGE, "Gazipur", 870, false,
                    "Whole broiler, skin off and cut into curry pieces.",
                    List.of(p("~1 kg", "1", "KG", 260), deal("~2 kg", "2", "KG", 499, 520))),
            new Product("Beef (bone-in)", "গরুর মাংস", "fish-meat", FARM, Kind.MEAT, "#B23A48", "#F2D0A9",
                    "BEEF", null, false, true, true, true, FRIDGE, "Dhaka", 610, false,
                    "Fresh deshi beef with bone, cut for curry and kala bhuna.",
                    List.of(p("500 g", "500", "G", 395), p("1 kg", "1", "KG", 780))),
            new Product("Miniket Rice", "মিনিকেট চাল", "rice-flour", SELECT, Kind.BAG, "#2E7D5B", "#C9A35A",
                    "MINIKET RICE", null, false, true, false, false, COOL, "Kushtia", 1520, true,
                    "Fine, polished everyday rice that cooks up fluffy and separate.",
                    List.of(p("1 kg", "1", "KG", 78), deal("5 kg", "5", "KG", 365, 390), p("25 kg", "25", "KG", 1790))),
            new Product("Chinigura Rice", "চিনিগুঁড়া চাল", "rice-flour", SELECT, Kind.BAG, "#7B2D26", "#E0B04A",
                    "CHINIGURA", "aromatic", false, true, false, false, COOL, "Dinajpur", 690, false,
                    "Small-grained aromatic rice for polao, biryani and payesh.",
                    List.of(p("1 kg", "1", "KG", 150), p("2 kg", "2", "KG", 290))),
            new Product("Whole Wheat Atta", "লাল আটা", "rice-flour", "Padma Mills", Kind.BAG, "#B5651D", "#F2C14E",
                    "WHOLE WHEAT", "atta", false, true, false, false, COOL, "Bangladesh", 480, false,
                    "Stone-ground whole wheat flour for soft, nutty ruti.",
                    List.of(p("2 kg", "2", "KG", 120))),
            new Product("Soybean Oil", "সয়াবিন তেল", "oil-ghee", "Nodi Foods", Kind.BOTTLE, "#F2C14E", "#E36414",
                    "SOYBEAN OIL", "fortified", false, false, false, false, COOL, "Bangladesh", 1180, true,
                    "Refined, vitamin-A fortified soybean oil for everyday cooking.",
                    List.of(limited("1 L", "1", "L", 175, 6), deal("2 L", "2", "L", 340, 350), limited("5 L", "5", "L", 845, 3))),
            new Product("Mustard Oil", "সরিষার তেল", "oil-ghee", "Nodi Foods", Kind.BOTTLE, "#D4A017", "#7B2D26",
                    "MUSTARD OIL", "cold pressed", false, true, false, false, COOL, "Sirajganj", 520, false,
                    "Pungent ghani-pressed mustard oil for bhorta, achar and fish curries.",
                    List.of(p("500 ml", "500", "ML", 150), p("1 L", "1", "L", 290))),
            new Product("Pure Ghee", "ঘি", "oil-ghee", "Megh Dairy", Kind.JAR, "#F4C430", "#8B5E34", "PURE GHEE", null,
                    false, true, false, false, COOL, "Sirajganj", 230, false,
                    "Slow-cooked cow ghee with a rich, nutty aroma.",
                    List.of(p("200 g", "200", "G", 380), p("400 g", "400", "G", 720))),
            new Product("Masoor Dal", "মসুর ডাল", "dal", SELECT, Kind.BAG, "#D35400", "#F39C12", "MASOOR DAL", "red lentils",
                    false, true, false, false, COOL, "Faridpur", 990, false,
                    "Small deshi red lentils that cook down quickly into a silky dal.",
                    List.of(p("1 kg", "1", "KG", 140), deal("2 kg", "2", "KG", 270, 280))),
            new Product("Moong Dal", "মুগ ডাল", "dal", SELECT, Kind.BAG, "#C9A227", "#5E8C3A", "MOONG DAL", "split",
                    false, true, false, false, COOL, "Bangladesh", 410, false,
                    "Roasted split moong dal for khichuri and light dal.",
                    List.of(p("1 kg", "1", "KG", 175))),
            new Product("Chickpeas", "ছোলা", "dal", SELECT, Kind.BAG, "#A0522D", "#E0B04A", "CHOLA", "chickpeas",
                    false, false, false, false, COOL, "Imported", 350, false,
                    "Plump chickpeas for chola bhuna and iftar.",
                    List.of(p("1 kg", "1", "KG", 130))),
            new Product("Turmeric Powder", "হলুদ গুঁড়া", "spices", "Kona Kitchen", Kind.JAR, "#E9B10F", "#B5651D",
                    "TURMERIC", null, true, true, false, false, COOL, "Bangladesh", 560, false,
                    "Bright, earthy turmeric ground from sun-dried roots.",
                    List.of(p("100 g", "100", "G", 55), p("200 g", "200", "G", 95))),
            new Product("Chilli Powder", "মরিচ গুঁড়া", "spices", "Kona Kitchen", Kind.JAR, "#C0392B", "#7B241C",
                    "CHILLI", "powder", false, true, false, false, COOL, "Bogura", 520, false,
                    "Deep red chilli powder with steady, warming heat.",
                    List.of(p("200 g", "200", "G", 110))),
            new Product("Garam Masala", "গরম মসলা", "spices", "Kona Kitchen", Kind.JAR, "#6E4B2A", "#C9A35A",
                    "GARAM MASALA", null, false, false, false, false, COOL, "Bangladesh", 210, false,
                    "A warm blend of cardamom, cinnamon and clove for meat and polao.",
                    List.of(deal("50 g", "50", "G", 75, 85))),
            new Product("Iodised Salt", "লবণ", "spices", SELECT, Kind.BAG, "#1F6FB2", "#FFFFFF", "IODISED SALT", null,
                    false, true, false, false, COOL, "Chattogram", 880, false,
                    "Fine, free-flowing iodised salt.",
                    List.of(p("1 kg", "1", "KG", 42))),
            new Product("Sugar", "চিনি", "spices", SELECT, Kind.BAG, "#5DA9E9", "#FFFFFF", "WHITE SUGAR", null,
                    false, false, false, false, COOL, "Bangladesh", 760, false,
                    "Refined white sugar for tea and sweets.",
                    List.of(limited("1 kg", "1", "KG", 135, 5))),
            new Product("Pasteurised Milk", "তরল দুধ", "milk-dairy", "Megh Dairy", Kind.CARTON, "#5DA9E9", "#1D4E89",
                    "FRESH MILK", null, false, true, true, false, FRIDGE, "Sirajganj", 1110, true,
                    "Full-cream pasteurised milk, delivered chilled.",
                    List.of(p("500 ml", "500", "ML", 50), p("1 L", "1", "L", 95))),
            new Product("Mishti Doi", "মিষ্টি দই", "milk-dairy", "Megh Dairy", Kind.JAR, "#E7C8A0", "#8B4513",
                    "MISHTI DOI", null, false, true, true, false, FRIDGE, "Bogura", 450, false,
                    "Bogura's famous caramelised sweet yoghurt in a clay-style tub.",
                    List.of(deal("500 g", "500", "G", 170, 190))),
            new Product("Salted Butter", "মাখন", "milk-dairy", "Megh Dairy", Kind.BOX, "#F2C14E", "#FFFDF7",
                    "BUTTER", "salted", false, false, false, false, FRIDGE, "Imported", 260, false,
                    "Creamy salted butter for toast and baking.",
                    List.of(p("200 g", "200", "G", 260))),
            new Product("Farm Eggs (brown)", "ডিম", "eggs", FARM, Kind.EGGS, "#C98B5A", null, "FARM EGGS", null,
                    false, true, true, false, "Keep in a cool place", "Gazipur", 1380, true,
                    "Fresh brown eggs collected this week.",
                    List.of(p("12 pcs", "12", "PCS", 150), deal("30 pcs", "30", "PCS", 355, 375))),
            new Product("Duck Eggs", "হাঁসের ডিম", "eggs", FARM, Kind.EGGS, "#E8E2D0", null, "DUCK EGGS", null,
                    false, true, true, false, "Keep in a cool place", "Sunamganj", 240, false,
                    "Rich, large duck eggs from haor farms.",
                    List.of(p("12 pcs", "12", "PCS", 240))),
            new Product("Sandwich Bread", "পাউরুটি", "bakery", "Shapla Bakery", Kind.BREAD, "#C68642", null, "SANDWICH",
                    null, false, true, true, false, "Keep in a cool, dry place", "Dhaka", 690, false,
                    "Soft, sliced white bread baked every morning.",
                    List.of(p("1 loaf", "1", "PCS", 75))),
            new Product("Cream Biscuits", "ক্রিম বিস্কুট", "snacks", "Jhorna", Kind.PACK, "#6A1B9A", "#F2C14E",
                    "CREAM BISCUIT", "chocolate", false, false, false, false, COOL, "Bangladesh", 830, false,
                    "Crunchy biscuits with a chocolate cream filling.",
                    List.of(p("1 pack", "1", "PCS", 40), deal("6 packs", "6", "PCS", 210, 240))),
            new Product("Chanachur", "চানাচুর", "snacks", "Jhorna", Kind.PACK, "#E36414", "#F2C14E", "CHANACHUR",
                    "hot mix", false, true, false, false, COOL, "Bangladesh", 720, false,
                    "Crunchy, spicy mix of lentils, peanuts and noodles.",
                    List.of(p("150 g", "150", "G", 50), p("300 g", "300", "G", 90))),
            new Product("Instant Noodles", "নুডলস", "snacks", "Jhorna", Kind.PACK, "#D62828", "#F77F00", "NOODLES",
                    "masala", false, false, false, false, COOL, "Bangladesh", 990, false,
                    "Ready in two minutes with a spicy masala sachet.",
                    List.of(p("4 pack", "4", "PCS", 80), p("8 pack", "8", "PCS", 155))),
            new Product("Black Tea", "চা পাতা", "tea-coffee", "Sylhet Garden", Kind.BOX, "#9B1D20", "#F2C14E",
                    "BLACK TEA", null, false, true, false, false, COOL, "Sreemangal", 880, true,
                    "Strong, brisk CTC tea from Sreemangal gardens. Perfect for milk tea.",
                    List.of(p("200 g", "200", "G", 140), deal("400 g", "400", "G", 255, 275))),
            new Product("Instant Coffee", "কফি", "tea-coffee", SELECT, Kind.JAR, "#4E342E", "#D7A86E", "COFFEE",
                    "instant", false, false, false, false, COOL, "Imported", 260, false,
                    "Smooth instant coffee granules.",
                    List.of(p("100 g", "100", "G", 420))),
            new Product("Mango Juice", "আমের জুস", "drinks", "Nodi Foods", Kind.CARTON, "#F4A300", "#2E7D32",
                    "MANGO JUICE", null, false, true, false, false, "Refrigerate after opening", "Bangladesh", 540, false,
                    "Thick mango drink made with Rajshahi mangoes.",
                    List.of(p("250 ml", "250", "ML", 30), p("1 L", "1", "L", 120))),
            new Product("Drinking Water", "পানি", "drinks", SELECT, Kind.BOTTLE, "#8ECAE6", "#219EBC", "WATER",
                    "mineral", false, true, false, false, COOL, "Bangladesh", 1210, false,
                    "Purified mineral water.",
                    List.of(p("500 ml", "500", "ML", 20), p("1.5 L", "1.5", "L", 30), p("5 L", "5", "L", 90))),
            new Product("Cola", "কোলা", "drinks", "Jhorna", Kind.CAN, "#C62828", "#1A1A2E", "COLA", null,
                    false, false, false, false, "Serve chilled", "Bangladesh", 630, false,
                    "Classic fizzy cola in a can.",
                    List.of(p("250 ml", "250", "ML", 40), deal("6 cans", "6", "PCS", 215, 240))),
            new Product("Dishwash Liquid", "ডিশওয়াশ", "cleaning", "Shapla Home", Kind.BOTTLE, "#43AA8B", "#F9C74F",
                    "DISHWASH", "lemon", false, false, false, false, "Keep away from children", "Bangladesh", 520, false,
                    "Cuts through oil with a fresh lemon scent.",
                    List.of(p("500 ml", "500", "ML", 115), p("1 L", "1", "L", 210))),
            new Product("Laundry Powder", "ডিটারজেন্ট", "cleaning", "Shapla Home", Kind.BOX, "#277DA1", "#F94144",
                    "LAUNDRY", "powder", false, false, false, false, "Keep dry", "Bangladesh", 470, false,
                    "Powerful laundry powder for hand and machine wash.",
                    List.of(p("500 g", "500", "G", 95), deal("1 kg", "1", "KG", 175, 190))),
            new Product("Floor Cleaner", "ফ্লোর ক্লিনার", "cleaning", "Shapla Home", Kind.BOTTLE, "#9D4EDD", "#E0AAFF",
                    "FLOOR CLEANER", "lavender", false, false, false, false, "Keep away from children", "Bangladesh", 310, false,
                    "Kills germs and leaves a lavender fragrance.",
                    List.of(p("1 L", "1", "L", 210))),
            new Product("Rose Bath Soap", "সাবান", "personal-care", "Shapla Home", Kind.SOAP, "#F4A6B8", null, "ROSE",
                    null, false, false, false, false, COOL, "Bangladesh", 540, false,
                    "Gentle moisturising soap with rose extract.",
                    List.of(p("100 g", "100", "G", 55), deal("4 x 100 g", "4", "PCS", 199, 220))),
            new Product("Herbal Toothpaste", "টুথপেস্ট", "personal-care", "Shapla Home", Kind.BOX, "#2A9D8F", "#E9C46A",
                    "TOOTHPASTE", "neem & clove", false, false, false, false, COOL, "Bangladesh", 450, false,
                    "Neem and clove toothpaste for strong teeth and fresh breath.",
                    List.of(p("140 g", "140", "G", 150)))
    );

    static final List<String[]> CATEGORIES = List.of(
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

    static final List<String> AREAS = List.of("Dhanmondi", "Gulshan", "Banani", "Baridhara", "Bashundhara R/A",
            "Mohammadpur", "Lalmatia", "Mirpur", "Uttara", "Mohakhali", "Tejgaon", "Badda", "Rampura", "Khilgaon",
            "Motijheel", "Old Dhaka");

    @Bean
    ApplicationRunner demoDataLoader(JdbcClient jdbc, TransactionTemplate tx, StoreProperties properties) {
        return new Loader(jdbc, tx, properties.storageRoot().resolve("demo"));
    }

    static final class Loader implements ApplicationRunner {

        private final JdbcClient jdbc;
        private final TransactionTemplate tx;
        private final Path folder;
        private final Map<String, Long> categories = new HashMap<>();

        Loader(JdbcClient jdbc, TransactionTemplate tx, Path folder) {
            this.jdbc = jdbc;
            this.tx = tx;
            this.folder = folder;
        }

        @Override
        public void run(ApplicationArguments args) {
            if (jdbc.sql("select count(*) from products").query(Long.class).single() == 0) {
                tx.executeWithoutResult(status -> load());
            }
        }

        private void load() {
            for (int i = 0; i < CATEGORIES.size(); i++) {
                String[] c = CATEGORIES.get(i);
                long id = jdbc.sql("""
                                insert into categories (parent_id, name, slug, icon, color, position)
                                values (:parent, :name, :slug, :icon, :color, :position) returning id""")
                        .param("parent", c[0] == null ? null : categories.get(c[0]))
                        .param("name", c[1]).param("slug", c[2]).param("icon", c[3]).param("color", c[4])
                        .param("position", i).query(Long.class).single();
                categories.put(c[2], id);
            }
            for (int i = 0; i < AREAS.size(); i++) {
                jdbc.sql("insert into delivery_areas (name, slug, position) values (:name, :slug, :position)")
                        .param("name", AREAS.get(i)).param("slug", slug(AREAS.get(i))).param("position", i).update();
            }
            coupon("WELCOME15", "15% off your first order (up to ৳150)", "PERCENT", 15, 150, 500, null);
            coupon("FRESH50", "৳50 off orders over ৳800", "FIXED", 50, null, 800, null);
            coupon("FREEDEL", "Free delivery on orders over ৳600", "FREE_SHIPPING", 0, null, 600, null);
            coupon("OLD10", "Last month's offer", "PERCENT", 10, null, 0, Instant.now().minus(Duration.ofDays(20)));

            Instant now = Instant.now();
            for (int i = 0; i < PRODUCTS.size(); i++) {
                product(PRODUCTS.get(i), i, now);
            }
            jdbc.sql("""
                    update categories c set image_url = (
                        select p.image_url from products p
                         where p.category_id in (select id from categories where id = c.id or parent_id = c.id)
                         order by p.sold_count desc limit 1)""").update();
        }

        private void coupon(String code, String description, String kind, int value, Integer max, int min, Instant ends) {
            jdbc.sql("""
                            insert into coupons (code, description, kind, value, max_discount, min_subtotal, ends_at)
                            values (:code, :description, :kind, :value, :max, :min, :ends)""")
                    .param("code", code).param("description", description).param("kind", kind).param("value", value)
                    .param("max", max).param("min", min).param("ends", ends == null ? null : Timestamp.from(ends))
                    .update();
        }

        private void product(Product p, int index, Instant now) {
            String slug = slug(p.name());
            String image = write(slug + ".svg", DemoArt.draw(new Item(p.kind(), p.color(), p.accent(), p.label(), p.sub(), index + 11L)));
            long id = jdbc.sql("""
                            insert into products (name, local_name, slug, category_id, brand, description, storage, origin,
                                                  organic, local, fresh, weighed, image_url, status, featured, sold_count,
                                                  created_at, updated_at)
                            values (:name, :local, :slug, :category, :brand, :description, :storage, :origin, :organic,
                                    :isLocal, :fresh, :weighed, :image, 'ACTIVE', :featured, :sold, :created, :created)
                            returning id""")
                    .param("name", p.name()).param("local", p.localName()).param("slug", slug)
                    .param("category", categories.get(p.category())).param("brand", p.brand())
                    .param("description", p.description()).param("storage", p.storage()).param("origin", p.origin())
                    .param("organic", p.organic()).param("isLocal", p.local()).param("fresh", p.fresh())
                    .param("weighed", p.weighed()).param("image", image).param("featured", p.featured())
                    .param("sold", p.sold())
                    .param("created", Timestamp.from(now.minus(Duration.ofDays(index % 40)).minus(Duration.ofMinutes(index))))
                    .query(Long.class).single();
            for (int k = 0; k < p.packs().size(); k++) {
                Pack pack = p.packs().get(k);
                jdbc.sql("""
                                insert into packs (product_id, label, amount, unit, sku, price, compare_at_price, stock,
                                                   max_per_order, position)
                                values (:p, :label, :amount, :unit, :sku, :price, :compare, :stock, :max, :position)""")
                        .param("p", id).param("label", pack.label()).param("amount", new BigDecimal(pack.amount()))
                        .param("unit", pack.unit()).param("sku", String.format(Locale.ROOT, "HT-%03d-%d", index + 1, k + 1))
                        .param("price", pack.price()).param("compare", pack.compareAt())
                        .param("stock", stock(index, k)).param("max", pack.max()).param("position", k)
                        .update();
            }
        }

        /** Plenty of most things; a few low or sold out. */
        private static int stock(int product, int pack) {
            int h = (product * 37 + pack * 11) % 29;
            return h == 5 ? 0 : h < 3 ? h + 1 : 20 + h * 3;
        }

        private String write(String name, String svg) {
            try {
                Files.createDirectories(folder);
                Files.writeString(folder.resolve(name), svg);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return "/media/demo/" + name;
        }

        static String slug(String text) {
            return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        }
    }
}

package com.haat.store.admin;

import com.haat.store.shared.ApiException;
import com.haat.store.shared.StoreProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Products, their packs, pictures and stock, as the shop edits them. */
@Service
public class AdminCatalog {

    public static final Set<String> UNITS = Set.of("G", "KG", "ML", "L", "PCS");
    public static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "ARCHIVED");

    public record PackInput(Long id, String label, BigDecimal amount, String unit, String sku, Integer price,
                            Integer compareAtPrice, Integer stock, Integer maxPerOrder) {
    }

    public record ProductInput(String name, String localName, String slug, Long categoryId, String brand,
                               String description, String storage, String origin, Boolean organic, Boolean local,
                               Boolean fresh, Boolean weighed, Boolean featured, String status,
                               List<PackInput> packs) {
    }

    public record PackView(long id, String label, BigDecimal amount, String unit, String sku, int price,
                           Integer compareAtPrice, int stock, int maxPerOrder) {
    }

    public record ProductView(long id, String name, String localName, String slug, long categoryId, String category,
                              String brand, String description, String storage, String origin, boolean organic,
                              boolean local, boolean fresh, boolean weighed, boolean featured, String status,
                              String image, int soldCount, List<PackView> packs) {
    }

    public record ProductRow(long id, String name, String localName, String slug, String image, String category,
                             String brand, String status, int packCount, int fromPrice, int stock, boolean low,
                             int soldCount) {
    }

    public record ProductPage(List<ProductRow> items, int page, int pageSize, long total) {
    }

    public record StockRow(long packId, long productId, String product, String image, String label, String sku,
                           int price, int stock, int maxPerOrder, String status) {
    }

    private final JdbcClient jdbc;
    private final StoreProperties properties;

    AdminCatalog(JdbcClient jdbc, StoreProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    public ProductPage products(String q, Long categoryId, String status, boolean lowOnly, int page) {
        List<String> where = new ArrayList<>(List.of("true"));
        Map<String, Object> params = new HashMap<>();
        params.put("low", properties.admin().lowStock());
        if (q != null && !q.isBlank()) {
            where.add("(p.name ilike :q or p.local_name ilike :q or p.brand ilike :q or exists "
                    + "(select 1 from packs s where s.product_id = p.id and s.sku ilike :q))");
            params.put("q", "%" + q.trim() + "%");
        }
        if (categoryId != null) {
            where.add("(p.category_id = :cat or p.category_id in (select id from categories where parent_id = :cat))");
            params.put("cat", categoryId);
        }
        if (status != null && STATUSES.contains(status)) {
            where.add("p.status = :status");
            params.put("status", status);
        }
        if (lowOnly) {
            where.add("exists (select 1 from packs s where s.product_id = p.id and s.stock <= :low)");
        }
        String condition = String.join(" and ", where);
        int size = 30;
        long total = jdbc.sql("select count(*) from products p where " + condition).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<ProductRow> rows = jdbc.sql("""
                        select p.id, p.name, p.local_name, p.slug, p.image_url, c.name, p.brand, p.status,
                               (select count(*) from packs s where s.product_id = p.id),
                               coalesce((select min(price) from packs s where s.product_id = p.id), 0),
                               coalesce((select sum(stock) from packs s where s.product_id = p.id), 0),
                               exists (select 1 from packs s where s.product_id = p.id and s.stock <= :low),
                               p.sold_count
                          from products p join categories c on c.id = p.category_id
                         where %s order by p.status = 'ARCHIVED', p.name limit :limit offset :offset""".formatted(condition))
                .params(params)
                .query((rs, n) -> new ProductRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getInt(9), rs.getInt(10),
                        rs.getInt(11), rs.getBoolean(12), rs.getInt(13)))
                .list();
        return new ProductPage(rows, page, size, total);
    }

    public ProductView product(long id) {
        List<PackView> packs = jdbc.sql("""
                        select id, label, amount, unit, sku, price, compare_at_price, stock, max_per_order
                          from packs where product_id = :id order by position, id""")
                .param("id", id)
                .query((rs, n) -> new PackView(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3).stripTrailingZeros(),
                        rs.getString(4), rs.getString(5), rs.getInt(6), (Integer) rs.getObject(7), rs.getInt(8), rs.getInt(9)))
                .list();
        return jdbc.sql("""
                        select p.id, p.name, p.local_name, p.slug, p.category_id, c.name, p.brand, p.description, p.storage,
                               p.origin, p.organic, p.local, p.fresh, p.weighed, p.featured, p.status, p.image_url,
                               p.sold_count
                          from products p join categories c on c.id = p.category_id where p.id = :id""")
                .param("id", id)
                .query((rs, n) -> new ProductView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getLong(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                        rs.getString(10), rs.getBoolean(11), rs.getBoolean(12), rs.getBoolean(13), rs.getBoolean(14),
                        rs.getBoolean(15), rs.getString(16), rs.getString(17), rs.getInt(18), packs))
                .optional()
                .orElseThrow(() -> ApiException.notFound("product"));
    }

    /** Creates ({@code id} null) or updates a product with all of its packs. Packs left out are removed. */
    @Transactional
    public ProductView save(Long id, ProductInput in) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = trim(in.name());
        if (name == null) {
            errors.put("name", "Please enter the product name.");
        }
        if (in.categoryId() == null || jdbc.sql("select count(*) from categories where id = :id").param("id", in.categoryId())
                .query(Integer.class).single() == 0) {
            errors.put("categoryId", "Please choose a category.");
        }
        String status = in.status() == null ? "DRAFT" : in.status();
        if (!STATUSES.contains(status)) {
            errors.put("status", "Choose draft, on sale or archived.");
        }
        List<PackInput> packs = in.packs() == null ? List.of() : in.packs();
        if (status.equals("ACTIVE") && packs.isEmpty()) {
            errors.put("packs", "A product on sale needs at least one pack.");
        }
        Set<String> labels = new HashSet<>();
        Set<String> skus = new HashSet<>();
        for (int i = 0; i < packs.size(); i++) {
            PackInput p = packs.get(i);
            String at = "packs." + i + ".";
            if (trim(p.label()) == null) {
                errors.put(at + "label", "Name the pack, e.g. 1 kg.");
            } else if (!labels.add(p.label().trim().toLowerCase(Locale.ROOT))) {
                errors.put(at + "label", "Two packs have this name.");
            }
            if (p.amount() == null || p.amount().signum() <= 0) {
                errors.put(at + "amount", "Enter the amount.");
            }
            if (p.unit() == null || !UNITS.contains(p.unit())) {
                errors.put(at + "unit", "Choose a unit.");
            }
            if (trim(p.sku()) == null) {
                errors.put(at + "sku", "Enter a SKU.");
            } else if (!skus.add(p.sku().trim().toUpperCase(Locale.ROOT))) {
                errors.put(at + "sku", "Two packs have this SKU.");
            }
            if (p.price() == null || p.price() <= 0) {
                errors.put(at + "price", "Enter a price above ৳0.");
            } else if (p.compareAtPrice() != null && p.compareAtPrice() <= p.price()) {
                errors.put(at + "compareAtPrice", "The old price must be higher than the price.");
            }
            if (p.stock() != null && p.stock() < 0) {
                errors.put(at + "stock", "Stock can't be below 0.");
            }
            if (p.maxPerOrder() != null && (p.maxPerOrder() < 1 || p.maxPerOrder() > 99)) {
                errors.put(at + "maxPerOrder", "Between 1 and 99.");
            }
        }
        String slug = trim(in.slug()) == null ? slug(name == null ? "" : name) : slug(in.slug());
        if (slug.isEmpty()) {
            errors.put("slug", "Enter a web address name using English letters.");
        } else if (jdbc.sql("select count(*) from products where slug = :s and id <> coalesce(cast(:id as bigint), -1)")
                .param("s", slug).param("id", id).query(Integer.class).single() > 0) {
            errors.put("slug", "Another product already uses this web address.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.", errors);
        }
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        fields.put("local", trim(in.localName()));
        fields.put("slug", slug);
        fields.put("cat", in.categoryId());
        fields.put("brand", trim(in.brand()));
        fields.put("description", in.description() == null ? "" : in.description().trim());
        fields.put("storage", trim(in.storage()));
        fields.put("origin", trim(in.origin()));
        fields.put("organic", Boolean.TRUE.equals(in.organic()));
        fields.put("isLocal", Boolean.TRUE.equals(in.local()));
        fields.put("fresh", Boolean.TRUE.equals(in.fresh()));
        fields.put("weighed", Boolean.TRUE.equals(in.weighed()));
        fields.put("featured", Boolean.TRUE.equals(in.featured()));
        fields.put("status", status);
        long productId;
        if (id == null) {
            productId = jdbc.sql("""
                            insert into products (name, local_name, slug, category_id, brand, description, storage, origin,
                                                  organic, local, fresh, weighed, featured, status)
                            values (:name, :local, :slug, :cat, :brand, :description, :storage, :origin, :organic,
                                    :isLocal, :fresh, :weighed, :featured, :status) returning id""")
                    .params(fields).query(Long.class).single();
        } else {
            product(id);
            fields.put("id", id);
            jdbc.sql("""
                            update products set name = :name, local_name = :local, slug = :slug, category_id = :cat,
                                   brand = :brand, description = :description, storage = :storage, origin = :origin,
                                   organic = :organic, local = :isLocal, fresh = :fresh, weighed = :weighed,
                                   featured = :featured, status = :status, updated_at = now()
                             where id = :id""")
                    .params(fields).update();
            productId = id;
        }
        savePacks(productId, packs);
        return product(productId);
    }

    private void savePacks(long productId, List<PackInput> packs) {
        Set<Long> keep = new HashSet<>();
        packs.stream().map(PackInput::id).filter(java.util.Objects::nonNull).forEach(keep::add);
        List<Long> existing = jdbc.sql("select id from packs where product_id = :p").param("p", productId)
                .query(Long.class).list();
        for (Long packId : keep) {
            if (!existing.contains(packId)) {
                throw ApiException.badRequest("pack.not_found", "One of the packs doesn't belong to this product.");
            }
        }
        // Remove dropped packs first, and park labels, so renaming "1 kg" ↔ "2 kg" can't clash.
        for (Long old : existing) {
            if (!keep.contains(old)) {
                jdbc.sql("delete from packs where id = :id").param("id", old).update();
            }
        }
        jdbc.sql("update packs set label = '~' || id, sku = '~' || id where product_id = :p").param("p", productId).update();
        try {
            for (int i = 0; i < packs.size(); i++) {
                PackInput p = packs.get(i);
                Map<String, Object> f = new HashMap<>();
                f.put("product", productId);
                f.put("label", p.label().trim());
                f.put("amount", p.amount());
                f.put("unit", p.unit());
                f.put("sku", p.sku().trim().toUpperCase(Locale.ROOT));
                f.put("price", p.price());
                f.put("compare", p.compareAtPrice());
                f.put("stock", p.stock() == null ? 0 : p.stock());
                f.put("max", p.maxPerOrder() == null ? 20 : p.maxPerOrder());
                f.put("position", i);
                if (p.id() == null) {
                    jdbc.sql("""
                                    insert into packs (product_id, label, amount, unit, sku, price, compare_at_price, stock,
                                                       max_per_order, position)
                                    values (:product, :label, :amount, :unit, :sku, :price, :compare, :stock, :max, :position)""")
                            .params(f).update();
                } else {
                    f.put("id", p.id());
                    jdbc.sql("""
                                    update packs set label = :label, amount = :amount, unit = :unit, sku = :sku, price = :price,
                                                     compare_at_price = :compare, stock = :stock, max_per_order = :max,
                                                     position = :position
                                     where id = :id""")
                            .params(f).update();
                }
            }
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "pack.sku_taken", "A SKU is already used by another product.",
                    Map.of("packs", "A SKU is already used by another product."));
        }
    }

    /** Stores a JPEG, PNG or WebP picture (checked by its bytes, not its name) as the product's image. */
    @Transactional
    public ProductView setImage(long id, byte[] bytes) {
        product(id);
        String ext = imageType(bytes);
        if (ext == null) {
            throw ApiException.badRequest("image.type", "Upload a JPEG, PNG or WebP picture.");
        }
        String name = UUID.randomUUID() + "." + ext;
        Path folder = properties.storageRoot().resolve("products");
        try {
            Files.createDirectories(folder);
            Files.write(folder.resolve(name), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        jdbc.sql("update products set image_url = :u, updated_at = now() where id = :id")
                .param("u", properties.mediaUrl() + "/products/" + name).param("id", id).update();
        return product(id);
    }

    public List<StockRow> stock(String q, boolean lowOnly) {
        Map<String, Object> params = new HashMap<>();
        params.put("low", properties.admin().lowStock());
        String where = "p.status <> 'ARCHIVED'";
        if (lowOnly) {
            where += " and k.stock <= :low";
        }
        if (q != null && !q.isBlank()) {
            where += " and (p.name ilike :q or k.sku ilike :q or p.local_name ilike :q)";
            params.put("q", "%" + q.trim() + "%");
        }
        return jdbc.sql("""
                        select k.id, p.id, p.name, p.image_url, k.label, k.sku, k.price, k.stock, k.max_per_order, p.status
                          from packs k join products p on p.id = k.product_id
                         where %s order by k.stock > :low, p.name, k.position limit 400""".formatted(where))
                .params(params)
                .query((rs, n) -> new StockRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getString(10)))
                .list();
    }

    @Transactional
    public StockRow setStock(long packId, int stock) {
        if (stock < 0 || stock > 100_000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.",
                    Map.of("stock", "Enter a number from 0 to 100000."));
        }
        int updated = jdbc.sql("update packs set stock = :s where id = :id").param("s", stock).param("id", packId).update();
        if (updated == 0) {
            throw ApiException.notFound("pack");
        }
        return jdbc.sql("""
                        select k.id, p.id, p.name, p.image_url, k.label, k.sku, k.price, k.stock, k.max_per_order, p.status
                          from packs k join products p on p.id = k.product_id where k.id = :id""")
                .param("id", packId)
                .query((rs, n) -> new StockRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getString(10)))
                .single();
    }

    static String imageType(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }

    static String slug(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

package com.haat.store.catalog;

import com.haat.store.catalog.CatalogViews.CategoryNode;
import com.haat.store.catalog.CatalogViews.CategoryRef;
import com.haat.store.catalog.CatalogViews.Home;
import com.haat.store.catalog.CatalogViews.Pack;
import com.haat.store.catalog.CatalogViews.ProductCard;
import com.haat.store.catalog.CatalogViews.ProductDetail;
import com.haat.store.shared.ApiException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Categories, product pages and the home page. */
@Service
public class CatalogService {

    private final JdbcClient jdbc;
    private final ProductSearch search;

    CatalogService(JdbcClient jdbc, ProductSearch search) {
        this.jdbc = jdbc;
        this.search = search;
    }

    private record CategoryRow(long id, Long parentId, String name, String slug, String icon, String color,
                               String image, int ownCount) {
    }

    public List<CategoryNode> categoryTree() {
        List<CategoryRow> rows = jdbc.sql("""
                        select c.id, c.parent_id, c.name, c.slug, c.icon, c.color, c.image_url,
                               (select count(*) from products p where p.category_id = c.id and p.status = 'ACTIVE')
                          from categories c order by c.position, c.name""")
                .query((rs, n) -> new CategoryRow(rs.getLong(1), (Long) rs.getObject(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getInt(8)))
                .list();
        Map<Long, List<CategoryRow>> byParent = new HashMap<>();
        for (CategoryRow row : rows) {
            byParent.computeIfAbsent(row.parentId() == null ? 0L : row.parentId(), k -> new ArrayList<>()).add(row);
        }
        return build(0L, byParent);
    }

    private List<CategoryNode> build(long parentId, Map<Long, List<CategoryRow>> byParent) {
        List<CategoryNode> nodes = new ArrayList<>();
        for (CategoryRow row : byParent.getOrDefault(parentId, List.of())) {
            List<CategoryNode> children = build(row.id(), byParent);
            int count = row.ownCount() + children.stream().mapToInt(CategoryNode::productCount).sum();
            nodes.add(new CategoryNode(row.id(), row.name(), row.slug(), row.icon(), row.color(), row.image(), count,
                    children));
        }
        return nodes;
    }

    public CategoryNode category(String slug) {
        return find(categoryTree(), slug).orElseThrow(() -> ApiException.notFound("category"));
    }

    private static Optional<CategoryNode> find(List<CategoryNode> nodes, String slug) {
        for (CategoryNode node : nodes) {
            if (node.slug().equals(slug)) {
                return Optional.of(node);
            }
            Optional<CategoryNode> inChild = find(node.children(), slug);
            if (inChild.isPresent()) {
                return inChild;
            }
        }
        return Optional.empty();
    }

    public ProductDetail product(String slug) {
        record Row(long id, String slug, String name, String localName, String brand, long categoryId,
                   String description, String storage, String origin, String image, boolean organic, boolean local,
                   boolean fresh, boolean weighed) {
        }
        Row p = jdbc.sql("""
                        select id, slug, name, local_name, brand, category_id, description, storage, origin, image_url,
                               organic, local, fresh, weighed
                          from products where slug = :slug and status = 'ACTIVE'""")
                .param("slug", slug)
                .query((rs, n) -> new Row(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getLong(6), rs.getString(7), rs.getString(8), rs.getString(9),
                        rs.getString(10), rs.getBoolean(11), rs.getBoolean(12), rs.getBoolean(13), rs.getBoolean(14)))
                .optional()
                .orElseThrow(() -> ApiException.notFound("product"));
        List<Pack> packs = search.packs(List.of(p.id())).getOrDefault(p.id(), List.of());
        return new ProductDetail(p.id(), p.slug(), p.name(), p.localName(), p.brand(), breadcrumb(p.categoryId()),
                p.description(), p.storage(), p.origin(), p.image(), p.organic(), p.local(), p.fresh(), p.weighed(),
                packs);
    }

    private List<CategoryRef> breadcrumb(long categoryId) {
        return jdbc.sql("""
                        with recursive up as (
                            select id, parent_id, name, slug, 0 as depth from categories where id = :id
                            union all
                            select c.id, c.parent_id, c.name, c.slug, up.depth + 1
                              from categories c join up on c.id = up.parent_id)
                        select name, slug from up order by depth desc""")
                .param("id", categoryId)
                .query((rs, n) -> new CategoryRef(rs.getString(1), rs.getString(2)))
                .list();
    }

    /** Same category first, then the rest of its parent category. */
    public List<ProductCard> related(String slug, int limit) {
        return search.cards("""
                        p.slug <> :slug and p.category_id in (
                            select c.id from categories c, products me join categories mc on mc.id = me.category_id
                             where me.slug = :slug
                               and (c.id = me.category_id or c.parent_id = coalesce(mc.parent_id, mc.id)))""",
                Map.of("slug", slug),
                "(p.category_id = (select category_id from products where slug = :slug)) desc, p.sold_count desc, p.id",
                limit);
    }

    public record SitemapEntry(String slug, java.time.Instant updatedAt) {
    }

    public record Sitemap(List<String> categories, List<SitemapEntry> products) {
    }

    public Sitemap sitemap() {
        List<String> categories = jdbc.sql("""
                        select slug from categories c
                         where exists (select 1 from products p where p.status = 'ACTIVE'
                                        and (p.category_id = c.id or p.category_id in (select id from categories where parent_id = c.id)))
                         order by position""")
                .query(String.class).list();
        List<SitemapEntry> products = jdbc.sql("select slug, updated_at from products where status = 'ACTIVE' order by id")
                .query((rs, n) -> new SitemapEntry(rs.getString(1), rs.getTimestamp(2).toInstant())).list();
        return new Sitemap(categories, products);
    }

    public Home home() {
        return new Home(
                categoryTree(),
                search.cards("mp.best_discount is not null", Map.of(), "mp.best_discount desc, p.sold_count desc, p.id", 12),
                search.cards("true", Map.of(), "p.sold_count desc, p.id", 12),
                search.cards("p.fresh", Map.of(), "p.featured desc, p.sold_count desc, p.id", 12),
                search.cards("p.category_id in (select c.id from categories c join categories top on top.id = c.parent_id where top.slug = 'pantry')",
                        Map.of(), "p.sold_count desc, p.id", 12));
    }
}

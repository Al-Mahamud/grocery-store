package com.haat.store.catalog;

import com.haat.store.catalog.CatalogViews.Facets;
import com.haat.store.catalog.CatalogViews.FacetValue;
import com.haat.store.catalog.CatalogViews.Pack;
import com.haat.store.catalog.CatalogViews.ProductCard;
import com.haat.store.catalog.CatalogViews.ProductPage;
import com.haat.store.catalog.CatalogViews.Suggestion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Finds active products for grids and the search box, with facet counts for the filters. */
@Service
public class ProductSearch {

    /** Every query reads products with their cheapest pack and best discount. */
    private static final String FROM = """
            products p
            cross join lateral (
                select min(k.price) as min_price,
                       max(case when k.compare_at_price is not null
                                then (k.compare_at_price - k.price) * 100 / k.compare_at_price end) as best_discount
                  from packs k where k.product_id = p.id) mp""";

    private final JdbcClient jdbc;

    ProductSearch(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public ProductPage search(ProductQuery query) {
        Where where = where(query, false);
        long total = jdbc.sql("select count(*) from " + FROM + " where " + where.sql).params(where.params)
                .query(Long.class).single();
        String orderBy = query.sort().orderBy;
        if (where.params.containsKey("tsq") && query.sort() == ProductQuery.Sort.RECOMMENDED) {
            // With search text, the best match comes first: names, then how well the text matches.
            orderBy = "(p.name ilike :like or p.local_name ilike :like) desc, "
                    + "ts_rank(p.search, to_tsquery('simple', :tsq)) desc, p.sold_count desc, p.id";
        }
        List<ProductCard> items = cards(where.sql, where.params, orderBy, query.pageSize(),
                (long) query.page() * query.pageSize());
        return new ProductPage(items, query.page(), query.pageSize(), total, facets(query));
    }

    public List<ProductCard> cards(String whereSql, Map<String, ?> params, String orderBy, int limit) {
        return cards("p.status = 'ACTIVE' and (" + whereSql + ")", params, orderBy, limit, 0);
    }

    private List<ProductCard> cards(String whereSql, Map<String, ?> params, String orderBy, int limit, long offset) {
        Map<String, Object> all = new HashMap<>(params);
        all.put("limit", limit);
        all.put("offset", offset);
        record Row(long id, String slug, String name, String localName, String brand, String category, String image,
                   boolean organic, boolean local, boolean fresh, boolean weighed) {
        }
        List<Row> rows = jdbc.sql("""
                        select p.id, p.slug, p.name, p.local_name, p.brand, c.name, p.image_url,
                               p.organic, p.local, p.fresh, p.weighed
                          from %s join categories c on c.id = p.category_id
                         where %s order by %s limit :limit offset :offset""".formatted(FROM, whereSql, orderBy))
                .params(all)
                .query((rs, n) -> new Row(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getBoolean(8), rs.getBoolean(9),
                        rs.getBoolean(10), rs.getBoolean(11)))
                .list();
        Map<Long, List<Pack>> packs = packs(rows.stream().map(Row::id).toList());
        return rows.stream().map(r -> {
            List<Pack> own = packs.getOrDefault(r.id(), List.of());
            return new ProductCard(r.id(), r.slug(), r.name(), r.localName(), r.brand(), r.category(), r.image(),
                    r.organic(), r.local(), r.fresh(), r.weighed(), own, own.stream().allMatch(k -> k.stock() == 0));
        }).toList();
    }

    /** Packs of several products at once, in display order. */
    public Map<Long, List<Pack>> packs(List<Long> productIds) {
        Map<Long, List<Pack>> byProduct = new LinkedHashMap<>();
        if (productIds.isEmpty()) {
            return byProduct;
        }
        jdbc.sql("""
                        select product_id, id, label, amount, unit, price, compare_at_price, stock, max_per_order
                          from packs where product_id in (:ids) order by position, price, id""")
                .param("ids", productIds)
                .query(rs -> {
                    byProduct.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                            .add(Packs.pack(rs.getLong(2), rs.getString(3), rs.getBigDecimal(4), rs.getString(5),
                                    rs.getInt(6), (Integer) rs.getObject(7), rs.getInt(8), rs.getInt(9)));
                });
        return byProduct;
    }

    /** Up to six quick matches for the search box: the product's cheapest pack. */
    public List<Suggestion> suggest(String q) {
        String tsQuery = tsQuery(q);
        if (tsQuery == null) {
            return List.of();
        }
        return jdbc.sql("""
                        select p.slug, p.name, p.local_name, p.image_url, k.price, k.label
                          from products p
                          join lateral (select price, label from packs where product_id = p.id
                                         order by position, price limit 1) k on true
                         where p.status = 'ACTIVE'
                           and (p.search @@ to_tsquery('simple', :tsq) or p.name ilike :like or p.local_name ilike :like)
                         order by (p.name ilike :prefix) desc, p.sold_count desc
                         limit 6""")
                .param("tsq", tsQuery).param("like", like(q)).param("prefix", escape(q.trim()) + "%")
                .query((rs, n) -> new Suggestion(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), rs.getString(6)))
                .list();
    }

    private Facets facets(ProductQuery query) {
        Where forBrands = where(query, true);
        List<FacetValue> brands = jdbc.sql("select p.brand, count(*) from " + FROM + " where " + forBrands.sql
                        + " and p.brand is not null group by p.brand order by count(*) desc, p.brand limit 15")
                .params(forBrands.params)
                .query((rs, n) -> new FacetValue(rs.getString(1), rs.getString(1), rs.getInt(2),
                        query.brands().contains(rs.getString(1))))
                .list();
        Where all = where(query, false);
        int[] counts = jdbc.sql("select count(*) filter (where p.organic), count(*) filter (where p.local), "
                        + "count(*) filter (where mp.best_discount is not null) from " + FROM + " where "
                        + where(new ProductQuery(query.category(), query.q(), query.brands(), false, false, false,
                                query.sort(), 0, 1), false).sql)
                .params(all.params)
                .query((rs, n) -> new int[] {rs.getInt(1), rs.getInt(2), rs.getInt(3)})
                .single();
        return new Facets(brands, counts[0], counts[1], counts[2]);
    }

    private record Where(String sql, Map<String, Object> params) {
    }

    private Where where(ProductQuery query, boolean leaveOutBrand) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        conditions.add("p.status = 'ACTIVE'");
        if (query.category() != null) {
            conditions.add("""
                    p.category_id in (
                        with recursive tree as (
                            select id from categories where slug = :category
                            union all
                            select c.id from categories c join tree on c.parent_id = tree.id)
                        select id from tree)""");
            params.put("category", query.category());
        }
        String tsQuery = tsQuery(query.q());
        if (tsQuery != null) {
            conditions.add("p.search @@ to_tsquery('simple', :tsq) or p.name ilike :like or p.local_name ilike :like or p.brand ilike :like");
            params.put("tsq", tsQuery);
            params.put("like", like(query.q()));
        }
        if (!leaveOutBrand && !query.brands().isEmpty()) {
            conditions.add("p.brand in (:brands)");
            params.put("brands", query.brands());
        }
        if (query.organic()) {
            conditions.add("p.organic");
        }
        if (query.local()) {
            conditions.add("p.local");
        }
        if (query.deal()) {
            conditions.add("mp.best_discount is not null");
        }
        return new Where(conditions.stream().map(c -> "(" + c + ")").collect(Collectors.joining(" and ")), params);
    }

    private static String like(String q) {
        return "%" + escape(q.trim()) + "%";
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** "tom ket" becomes {@code tom:* & ket:*}, so partly typed words still match. */
    static String tsQuery(String q) {
        if (q == null) {
            return null;
        }
        List<String> terms = Arrays.stream(q.toLowerCase().split("[^\\p{L}\\p{N}\\p{M}]+"))
                .filter(t -> !t.isBlank()).limit(8).map(t -> t + ":*").toList();
        return terms.isEmpty() ? null : String.join(" & ", terms);
    }
}

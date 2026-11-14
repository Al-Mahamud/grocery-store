package com.haat.store.admin;

import com.haat.store.shared.ApiException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Categories as the shop edits them: two levels (aisles and shelves). A category's web address
 * (slug) is fixed when it is made, so links and search results keep working after a rename.
 */
@Service
public class AdminCategories {

    /** The icon names the web app can draw. */
    public static final Set<String> ICONS = Set.of("leaf", "carrot", "apple", "fish", "wheat", "droplet", "bean",
            "flame", "milk", "egg", "croissant", "cookie", "coffee", "cup-soda", "sparkles", "spray-can", "bath", "basket");

    public record Input(String name, Long parentId, String icon, String color) {
    }

    public record Node(long id, Long parentId, String name, String slug, String icon, String color, int position,
                       int products, List<Node> children) {
    }

    private final JdbcClient jdbc;

    AdminCategories(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Node> tree() {
        record Row(long id, Long parentId, String name, String slug, String icon, String color, int position, int products) {
        }
        List<Row> rows = jdbc.sql("""
                        select c.id, c.parent_id, c.name, c.slug, c.icon, c.color, c.position,
                               (select count(*) from products p where p.category_id = c.id and p.status <> 'ARCHIVED')
                          from categories c order by c.position, c.id""")
                .query((rs, n) -> new Row(rs.getLong(1), (Long) rs.getObject(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getInt(7), rs.getInt(8)))
                .list();
        List<Node> top = new ArrayList<>();
        for (Row r : rows) {
            if (r.parentId() == null) {
                List<Node> children = rows.stream().filter(c -> r.id() == (c.parentId() == null ? -1 : c.parentId()))
                        .map(c -> new Node(c.id(), c.parentId(), c.name(), c.slug(), c.icon(), c.color(), c.position(),
                                c.products(), List.of()))
                        .toList();
                top.add(new Node(r.id(), null, r.name(), r.slug(), r.icon(), r.color(), r.position(),
                        r.products() + children.stream().mapToInt(Node::products).sum(), children));
            }
        }
        return top;
    }

    @Transactional
    public List<Node> create(Input in) {
        String name = check(in, null);
        String slug = AdminCatalog.slug(name);
        int position = jdbc.sql("select coalesce(max(position), 0) + 1 from categories").query(Integer.class).single();
        try {
            jdbc.sql("""
                            insert into categories (parent_id, name, slug, icon, color, position)
                            values (:parent, :name, :slug, :icon, :color, :position)""")
                    .param("parent", in.parentId()).param("name", name).param("slug", slug)
                    .param("icon", in.icon()).param("color", in.color().toUpperCase()).param("position", position).update();
        } catch (DuplicateKeyException e) {
            throw invalid("name", "Another category already has this name.");
        }
        return tree();
    }

    @Transactional
    public List<Node> update(long id, Input in) {
        String name = check(in, id);
        if (jdbc.sql("update categories set name = :name, icon = :icon, color = :color where id = :id")
                .param("name", name).param("icon", in.icon()).param("color", in.color().toUpperCase()).param("id", id)
                .update() == 0) {
            throw ApiException.notFound("category");
        }
        return tree();
    }

    /** Moves a category one place up (-1) or down (+1) among its siblings. */
    @Transactional
    public List<Node> move(long id, int direction) {
        if (jdbc.sql("select count(*) from categories where id = :id").param("id", id).query(Integer.class).single() == 0) {
            throw ApiException.notFound("category");
        }
        Long parent = jdbc.sql("select parent_id from categories where id = :id").param("id", id)
                .query((rs, n) -> (Long) rs.getObject(1)).list().get(0);
        List<Long> siblings = new ArrayList<>(jdbc.sql("""
                        select id from categories where parent_id is not distinct from cast(:p as bigint) order by position, id""")
                .param("p", parent).query(Long.class).list());
        int at = siblings.indexOf(id);
        int to = at + Integer.signum(direction);
        if (to >= 0 && to < siblings.size()) {
            siblings.remove(at);
            siblings.add(to, id);
            // Siblings keep their place among the other categories' numbers.
            int base = jdbc.sql("select coalesce(min(position), 0) from categories where id in (:ids)")
                    .param("ids", siblings).query(Integer.class).single();
            for (int i = 0; i < siblings.size(); i++) {
                jdbc.sql("update categories set position = :p where id = :id").param("p", base + i).param("id", siblings.get(i)).update();
            }
        }
        return tree();
    }

    /** Only empty categories can go: no products (any status) and no shelves under them. */
    @Transactional
    public List<Node> delete(long id) {
        int products = jdbc.sql("select count(*) from products where category_id = :id").param("id", id).query(Integer.class).single();
        int children = jdbc.sql("select count(*) from categories where parent_id = :id").param("id", id).query(Integer.class).single();
        if (products > 0 || children > 0) {
            throw ApiException.conflict("category.not_empty", products > 0
                    ? "Move or archive its " + products + " product" + (products == 1 ? "" : "s") + " first. Archived products count too."
                    : "Remove the categories inside it first.");
        }
        if (jdbc.sql("delete from categories where id = :id").param("id", id).update() == 0) {
            throw ApiException.notFound("category");
        }
        return tree();
    }

    private String check(Input in, Long id) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = in.name() == null ? "" : in.name().trim();
        if (name.length() < 2 || name.length() > 40) {
            errors.put("name", "Use 2 to 40 characters.");
        } else if (AdminCatalog.slug(name).isEmpty()) {
            errors.put("name", "Use English letters too, so the category gets a web address.");
        }
        if (in.icon() == null || !ICONS.contains(in.icon())) {
            errors.put("icon", "Choose an icon.");
        }
        if (in.color() == null || !in.color().matches("#[0-9A-Fa-f]{6}")) {
            errors.put("color", "Choose a colour.");
        }
        if (id == null && in.parentId() != null) {
            // A row whose parent_id is null maps to null, which optional() treats as "no row", so ask directly.
            Boolean topLevel = jdbc.sql("select parent_id is null from categories where id = :id").param("id", in.parentId())
                    .query(Boolean.class).optional().orElse(null);
            if (topLevel == null) {
                errors.put("parentId", "That category doesn't exist.");
            } else if (!topLevel) {
                errors.put("parentId", "Categories go two levels deep at most.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.", errors);
        }
        return name;
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException(HttpStatus.CONFLICT, "category.taken", "Please check the highlighted fields.", Map.of(field, message));
    }
}

package com.haat.store.admin;

import com.haat.store.shared.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coupons and delivery areas. */
@Service
public class AdminSettings {

    public record CouponInput(String code, String description, String kind, Integer value, Integer maxDiscount,
                              Integer minSubtotal, Instant startsAt, Instant endsAt, Integer usageLimit, Boolean active) {
    }

    public record CouponView(long id, String code, String description, String kind, int value, Integer maxDiscount,
                             int minSubtotal, Instant startsAt, Instant endsAt, Integer usageLimit, int usedCount,
                             boolean active) {
    }

    public record AreaView(long id, String name, String slug, boolean active, int orders) {
    }

    public record Text(long id, String phone, String kind, String body, String status, int attempts, String provider,
                       String error, String orderNumber, Instant createdAt, Instant sentAt) {
    }

    public record TextPage(List<Text> items, int page, int pageSize, long total, Map<String, Integer> counts) {
    }

    private final JdbcClient jdbc;

    AdminSettings(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<CouponView> coupons() {
        return jdbc.sql("""
                        select id, code, description, kind, value, max_discount, min_subtotal, starts_at, ends_at,
                               usage_limit, used_count, active
                          from coupons order by active desc, created_at desc""")
                .query((rs, n) -> new CouponView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), (Integer) rs.getObject(6), rs.getInt(7), instant(rs.getTimestamp(8)),
                        instant(rs.getTimestamp(9)), (Integer) rs.getObject(10), rs.getInt(11), rs.getBoolean(12)))
                .list();
    }

    @Transactional
    public CouponView saveCoupon(Long id, CouponInput in) {
        Map<String, String> errors = new LinkedHashMap<>();
        String code = in.code() == null ? "" : in.code().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9]{3,20}")) {
            errors.put("code", "3 to 20 letters and numbers, e.g. EID200.");
        }
        String kind = in.kind() == null ? "" : in.kind();
        int value = in.value() == null ? 0 : in.value();
        switch (kind) {
            case "PERCENT" -> {
                if (value < 1 || value > 90) {
                    errors.put("value", "A percentage from 1 to 90.");
                }
            }
            case "FIXED" -> {
                if (value < 1) {
                    errors.put("value", "Enter the amount in taka.");
                }
            }
            case "FREE_SHIPPING" -> value = 0;
            default -> errors.put("kind", "Choose what the coupon gives.");
        }
        if (in.maxDiscount() != null && in.maxDiscount() < 1) {
            errors.put("maxDiscount", "Leave empty or enter an amount above ৳0.");
        }
        if (in.minSubtotal() != null && in.minSubtotal() < 0) {
            errors.put("minSubtotal", "Can't be below ৳0.");
        }
        if (in.usageLimit() != null && in.usageLimit() < 1) {
            errors.put("usageLimit", "Leave empty for no limit, or enter 1 or more.");
        }
        if (in.startsAt() != null && in.endsAt() != null && !in.endsAt().isAfter(in.startsAt())) {
            errors.put("endsAt", "The end must be after the start.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.", errors);
        }
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("code", code);
        f.put("description", in.description() == null ? "" : in.description().trim());
        f.put("kind", kind);
        f.put("value", value);
        f.put("max", kind.equals("PERCENT") ? in.maxDiscount() : null);
        f.put("min", in.minSubtotal() == null ? 0 : in.minSubtotal());
        f.put("starts", in.startsAt() == null ? null : Timestamp.from(in.startsAt()));
        f.put("ends", in.endsAt() == null ? null : Timestamp.from(in.endsAt()));
        f.put("limit", in.usageLimit());
        f.put("active", in.active() == null || in.active());
        try {
            if (id == null) {
                id = jdbc.sql("""
                                insert into coupons (code, description, kind, value, max_discount, min_subtotal, starts_at,
                                                     ends_at, usage_limit, active)
                                values (:code, :description, :kind, :value, :max, :min, :starts, :ends, :limit, :active)
                                returning id""")
                        .params(f).query(Long.class).single();
            } else {
                f.put("id", id);
                if (jdbc.sql("""
                                update coupons set code = :code, description = :description, kind = :kind, value = :value,
                                       max_discount = :max, min_subtotal = :min, starts_at = :starts, ends_at = :ends,
                                       usage_limit = :limit, active = :active
                                 where id = :id""").params(f).update() == 0) {
                    throw ApiException.notFound("coupon");
                }
            }
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "coupon.code_taken", "Please check the highlighted fields.",
                    Map.of("code", "Another coupon already uses this code."));
        }
        long found = id;
        return coupons().stream().filter(c -> c.id() == found).findFirst().orElseThrow();
    }

    public List<AreaView> areas() {
        return jdbc.sql("""
                        select a.id, a.name, a.slug, a.active, (select count(*) from orders o where o.area_id = a.id)
                          from delivery_areas a order by a.active desc, a.position, a.name""")
                .query((rs, n) -> new AreaView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getInt(5)))
                .list();
    }

    /** Areas are never deleted (old orders point at them); switching one off stops new deliveries there. */
    @Transactional
    public AreaView saveArea(Long id, String name, boolean active) {
        String clean = name == null ? "" : name.trim();
        if (clean.length() < 2 || clean.length() > 60) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.",
                    Map.of("name", "Enter the area name."));
        }
        String slug = AdminCatalog.slug(clean);
        try {
            if (id == null) {
                int position = jdbc.sql("select coalesce(max(position), 0) + 1 from delivery_areas").query(Integer.class).single();
                id = jdbc.sql("insert into delivery_areas (name, slug, active, position) values (:n, :s, :a, :p) returning id")
                        .param("n", clean).param("s", slug).param("a", active).param("p", position)
                        .query(Long.class).single();
            } else if (jdbc.sql("update delivery_areas set name = :n, slug = :s, active = :a where id = :id")
                    .param("n", clean).param("s", slug).param("a", active).param("id", id).update() == 0) {
                throw ApiException.notFound("area");
            }
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "area.taken", "Please check the highlighted fields.",
                    Map.of("name", "That area is already on the list."));
        }
        long found = id;
        return areas().stream().filter(a -> a.id() == found).findFirst().orElseThrow();
    }

    /** Texts sent or waiting, newest first. Codes show masked once sent. */
    public TextPage texts(String status, String phone, int page) {
        String where = "true";
        Map<String, Object> params = new LinkedHashMap<>();
        if (status != null && List.of("QUEUED", "SENDING", "SENT", "FAILED").contains(status)) {
            where += " and m.status = :status";
            params.put("status", status);
        }
        if (phone != null && !phone.isBlank()) {
            where += " and m.phone like :phone";
            params.put("phone", "%" + phone.trim() + "%");
        }
        int size = 50;
        long total = jdbc.sql("select count(*) from sms_messages m where " + where).params(params).query(Long.class).single();
        Map<String, Object> paged = new LinkedHashMap<>(params);
        paged.put("limit", size);
        paged.put("offset", (long) page * size);
        List<Text> items = jdbc.sql("""
                        select m.id, m.phone, m.kind,
                               case when m.sensitive and m.status <> 'SENT' then regexp_replace(m.body, '[0-9]{6}', '••••••', 'g')
                                    else m.body end,
                               m.status, m.attempts, m.provider, m.error, o.number, m.created_at, m.sent_at
                          from sms_messages m left join orders o on o.id = m.order_id
                         where %s order by m.created_at desc, m.id desc limit :limit offset :offset""".formatted(where))
                .params(paged)
                .query((rs, n) -> new Text(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getInt(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getTimestamp(10).toInstant(),
                        instant(rs.getTimestamp(11))))
                .list();
        Map<String, Integer> counts = new LinkedHashMap<>();
        jdbc.sql("""
                        select status, count(*) from sms_messages where created_at > now() - interval '24 hours'
                         group by status""")
                .query(rs -> {
                    counts.put(rs.getString(1), rs.getInt(2));
                });
        return new TextPage(items, page, size, total, counts);
    }

    /** Sends a failed text again. */
    @Transactional
    public void retryText(long id) {
        if (jdbc.sql("update sms_messages set status = 'QUEUED', attempts = 0, error = null where id = :id and status = 'FAILED'")
                .param("id", id).update() == 0) {
            throw ApiException.conflict("text.not_failed", "Only failed texts can be sent again.");
        }
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}

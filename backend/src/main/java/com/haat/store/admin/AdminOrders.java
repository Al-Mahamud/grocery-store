package com.haat.store.admin;

import com.haat.store.delivery.DeliveryService;
import com.haat.store.order.OrderService;
import com.haat.store.order.OrderService.OrderView;
import com.haat.store.order.OrderStock;
import com.haat.store.shared.ApiException;
import com.haat.store.shared.StoreProperties;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orders as the shop sees them: the queue, status changes, cancellations, refunds and picking. */
@Service
public class AdminOrders {

    /** What the next steps of an order can be. Cash orders are marked paid when delivered. */
    static final Map<String, List<String>> NEXT = Map.of(
            "PENDING_PAYMENT", List.of("CANCELLED"),
            "PLACED", List.of("PACKING", "CANCELLED"),
            "PACKING", List.of("OUT_FOR_DELIVERY", "CANCELLED"),
            "OUT_FOR_DELIVERY", List.of("DELIVERED", "CANCELLED"),
            "DELIVERED", List.of(),
            "CANCELLED", List.of());

    static final Set<String> FILTERS = Set.of("OPEN", "ALL", "REFUND_DUE", "PENDING_PAYMENT", "PLACED", "PACKING",
            "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED");

    public record Row(String number, String status, String paymentMethod, String paymentStatus, Instant placedAt,
                      String name, String phone, String area, OrderService.Slot slot, int itemCount, int total) {
    }

    public record Page(List<Row> items, int page, int pageSize, long total, Map<String, Integer> counts) {
    }

    public record Attempt(String tranId, int amount, String status, String cardType, String bankTranId, Instant at) {
    }

    public record Detail(OrderView order, List<String> next, List<Attempt> attempts, Instant refundedAt,
                         String refundReference, boolean refundDue, Long customerId, int customerOrders) {
    }

    public record PickOrder(String number, String status, String name, String phone, String area, String address,
                            String note, String substitution, String paymentMethod, String paymentStatus, int total,
                            List<PickItem> items) {
    }

    public record PickItem(String name, String packLabel, int quantity) {
    }

    public record PickLine(String category, String name, String packLabel, String sku, String image, int quantity,
                           List<String> orders) {
    }

    public record Picking(LocalDate date, LocalTime start, LocalTime end, List<PickOrder> orders, List<PickLine> lines) {
    }

    public record DayTotal(LocalDate date, int orders, int revenue) {
    }

    public record Dashboard(int ordersToday, int revenueToday, int averageBasket, Map<String, Integer> open,
                            int refundsDue, int lowStock, List<DayTotal> week, List<DeliveryService.BoardDay> slots,
                            List<Row> recent) {
    }

    private static final String ROW_SELECT = """
            select o.number, o.status, o.payment_method, o.payment_status, o.created_at, o.name, o.phone, o.area_name,
                   o.slot_date, o.slot_start, o.slot_end,
                   (select coalesce(sum(quantity), 0) from order_items where order_id = o.id), o.total
              from orders o""";

    private final JdbcClient jdbc;
    private final OrderService orders;
    private final OrderStock stock;
    private final DeliveryService delivery;
    private final StoreProperties properties;
    private final Clock clock;

    AdminOrders(JdbcClient jdbc, OrderService orders, OrderStock stock, DeliveryService delivery,
                StoreProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.stock = stock;
        this.delivery = delivery;
        this.properties = properties;
        this.clock = clock;
    }

    public Page list(String filter, LocalDate slotDate, String q, int page) {
        String f = filter == null || !FILTERS.contains(filter) ? "OPEN" : filter;
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        switch (f) {
            case "ALL" -> where.add("true");
            case "OPEN" -> where.add("o.status in ('PLACED', 'PACKING', 'OUT_FOR_DELIVERY')");
            case "REFUND_DUE" -> where.add("o.status = 'CANCELLED' and o.payment_status = 'PAID'");
            default -> {
                where.add("o.status = :status");
                params.put("status", f);
            }
        }
        if (slotDate != null) {
            where.add("o.slot_date = :date");
            params.put("date", Date.valueOf(slotDate));
        }
        if (q != null && !q.isBlank()) {
            where.add("(o.number ilike :q or o.phone like :q or o.name ilike :q)");
            params.put("q", "%" + q.trim() + "%");
        }
        String condition = String.join(" and ", where);
        // Open orders in delivery order; everything else newest first.
        String order = f.equals("OPEN") || f.equals("PLACED") || f.equals("PACKING") || f.equals("OUT_FOR_DELIVERY")
                ? "o.slot_date, o.slot_start, o.created_at" : "o.created_at desc";
        int size = 25;
        long total = jdbc.sql("select count(*) from orders o where " + condition).params(params).query(Long.class).single();
        Map<String, Object> paged = new HashMap<>(params);
        paged.put("limit", size);
        paged.put("offset", (long) page * size);
        List<Row> items = jdbc.sql(ROW_SELECT + " where " + condition + " order by " + order + " limit :limit offset :offset")
                .params(paged).query((rs, n) -> row(rs)).list();
        return new Page(items, page, size, total, counts());
    }

    private Map<String, Integer> counts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : List.of("PENDING_PAYMENT", "PLACED", "PACKING", "OUT_FOR_DELIVERY")) {
            counts.put(s, 0);
        }
        jdbc.sql("""
                        select status, count(*) from orders
                         where status in ('PENDING_PAYMENT', 'PLACED', 'PACKING', 'OUT_FOR_DELIVERY') group by status""")
                .query(rs -> {
                    counts.put(rs.getString(1), rs.getInt(2));
                });
        counts.put("REFUND_DUE", refundsDue());
        return counts;
    }

    public Detail detail(String number) {
        long id = idOf(number);
        OrderView view = orders.byId(id).orElseThrow();
        List<Attempt> attempts = jdbc.sql("""
                        select tran_id, amount, status, card_type, bank_tran_id, created_at
                          from payment_attempts where order_id = :o order by created_at, id""")
                .param("o", id)
                .query((rs, n) -> new Attempt(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getTimestamp(6).toInstant()))
                .list();
        record Extra(Instant refundedAt, String reference, Long customerId) {
        }
        Extra extra = jdbc.sql("select refunded_at, refund_reference, customer_id from orders where id = :id").param("id", id)
                .query((rs, n) -> new Extra(rs.getTimestamp(1) == null ? null : rs.getTimestamp(1).toInstant(),
                        rs.getString(2), (Long) rs.getObject(3)))
                .single();
        int customerOrders = jdbc.sql("""
                        select count(*) from orders
                         where (customer_id = :c or phone = :p) and status <> 'CANCELLED'""")
                .param("c", extra.customerId()).param("p", view.phone()).query(Integer.class).single();
        boolean refundDue = view.status().equals("CANCELLED") && view.paymentStatus().equals("PAID");
        return new Detail(view, NEXT.getOrDefault(view.status(), List.of()), attempts, extra.refundedAt(),
                extra.reference(), refundDue, extra.customerId(), customerOrders);
    }

    /**
     * Moves an order on. Cancelling gives back stock (unless the goods can't be resold), and a paid
     * order then shows as a refund due. A cash order is marked paid when it is delivered.
     */
    @Transactional
    public void changeStatus(String number, String target, String note, boolean restock, String actor) {
        long id = lock(number);
        String current = jdbc.sql("select status from orders where id = :id").param("id", id).query(String.class).single();
        if (!NEXT.getOrDefault(current, List.of()).contains(target)) {
            throw ApiException.conflict("order.status", "An order that is " + human(current)
                    + " can't be marked " + human(target) + ".");
        }
        jdbc.sql("update orders set status = :s, payment_expires_at = null, updated_at = now() where id = :id")
                .param("s", target).param("id", id).update();
        List<String> notes = new ArrayList<>();
        if (note != null && !note.isBlank()) {
            notes.add(note.trim());
        }
        if (target.equals("CANCELLED")) {
            jdbc.sql("update payment_attempts set status = 'CANCELLED', updated_at = now() where order_id = :o and status = 'PENDING'")
                    .param("o", id).update();
            if (restock) {
                stock.release(id);
            } else {
                notes.add("Items not returned to stock");
            }
            Integer paid = jdbc.sql("select total from orders where id = :id and payment_status = 'PAID'").param("id", id)
                    .query(Integer.class).optional().orElse(null);
            if (paid != null) {
                notes.add("Refund ৳" + paid + " due");
            }
        }
        if (target.equals("DELIVERED")) {
            int collected = jdbc.sql("""
                            update orders set payment_status = 'PAID', paid_at = now()
                             where id = :id and payment_method = 'COD' and payment_status = 'UNPAID'""")
                    .param("id", id).update();
            if (collected > 0) {
                notes.add("Cash collected");
            }
        }
        event(id, target, notes.isEmpty() ? null : String.join(". ", notes), actor);
    }

    /** Records that a payment was refunded (in the SSLCommerz merchant panel), with its reference. */
    @Transactional
    public void refund(String number, String reference, String actor) {
        long id = lock(number);
        int updated = jdbc.sql("""
                        update orders set payment_status = 'REFUNDED', refunded_at = now(), refund_reference = :r,
                                          updated_at = now()
                         where id = :id and status = 'CANCELLED' and payment_status = 'PAID'""")
                .param("r", reference).param("id", id).update();
        if (updated == 0) {
            throw ApiException.conflict("order.refund", "Only cancelled orders that were paid can be refunded.");
        }
        event(id, "CANCELLED", "Refunded (" + reference + ")", actor);
    }

    @Transactional
    public void addNote(String number, String note, String actor) {
        long id = idOf(number);
        String status = jdbc.sql("select status from orders where id = :id").param("id", id).query(String.class).single();
        event(id, status, note.trim(), actor);
    }

    /** Everything to pack for one slot: order by order, and added up per pack, aisle by aisle. */
    public Picking picking(LocalDate date, LocalTime start) {
        List<Long> ids = jdbc.sql("""
                        select id from orders where slot_date = :d and slot_start = :s and status in ('PLACED', 'PACKING')
                         order by created_at""")
                .param("d", Date.valueOf(date)).param("s", Time.valueOf(start)).query(Long.class).list();
        List<PickOrder> pickOrders = new ArrayList<>();
        for (long id : ids) {
            List<PickItem> items = jdbc.sql("select product_name, pack_label, quantity from order_items where order_id = :o order by id")
                    .param("o", id).query((rs, n) -> new PickItem(rs.getString(1), rs.getString(2), rs.getInt(3))).list();
            pickOrders.add(jdbc.sql("""
                            select number, status, name, phone, area_name, address, note, substitution, payment_method,
                                   payment_status, total from orders where id = :id""")
                    .param("id", id)
                    .query((rs, n) -> new PickOrder(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                            rs.getString(10), rs.getInt(11), items))
                    .single());
        }
        List<PickLine> lines = ids.isEmpty() ? List.of() : jdbc.sql("""
                        select coalesce(c.name, 'Other'), i.product_name, i.pack_label, i.sku, max(i.image_url),
                               sum(i.quantity),
                               array_agg(o.number || ' ×' || i.quantity order by o.created_at)
                          from order_items i
                          join orders o on o.id = i.order_id
                          left join products p on p.id = i.product_id
                          left join categories c on c.id = p.category_id
                         where i.order_id in (:ids)
                         group by c.name, c.position, i.product_name, i.pack_label, i.sku
                         order by c.position nulls last, i.product_name, i.pack_label""")
                .param("ids", ids)
                .query((rs, n) -> new PickLine(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getInt(6), List.of((String[]) rs.getArray(7).getArray())))
                .list();
        return new Picking(date, start, start.plusHours(properties.slots().lengthHours()), pickOrders, lines);
    }

    /** Moves every placed order of a slot to packing in one go. Returns how many moved. */
    @Transactional
    public int startPacking(LocalDate date, LocalTime start, String actor) {
        List<Long> ids = jdbc.sql("""
                        select id from orders where slot_date = :d and slot_start = :s and status = 'PLACED' for update""")
                .param("d", Date.valueOf(date)).param("s", Time.valueOf(start)).query(Long.class).list();
        for (long id : ids) {
            jdbc.sql("update orders set status = 'PACKING', updated_at = now() where id = :id").param("id", id).update();
            event(id, "PACKING", null, actor);
        }
        return ids.size();
    }

    public Dashboard dashboard() {
        ZoneId zone = properties.slots().zone();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Timestamp from = Timestamp.from(today.minusDays(6).atStartOfDay(zone).toInstant());
        Map<LocalDate, DayTotal> byDay = new LinkedHashMap<>();
        for (int d = 6; d >= 0; d--) {
            byDay.put(today.minusDays(d), new DayTotal(today.minusDays(d), 0, 0));
        }
        jdbc.sql("""
                        select (created_at at time zone :zone)::date, count(*), coalesce(sum(total), 0)
                          from orders where created_at >= :from and status not in ('PENDING_PAYMENT', 'CANCELLED')
                         group by 1""")
                .param("zone", zone.getId()).param("from", from)
                .query(rs -> {
                    LocalDate day = rs.getDate(1).toLocalDate();
                    byDay.computeIfPresent(day, (k, v) -> new DayTotal(k, safeInt(rs, 2), safeInt(rs, 3)));
                });
        DayTotal now = byDay.get(today);
        Map<String, Integer> open = counts();
        open.remove("REFUND_DUE");
        int low = jdbc.sql("select count(*) from packs k join products p on p.id = k.product_id where p.status = 'ACTIVE' and k.stock <= :low")
                .param("low", properties.admin().lowStock()).query(Integer.class).single();
        List<Row> recent = jdbc.sql(ROW_SELECT + " order by o.created_at desc limit 8").query((rs, n) -> row(rs)).list();
        return new Dashboard(now.orders(), now.revenue(), now.orders() == 0 ? 0 : now.revenue() / now.orders(), open,
                refundsDue(), low, List.copyOf(byDay.values()), delivery.board(2), recent);
    }

    private int refundsDue() {
        return jdbc.sql("select count(*) from orders where status = 'CANCELLED' and payment_status = 'PAID'")
                .query(Integer.class).single();
    }

    private static int safeInt(java.sql.ResultSet rs, int column) {
        try {
            return rs.getInt(column);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private long idOf(String number) {
        return jdbc.sql("select id from orders where number = :n").param("n", number == null ? "" : number.trim().toUpperCase())
                .query(Long.class).optional().orElseThrow(() -> ApiException.notFound("order"));
    }

    private long lock(String number) {
        long id = idOf(number);
        jdbc.sql("select id from orders where id = :id for update").param("id", id).query(Long.class).single();
        return id;
    }

    private void event(long orderId, String status, String note, String actor) {
        jdbc.sql("insert into order_events (order_id, status, note, actor) values (:o, :s, :n, :a)")
                .param("o", orderId).param("s", status).param("n", note).param("a", actor).update();
    }

    private static Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Row(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5).toInstant(),
                rs.getString(6), rs.getString(7), rs.getString(8),
                new OrderService.Slot(rs.getDate(9).toLocalDate(), rs.getTime(10).toLocalTime(), rs.getTime(11).toLocalTime()),
                rs.getInt(12), rs.getInt(13));
    }

    private static String human(String status) {
        return switch (status) {
            case "PENDING_PAYMENT" -> "awaiting payment";
            case "PLACED" -> "placed";
            case "PACKING" -> "packing";
            case "OUT_FOR_DELIVERY" -> "out for delivery";
            case "DELIVERED" -> "delivered";
            case "CANCELLED" -> "cancelled";
            default -> status.toLowerCase();
        };
    }
}

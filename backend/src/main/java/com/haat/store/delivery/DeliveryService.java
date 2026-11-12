package com.haat.store.delivery;

import com.haat.store.shared.ApiException;
import com.haat.store.shared.StoreProperties;
import java.sql.Date;
import java.sql.Time;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Where we deliver and when: served areas and two-hour slots with limited capacity. */
@Service
public class DeliveryService {

    public record Area(long id, String name, String slug) {
    }

    public record Slot(LocalTime start, LocalTime end, String label, boolean available, int remaining) {
    }

    public record Day(LocalDate date, String label, List<Slot> slots) {
    }

    /** The admin's view of a slot: how full it is, and whether its capacity was changed for this day. */
    public record BoardSlot(LocalTime start, LocalTime end, String label, int booked, int capacity, boolean custom,
                            String note, boolean past) {
    }

    public record BoardDay(LocalDate date, String label, List<BoardSlot> slots) {
    }

    private record Override(int capacity, String note) {
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private final JdbcClient jdbc;
    private final StoreProperties.Slots settings;
    private final Clock clock;

    DeliveryService(JdbcClient jdbc, StoreProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.settings = properties.slots();
        this.clock = clock;
    }

    public List<Area> areas() {
        return jdbc.sql("select id, name, slug from delivery_areas where active order by position, name")
                .query((rs, n) -> new Area(rs.getLong(1), rs.getString(2), rs.getString(3))).list();
    }

    public Area area(long id) {
        return jdbc.sql("select id, name, slug from delivery_areas where id = :id and active").param("id", id)
                .query((rs, n) -> new Area(rs.getLong(1), rs.getString(2), rs.getString(3))).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "delivery.area",
                        "We don't deliver to that area yet.", Map.of("areaId", "Choose an area from the list.")));
    }

    /** Today and the next days, each with every slot and whether it can still be booked. */
    public List<Day> days() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(settings.zone()));
        LocalDate today = now.toLocalDate();
        Map<String, Integer> booked = booked(today, today.plusDays(settings.daysAhead() - 1L));
        Map<String, Override> overrides = overrides(today, today.plusDays(settings.daysAhead() - 1L));
        List<Day> days = new ArrayList<>();
        for (int d = 0; d < settings.daysAhead(); d++) {
            LocalDate date = today.plusDays(d);
            List<Slot> slots = new ArrayList<>();
            for (int hour : settings.starts()) {
                LocalTime start = LocalTime.of(hour, 0);
                LocalTime end = start.plusHours(settings.lengthHours());
                Override custom = overrides.get(key(date, start));
                int capacity = custom == null ? settings.capacity() : custom.capacity();
                int remaining = Math.max(0, capacity - booked.getOrDefault(key(date, start), 0));
                boolean open = date.atTime(start).atZone(settings.zone()).minus(settings.cutoff()).isAfter(now);
                slots.add(new Slot(start, end, label(start, end), open && remaining > 0, open ? remaining : 0));
            }
            String name = d == 0 ? "Today" : d == 1 ? "Tomorrow" : date.format(DAY);
            days.add(new Day(date, name, slots));
        }
        return days;
    }

    /**
     * Checks a slot can take one more order. Call inside the order's transaction: the advisory lock
     * makes two shoppers queue for the last place instead of both getting it.
     */
    public Slot reserve(LocalDate date, LocalTime start) {
        jdbc.sql("select pg_advisory_xact_lock(hashtext(:key))").param("key", "slot:" + key(date, start))
                .query(rs -> {
                });
        Slot slot = days().stream().filter(d -> d.date().equals(date)).flatMap(d -> d.slots().stream())
                .filter(s -> s.start().equals(start)).findFirst()
                .orElseThrow(() -> slotProblem("That delivery time isn't offered."));
        if (!slot.available()) {
            throw slotProblem("That delivery slot has just filled up or closed. Please choose another.");
        }
        return slot;
    }

    /** Every slot from today for {@code count} days, with bookings and capacity, for the admin. */
    public List<BoardDay> board(int count) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(settings.zone()));
        LocalDate today = now.toLocalDate();
        LocalDate last = today.plusDays(count - 1L);
        Map<String, Integer> booked = booked(today, last);
        Map<String, Override> overrides = overrides(today, last);
        List<BoardDay> days = new ArrayList<>();
        for (int d = 0; d < count; d++) {
            LocalDate date = today.plusDays(d);
            List<BoardSlot> slots = new ArrayList<>();
            for (int hour : settings.starts()) {
                LocalTime start = LocalTime.of(hour, 0);
                LocalTime end = start.plusHours(settings.lengthHours());
                Override custom = overrides.get(key(date, start));
                boolean past = !date.atTime(end).atZone(settings.zone()).isAfter(now);
                slots.add(new BoardSlot(start, end, label(start, end), booked.getOrDefault(key(date, start), 0),
                        custom == null ? settings.capacity() : custom.capacity(), custom != null,
                        custom == null ? null : custom.note(), past));
            }
            days.add(new BoardDay(date, d == 0 ? "Today" : d == 1 ? "Tomorrow" : date.format(DAY), slots));
        }
        return days;
    }

    /** Changes one slot's capacity for one day (0 closes it); null goes back to the usual capacity. */
    public void setCapacity(LocalDate date, LocalTime start, Integer capacity, String note) {
        if (!settings.starts().contains(start.getHour()) || start.getMinute() != 0) {
            throw slotProblem("That isn't one of the delivery slots.");
        }
        if (capacity == null) {
            jdbc.sql("delete from slot_capacity where slot_date = :d and slot_start = :s")
                    .param("d", Date.valueOf(date)).param("s", Time.valueOf(start)).update();
            return;
        }
        jdbc.sql("""
                        insert into slot_capacity (slot_date, slot_start, capacity, note) values (:d, :s, :c, :n)
                        on conflict (slot_date, slot_start) do update set capacity = :c, note = :n, updated_at = now()""")
                .param("d", Date.valueOf(date)).param("s", Time.valueOf(start)).param("c", capacity).param("n", note)
                .update();
    }

    public int usualCapacity() {
        return settings.capacity();
    }

    private Map<String, Override> overrides(LocalDate from, LocalDate to) {
        Map<String, Override> found = new HashMap<>();
        jdbc.sql("select slot_date, slot_start, capacity, note from slot_capacity where slot_date between :from and :to")
                .param("from", Date.valueOf(from)).param("to", Date.valueOf(to))
                .query(rs -> {
                    found.put(key(rs.getDate(1).toLocalDate(), rs.getTime(2).toLocalTime()),
                            new Override(rs.getInt(3), rs.getString(4)));
                });
        return found;
    }

    private Map<String, Integer> booked(LocalDate from, LocalDate to) {
        Map<String, Integer> counts = new HashMap<>();
        jdbc.sql("""
                        select slot_date, slot_start, count(*) from orders
                         where slot_date between :from and :to and status <> 'CANCELLED'
                         group by slot_date, slot_start""")
                .param("from", Date.valueOf(from)).param("to", Date.valueOf(to))
                .query(rs -> {
                    counts.put(key(rs.getDate(1).toLocalDate(), ((Time) rs.getTime(2)).toLocalTime()), rs.getInt(3));
                });
        return counts;
    }

    private static String key(LocalDate date, LocalTime start) {
        return date + "T" + start;
    }

    /** "6–8 pm", or "10 am–12 pm" when the slot crosses noon. */
    static String label(LocalTime start, LocalTime end) {
        String from = start.getHour() < 12 ? "am" : "pm";
        String to = end.getHour() < 12 ? "am" : "pm";
        return from.equals(to) ? hour(start) + "–" + hour(end) + " " + to
                : hour(start) + " " + from + "–" + hour(end) + " " + to;
    }

    private static String hour(LocalTime time) {
        int h = time.getHour() % 12;
        return String.valueOf(h == 0 ? 12 : h);
    }

    private static ApiException slotProblem(String message) {
        return new ApiException(HttpStatus.CONFLICT, "delivery.slot", message, Map.of("slot", message));
    }
}

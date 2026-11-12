package com.haat.store.customer;

import com.haat.store.delivery.DeliveryService;
import com.haat.store.shared.ApiException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A customer's saved delivery addresses. The first one becomes the default. */
@Service
public class AddressService {

    public static final int MAX_ADDRESSES = 10;

    public record AddressInput(String label, String name, String phone, String address, long areaId, String note) {
    }

    public record SavedAddress(long id, String label, String name, String phone, String address, long areaId,
                               String area, String note, boolean isDefault) {
    }

    private final JdbcClient jdbc;
    private final DeliveryService delivery;

    AddressService(JdbcClient jdbc, DeliveryService delivery) {
        this.jdbc = jdbc;
        this.delivery = delivery;
    }

    public List<SavedAddress> list(long customerId) {
        return jdbc.sql("""
                        select a.id, a.label, a.name, a.phone, a.address, a.area_id, d.name, a.note, a.is_default
                          from addresses a join delivery_areas d on d.id = a.area_id
                         where a.customer_id = :c order by a.is_default desc, a.created_at desc, a.id desc""")
                .param("c", customerId)
                .query((rs, n) -> new SavedAddress(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getLong(6), rs.getString(7), rs.getString(8), rs.getBoolean(9)))
                .list();
    }

    @Transactional
    public SavedAddress create(long customerId, AddressInput input, boolean makeDefault) {
        delivery.area(input.areaId());
        int count = jdbc.sql("select count(*) from addresses where customer_id = :c").param("c", customerId)
                .query(Integer.class).single();
        if (count >= MAX_ADDRESSES) {
            throw ApiException.conflict("address.limit", "You can save up to " + MAX_ADDRESSES + " addresses. Remove one first.");
        }
        boolean isDefault = makeDefault || count == 0;
        if (isDefault) {
            clearDefault(customerId);
        }
        long id = jdbc.sql("""
                        insert into addresses (customer_id, label, name, phone, address, area_id, note, is_default)
                        values (:c, :label, :name, :phone, :address, :area, :note, :def) returning id""")
                .param("c", customerId).param("label", input.label()).param("name", input.name())
                .param("phone", input.phone()).param("address", input.address()).param("area", input.areaId())
                .param("note", input.note()).param("def", isDefault)
                .query(Long.class).single();
        return get(customerId, id);
    }

    @Transactional
    public SavedAddress update(long customerId, long id, AddressInput input) {
        get(customerId, id);
        delivery.area(input.areaId());
        jdbc.sql("""
                        update addresses set label = :label, name = :name, phone = :phone, address = :address,
                                             area_id = :area, note = :note
                         where id = :id and customer_id = :c""")
                .param("label", input.label()).param("name", input.name()).param("phone", input.phone())
                .param("address", input.address()).param("area", input.areaId()).param("note", input.note())
                .param("id", id).param("c", customerId)
                .update();
        return get(customerId, id);
    }

    @Transactional
    public void delete(long customerId, long id) {
        SavedAddress removed = get(customerId, id);
        jdbc.sql("delete from addresses where id = :id and customer_id = :c").param("id", id).param("c", customerId).update();
        if (removed.isDefault()) {
            // The newest remaining address takes over as the default.
            jdbc.sql("""
                            update addresses set is_default = true where id = (
                                select id from addresses where customer_id = :c order by created_at desc, id desc limit 1)""")
                    .param("c", customerId).update();
        }
    }

    @Transactional
    public SavedAddress makeDefault(long customerId, long id) {
        get(customerId, id);
        clearDefault(customerId);
        jdbc.sql("update addresses set is_default = true where id = :id").param("id", id).update();
        return get(customerId, id);
    }

    /** From checkout: saves the address unless the customer already has the same one. */
    @Transactional
    public void rememberFromCheckout(long customerId, AddressInput input) {
        boolean known = jdbc.sql("""
                        select count(*) from addresses
                         where customer_id = :c and area_id = :area and lower(trim(address)) = lower(trim(:address))""")
                .param("c", customerId).param("area", input.areaId()).param("address", input.address())
                .query(Integer.class).single() > 0;
        int count = jdbc.sql("select count(*) from addresses where customer_id = :c").param("c", customerId)
                .query(Integer.class).single();
        if (!known && count < MAX_ADDRESSES) {
            create(customerId, input, false);
        }
    }

    private SavedAddress get(long customerId, long id) {
        return list(customerId).stream().filter(a -> a.id() == id).findFirst()
                .orElseThrow(() -> ApiException.notFound("address"));
    }

    private void clearDefault(long customerId) {
        jdbc.sql("update addresses set is_default = false where customer_id = :c and is_default").param("c", customerId).update();
    }
}

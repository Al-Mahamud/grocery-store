package com.haat.store.admin;

import com.haat.store.shared.ApiException;
import com.haat.store.shared.BangladeshPhone;
import com.haat.store.shared.StoreProperties;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Staff accounts for the admin area: a mobile number and password, like customers, but kept apart.
 * The first account comes from ADMIN_PHONE / ADMIN_PASSWORD when the shop starts with none.
 */
@Service
public class StaffService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);

    public record Staff(long id, String name, String phone) {
    }

    private record Credentials(long id, String hash, boolean active) {
    }

    private final JdbcClient jdbc;
    private final PasswordEncoder passwords;
    private final StoreProperties.Admin settings;
    private final String dummyHash;

    StaffService(JdbcClient jdbc, PasswordEncoder passwords, StoreProperties properties) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.settings = properties.admin();
        this.dummyHash = passwords.encode("not-a-real-password");
    }

    @Override
    public void run(ApplicationArguments args) {
        String phone = settings.firstStaffPhone();
        String password = settings.firstStaffPassword();
        if (phone == null || phone.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (jdbc.sql("select count(*) from staff").query(Long.class).single() > 0) {
            return;
        }
        String normalised = BangladeshPhone.normalise(phone)
                .orElseThrow(() -> new IllegalStateException("ADMIN_PHONE must be a Bangladeshi mobile number"));
        if (password.length() < 10) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 10 characters");
        }
        create(settings.firstStaffName(), normalised, password);
        log.info("Created the first staff account for {}", normalised);
    }

    public long create(String name, String phone, String password) {
        return jdbc.sql("insert into staff (name, phone, password_hash) values (:n, :p, :h) returning id")
                .param("n", name).param("p", phone).param("h", passwords.encode(password))
                .query(Long.class).single();
    }

    public Optional<Long> authenticate(String phone, String password) {
        Optional<Credentials> found = jdbc.sql("select id, password_hash, active from staff where phone = :p")
                .param("p", phone)
                .query((rs, n) -> new Credentials(rs.getLong(1), rs.getString(2), rs.getBoolean(3))).optional();
        boolean matches = passwords.matches(password, found.map(Credentials::hash).orElse(dummyHash));
        if (!matches || found.isEmpty() || !found.get().active()) {
            return Optional.empty();
        }
        jdbc.sql("update staff set last_signed_in = now() where id = :id").param("id", found.get().id()).update();
        return Optional.of(found.get().id());
    }

    /** The staff member, if the account still exists and is active. */
    public Optional<Staff> find(long id) {
        return jdbc.sql("select id, name, phone from staff where id = :id and active").param("id", id)
                .query((rs, n) -> new Staff(rs.getLong(1), rs.getString(2), rs.getString(3))).optional();
    }

    /** Who did it, for order history: "staff:Rina". */
    public String actor(long id) {
        return "staff:" + find(id).map(Staff::name).orElse(String.valueOf(id));
    }

    public Staff require(long id) {
        return find(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth.required", "Please sign in."));
    }
}

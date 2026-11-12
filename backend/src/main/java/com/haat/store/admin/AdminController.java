package com.haat.store.admin;

import com.haat.store.admin.AdminCatalog.ProductInput;
import com.haat.store.admin.AdminSettings.CouponInput;
import com.haat.store.delivery.DeliveryService;
import com.haat.store.shared.ApiException;
import com.haat.store.shared.CurrentStaff;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The admin area's API. Security lets only signed-in staff in; {@link AdminGuard} checks the account is still active. */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController {

    record StatusChange(@NotBlank String status, @Size(max = 300) String note, Boolean restock) {
    }

    record Refund(@NotBlank(message = "Enter the refund reference from SSLCommerz.") @Size(max = 80) String reference) {
    }

    record Note(@NotBlank(message = "Write a note.") @Size(max = 300) String note) {
    }

    record StockChange(@NotNull Integer stock) {
    }

    record SlotChange(@NotNull LocalDate date, @NotNull LocalTime start, Integer capacity, @Size(max = 120) String note) {
    }

    record AreaInput(String name, Boolean active) {
    }

    private final AdminOrders orders;
    private final AdminCatalog catalog;
    private final AdminSettings settings;
    private final DeliveryService delivery;
    private final StaffService staff;

    AdminController(AdminOrders orders, AdminCatalog catalog, AdminSettings settings, DeliveryService delivery,
                    StaffService staff) {
        this.orders = orders;
        this.catalog = catalog;
        this.settings = settings;
        this.delivery = delivery;
        this.staff = staff;
    }

    private String actor() {
        return staff.actor(CurrentStaff.require());
    }

    @GetMapping("/dashboard")
    AdminOrders.Dashboard dashboard() {
        return orders.dashboard();
    }

    // Orders

    @GetMapping("/orders")
    AdminOrders.Page orders(@RequestParam(defaultValue = "OPEN") String filter,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page) {
        return orders.list(filter, date, q, Math.max(0, page));
    }

    @GetMapping("/orders/{number}")
    AdminOrders.Detail order(@PathVariable String number) {
        return orders.detail(number);
    }

    @PostMapping("/orders/{number}/status")
    AdminOrders.Detail changeStatus(@PathVariable String number, @Valid @RequestBody StatusChange body) {
        orders.changeStatus(number, body.status(), body.note(), body.restock() == null || body.restock(), actor());
        return orders.detail(number);
    }

    @PostMapping("/orders/{number}/refund")
    AdminOrders.Detail refund(@PathVariable String number, @Valid @RequestBody Refund body) {
        orders.refund(number, body.reference().trim(), actor());
        return orders.detail(number);
    }

    @PostMapping("/orders/{number}/notes")
    AdminOrders.Detail note(@PathVariable String number, @Valid @RequestBody Note body) {
        orders.addNote(number, body.note(), actor());
        return orders.detail(number);
    }

    @GetMapping("/picking")
    AdminOrders.Picking picking(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start) {
        return orders.picking(date, start);
    }

    @PostMapping("/picking/start")
    Map<String, Integer> startPacking(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start) {
        return Map.of("moved", orders.startPacking(date, start, actor()));
    }

    // Products and stock

    @GetMapping("/products")
    AdminCatalog.ProductPage products(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) Long category,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "false") boolean low,
                                      @RequestParam(defaultValue = "0") int page) {
        return catalog.products(q, category, status, low, Math.max(0, page));
    }

    @GetMapping("/products/{id}")
    AdminCatalog.ProductView product(@PathVariable long id) {
        return catalog.product(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    AdminCatalog.ProductView createProduct(@RequestBody ProductInput body) {
        return catalog.save(null, body);
    }

    @PutMapping("/products/{id}")
    AdminCatalog.ProductView updateProduct(@PathVariable long id, @RequestBody ProductInput body) {
        return catalog.save(id, body);
    }

    @PostMapping("/products/{id}/image")
    AdminCatalog.ProductView uploadImage(@PathVariable long id, @RequestPart("file") MultipartFile file) throws IOException {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw ApiException.badRequest("image.size", "Pictures can be up to 5 MB.");
        }
        return catalog.setImage(id, file.getBytes());
    }

    @GetMapping("/stock")
    List<AdminCatalog.StockRow> stock(@RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "false") boolean low) {
        return catalog.stock(q, low);
    }

    @PatchMapping("/packs/{id}/stock")
    AdminCatalog.StockRow setStock(@PathVariable long id, @Valid @RequestBody StockChange body) {
        return catalog.setStock(id, body.stock());
    }

    // Coupons, areas and slots

    @GetMapping("/coupons")
    List<AdminSettings.CouponView> coupons() {
        return settings.coupons();
    }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    AdminSettings.CouponView createCoupon(@RequestBody CouponInput body) {
        return settings.saveCoupon(null, body);
    }

    @PutMapping("/coupons/{id}")
    AdminSettings.CouponView updateCoupon(@PathVariable long id, @RequestBody CouponInput body) {
        return settings.saveCoupon(id, body);
    }

    @GetMapping("/areas")
    List<AdminSettings.AreaView> areas() {
        return settings.areas();
    }

    @PostMapping("/areas")
    @ResponseStatus(HttpStatus.CREATED)
    AdminSettings.AreaView createArea(@RequestBody AreaInput body) {
        return settings.saveArea(null, body.name(), body.active() == null || body.active());
    }

    @PutMapping("/areas/{id}")
    AdminSettings.AreaView updateArea(@PathVariable long id, @RequestBody AreaInput body) {
        return settings.saveArea(id, body.name(), body.active() == null || body.active());
    }

    @GetMapping("/slots")
    Map<String, Object> slots(@RequestParam(defaultValue = "7") int days) {
        return Map.of("usualCapacity", delivery.usualCapacity(), "days", delivery.board(Math.min(Math.max(days, 1), 14)));
    }

    @PutMapping("/slots")
    Map<String, Object> setSlot(@Valid @RequestBody SlotChange body) {
        if (body.capacity() != null && (body.capacity() < 0 || body.capacity() > 500)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.",
                    Map.of("capacity", "From 0 (closed) to 500."));
        }
        delivery.setCapacity(body.date(), body.start(), body.capacity(), body.note());
        return slots(7);
    }
}

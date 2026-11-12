package com.haat.store.customer;

import com.haat.store.catalog.CatalogViews.ProductCard;
import com.haat.store.catalog.ProductSearch;
import com.haat.store.checkout.Bags;
import com.haat.store.checkout.CartService;
import com.haat.store.checkout.CartService.CartView;
import com.haat.store.customer.AddressService.AddressInput;
import com.haat.store.customer.AddressService.SavedAddress;
import com.haat.store.customer.CustomerService.Profile;
import com.haat.store.order.OrderService;
import com.haat.store.order.OrderService.Bought;
import com.haat.store.order.OrderService.History;
import com.haat.store.order.OrderService.OrderView;
import com.haat.store.order.OrderService.PastItem;
import com.haat.store.shared.ApiException;
import com.haat.store.shared.CurrentCustomer;
import com.haat.store.shared.LoginSessions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in customer's own things: profile, password, addresses, orders and "buy again". */
@RestController
@RequestMapping("/api/v1/account")
class AccountController {

    record ProfileUpdate(
            @NotBlank(message = "Please enter your name.") @Size(max = 80, message = "Name is too long.") String name,
            @Email(message = "That email doesn't look right.") @Size(max = 120) String email) {
    }

    record PasswordChange(@NotBlank(message = "Please enter your current password.") String currentPassword,
                          @NotBlank(message = "Please choose a new password.")
                          @Size(min = 8, max = 72, message = "Use at least 8 characters.") String newPassword) {
    }

    record AddressBody(
            @Size(max = 20, message = "Keep the label short, like Home or Office.") String label,
            @NotBlank(message = "Please enter the receiver's name.") @Size(max = 80, message = "Name is too long.") String name,
            @NotBlank(message = "Please enter a mobile number.") String phone,
            @NotBlank(message = "Please enter the address.") @Size(min = 5, max = 200, message = "Please enter the full address.") String address,
            @NotNull(message = "Please choose the area.") Long areaId,
            @Size(max = 200, message = "Notes can be up to 200 characters.") String note,
            Boolean makeDefault) {
    }

    record BuyAgainItem(ProductCard product, long packId, int timesBought) {
    }

    record Reordered(int added, List<String> unavailable, CartView cart) {
    }

    private final CustomerService customers;
    private final AddressService addresses;
    private final OrderService orders;
    private final ProductSearch search;
    private final CartService carts;
    private final Bags bags;
    private final LoginSessions sessions;
    private final JdbcClient jdbc;

    AccountController(CustomerService customers, AddressService addresses, OrderService orders, ProductSearch search,
                      CartService carts, Bags bags, LoginSessions sessions, JdbcClient jdbc) {
        this.customers = customers;
        this.addresses = addresses;
        this.orders = orders;
        this.search = search;
        this.carts = carts;
        this.bags = bags;
        this.sessions = sessions;
        this.jdbc = jdbc;
    }

    @GetMapping("/profile")
    Profile profile() {
        return customers.profile(CurrentCustomer.require());
    }

    @PutMapping("/profile")
    Profile updateProfile(@Valid @RequestBody ProfileUpdate body) {
        return customers.update(CurrentCustomer.require(), body.name().trim(), AuthController.blankToNull(body.email()));
    }

    /** Changing the password signs out every other device. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Valid @RequestBody PasswordChange body, HttpServletRequest request) {
        long id = CurrentCustomer.require();
        customers.changePassword(id, body.currentPassword(), body.newPassword());
        sessions.endOthers(id, request);
    }

    @GetMapping("/addresses")
    List<SavedAddress> addresses() {
        return addresses.list(CurrentCustomer.require());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    SavedAddress addAddress(@Valid @RequestBody AddressBody body) {
        return addresses.create(CurrentCustomer.require(), input(body), Boolean.TRUE.equals(body.makeDefault()));
    }

    @PutMapping("/addresses/{id}")
    SavedAddress updateAddress(@PathVariable long id, @Valid @RequestBody AddressBody body) {
        long customer = CurrentCustomer.require();
        SavedAddress saved = addresses.update(customer, id, input(body));
        return Boolean.TRUE.equals(body.makeDefault()) ? addresses.makeDefault(customer, id) : saved;
    }

    @PostMapping("/addresses/{id}/default")
    SavedAddress makeDefault(@PathVariable long id) {
        return addresses.makeDefault(CurrentCustomer.require(), id);
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAddress(@PathVariable long id) {
        addresses.delete(CurrentCustomer.require(), id);
    }

    @GetMapping("/orders")
    History orders(@RequestParam(defaultValue = "0") int page) {
        return orders.history(CurrentCustomer.require(), Math.max(0, page), 10);
    }

    @GetMapping("/orders/{number}")
    OrderView order(@PathVariable String number) {
        return orders.forCustomer(CurrentCustomer.require(), number);
    }

    /**
     * Puts an earlier order's items back in the bag. Each pack is raised to the earlier quantity
     * (never lowered), within stock and the per-order limit; sold-out or removed items are listed.
     */
    @PostMapping("/orders/{number}/reorder")
    Reordered reorder(@PathVariable String number, HttpServletRequest request) {
        long customer = CurrentCustomer.require();
        List<PastItem> past = orders.itemsForReorder(customer, number);
        Optional<UUID> bag = bags.current(request);
        int added = 0;
        List<String> unavailable = new ArrayList<>();
        for (PastItem item : past) {
            String label = item.name() + " (" + item.packLabel() + ")";
            Integer cap = item.packId() == null ? null : jdbc.sql("""
                            select least(k.stock, k.max_per_order) from packs k join products p on p.id = k.product_id
                             where k.id = :id and p.status = 'ACTIVE'""")
                    .param("id", item.packId()).query(Integer.class).optional().orElse(null);
            if (cap == null || cap == 0) {
                unavailable.add(label);
                continue;
            }
            int inBag = bag.map(id -> carts.lines(id).stream().filter(l -> l.packId() == item.packId())
                    .mapToInt(CartService.Line::quantity).sum()).orElse(0);
            int wanted = Math.min(Math.max(inBag, item.quantity()), cap);
            if (wanted > inBag) {
                try {
                    bag = Optional.of(carts.set(bag, customer, item.packId(), wanted));
                    added++;
                } catch (ApiException e) {
                    unavailable.add(label);
                }
            }
        }
        return new Reordered(added, unavailable, carts.view(bag.isPresent() ? bag : carts.ofCustomer(customer)));
    }

    @GetMapping("/buy-again")
    List<BuyAgainItem> buyAgain() {
        List<Bought> bought = orders.bought(CurrentCustomer.require(), 48);
        if (bought.isEmpty()) {
            return List.of();
        }
        Map<Long, ProductCard> cards = search.cards("p.id in (:ids)",
                        Map.of("ids", bought.stream().map(Bought::productId).toList()), "p.id", bought.size())
                .stream().collect(Collectors.toMap(ProductCard::id, Function.identity()));
        return bought.stream().filter(b -> cards.containsKey(b.productId()))
                .map(b -> new BuyAgainItem(cards.get(b.productId()), b.packId(), b.times()))
                .toList();
    }

    private static AddressInput input(AddressBody body) {
        String label = body.label() == null || body.label().isBlank() ? "Home" : body.label().trim();
        return new AddressInput(label, body.name().trim(), AuthController.phone(body.phone()), body.address().trim(),
                body.areaId(), AuthController.blankToNull(body.note()));
    }
}

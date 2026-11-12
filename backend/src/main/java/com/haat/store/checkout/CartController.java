package com.haat.store.checkout;

import com.haat.store.checkout.CartService.CartView;
import com.haat.store.shared.CurrentCustomer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The shopper's bag. Every change answers with the whole bag. */
@RestController
@RequestMapping("/api/v1/cart")
class CartController {

    record AddItem(@NotNull Long packId, @Min(1) @Max(99) int quantity) {
    }

    record SetQuantity(@Min(0) @Max(99) int quantity) {
    }

    record ApplyCoupon(@NotBlank String code) {
    }

    private final CartService carts;
    private final Bags bags;

    CartController(CartService carts, Bags bags) {
        this.carts = carts;
        this.bags = bags;
    }

    @GetMapping
    CartView get(HttpServletRequest request) {
        return carts.view(bags.current(request));
    }

    /** Adds to the quantity already in the bag. */
    @PostMapping("/items")
    CartView add(@Valid @RequestBody AddItem body, HttpServletRequest request, HttpServletResponse response) {
        return respond(request, response, carts.add(bags.current(request), owner(), body.packId(), body.quantity()));
    }

    /** Sets the exact quantity; 0 removes the item. This is what the − / + buttons call. */
    @PutMapping("/items/{packId}")
    CartView set(@PathVariable long packId, @Valid @RequestBody SetQuantity body, HttpServletRequest request,
                 HttpServletResponse response) {
        return respond(request, response, carts.set(bags.current(request), owner(), packId, body.quantity()));
    }

    @DeleteMapping("/items/{packId}")
    CartView remove(@PathVariable long packId, HttpServletRequest request, HttpServletResponse response) {
        Optional<UUID> current = bags.current(request);
        if (current.isEmpty()) {
            return carts.view(current);
        }
        return respond(request, response, carts.set(current, owner(), packId, 0));
    }

    @PostMapping("/coupon")
    CartView applyCoupon(@Valid @RequestBody ApplyCoupon body, HttpServletRequest request) {
        UUID id = bags.current(request).orElseThrow(CartService::emptyCart);
        carts.applyCoupon(id, body.code());
        return carts.view(Optional.of(id));
    }

    @DeleteMapping("/coupon")
    CartView removeCoupon(HttpServletRequest request) {
        Optional<UUID> id = bags.current(request);
        id.ifPresent(carts::removeCoupon);
        return carts.view(id);
    }

    private CartView respond(HttpServletRequest request, HttpServletResponse response, UUID id) {
        bags.remember(request, response, id);
        return carts.view(Optional.of(id));
    }

    private static Long owner() {
        return CurrentCustomer.id().orElse(null);
    }
}

package com.haat.store.checkout;

import com.haat.store.checkout.CheckoutService.Customer;
import com.haat.store.checkout.CheckoutService.PaymentMethod;
import com.haat.store.checkout.CheckoutService.Quote;
import com.haat.store.checkout.CheckoutService.Substitution;
import com.haat.store.shared.ApiException;
import com.haat.store.customer.AddressService;
import com.haat.store.payment.PaymentService;
import com.haat.store.shared.BangladeshPhone;
import com.haat.store.shared.CurrentCustomer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
class CheckoutController {

    record PlaceOrder(
            @NotBlank(message = "Please enter your name.") @Size(max = 80, message = "Name is too long.") String name,
            @NotBlank(message = "Please enter your mobile number.") String phone,
            @Email(message = "That email doesn't look right.") @Size(max = 120) String email,
            @NotBlank(message = "Please enter your address.") @Size(min = 5, max = 200, message = "Please enter your full address.") String address,
            @NotNull(message = "Please choose your area.") Long areaId,
            @NotNull(message = "Please choose a delivery slot.") LocalDate slotDate,
            @NotNull(message = "Please choose a delivery slot.") LocalTime slotStart,
            @NotNull(message = "Please tell us what to do if something is out of stock.") Substitution substitution,
            @Size(max = 500, message = "Notes can be up to 500 characters.") String note,
            @NotNull(message = "Please choose how to pay.") PaymentMethod paymentMethod,
            /* Signed-in customers: keep this address for next time. */
            Boolean saveAddress) {
    }

    record Placed(String number, String token, int total, String paymentUrl) {
    }

    private final CheckoutService checkout;
    private final Bags bags;
    private final AddressService addresses;
    private final PaymentService payments;

    CheckoutController(CheckoutService checkout, Bags bags, AddressService addresses, PaymentService payments) {
        this.checkout = checkout;
        this.bags = bags;
        this.addresses = addresses;
        this.payments = payments;
    }

    @PostMapping("/quote")
    Quote quote(HttpServletRequest request) {
        return checkout.quote(bags.current(request).orElseThrow(CartService::emptyCart));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Placed place(@Valid @RequestBody PlaceOrder body, HttpServletRequest request, HttpServletResponse response) {
        UUID cart = bags.current(request).orElseThrow(CartService::emptyCart);
        Optional<Long> customerId = CurrentCustomer.id();
        String phone = BangladeshPhone.normalise(body.phone()).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.",
                Map.of("phone", "Enter an 11-digit mobile number, like 01712345678.")));
        if (body.paymentMethod() == PaymentMethod.ONLINE && !payments.onlineAvailable()) {
            throw ApiException.badRequest("payment.unavailable", "Online payment isn't available right now. Please choose cash on delivery.");
        }
        Customer customer = new Customer(body.name().trim(), phone, blankToNull(body.email()), body.address().trim(),
                body.areaId(), blankToNull(body.note()));
        CheckoutService.Placed placed = checkout.place(cart, customer, body.slotDate(), body.slotStart(),
                body.substitution(), body.paymentMethod(), customerId.orElse(null));
        if (customerId.isPresent() && Boolean.TRUE.equals(body.saveAddress())) {
            addresses.rememberFromCheckout(customerId.get(), new AddressService.AddressInput("Home", customer.name(),
                    phone, customer.address(), customer.areaId(), null));
        }
        bags.forget(request, response);
        String paymentUrl = null;
        if (body.paymentMethod() == PaymentMethod.ONLINE) {
            try {
                paymentUrl = payments.start(placed.orderId());
            } catch (ApiException e) {
                // The order is saved and waits for payment; its page offers to try again.
            }
        }
        return new Placed(placed.number(), placed.accessToken(), placed.total(), paymentUrl);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.haat.store.customer;

import com.haat.store.checkout.Bags;
import com.haat.store.customer.CustomerService.Profile;
import com.haat.store.shared.ApiException;
import com.haat.store.shared.BangladeshPhone;
import com.haat.store.shared.CurrentCustomer;
import com.haat.store.shared.LoginSessions;
import com.haat.store.shared.SignInLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Create an account, sign in and sign out. The bag filled as a guest moves into the account. */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    record Register(
            @NotBlank(message = "Please enter your name.") @Size(max = 80, message = "Name is too long.") String name,
            @NotBlank(message = "Please enter your mobile number.") String phone,
            @Email(message = "That email doesn't look right.") @Size(max = 120) String email,
            @NotBlank(message = "Please choose a password.")
            @Size(min = 8, max = 72, message = "Use at least 8 characters.") String password) {
    }

    record SignIn(@NotBlank(message = "Please enter your mobile number.") String phone,
                  @NotBlank(message = "Please enter your password.") String password) {
    }

    record Me(Profile customer) {
    }

    private final CustomerService customers;
    private final LoginSessions sessions;
    private final SignInLimiter limiter;
    private final Bags bags;

    AuthController(CustomerService customers, LoginSessions sessions, SignInLimiter limiter, Bags bags) {
        this.customers = customers;
        this.sessions = sessions;
        this.limiter = limiter;
        this.bags = bags;
    }

    @GetMapping("/me")
    Me me() {
        return new Me(CurrentCustomer.id().flatMap(customers::find).orElse(null));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    Me register(@Valid @RequestBody Register body, HttpServletRequest request, HttpServletResponse response) {
        String phone = phone(body.phone());
        Profile profile = customers.register(body.name().trim(), phone, blankToNull(body.email()), body.password());
        signIn(profile.id(), request, response);
        return new Me(profile);
    }

    @PostMapping("/login")
    Me login(@Valid @RequestBody SignIn body, HttpServletRequest request, HttpServletResponse response) {
        String phone = BangladeshPhone.normalise(body.phone()).orElse(body.phone().trim());
        String address = request.getRemoteAddr();
        limiter.check(phone, address);
        long id = customers.authenticate(phone, body.password()).orElseThrow(() -> {
            limiter.failed(phone, address);
            return new ApiException(HttpStatus.UNAUTHORIZED, "auth.bad_credentials",
                    "That mobile number and password don't match.");
        });
        limiter.succeeded(phone);
        signIn(id, request, response);
        return new Me(customers.profile(id));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        sessions.end(request);
        bags.forget(request, response);
    }

    private void signIn(long customerId, HttpServletRequest request, HttpServletResponse response) {
        sessions.startCustomer(customerId, request, response);
        bags.claim(customerId, request, response);
    }

    static String phone(String input) {
        return BangladeshPhone.normalise(input).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                "request.invalid", "Please check the highlighted fields.",
                Map.of("phone", "Enter an 11-digit mobile number, like 01712345678.")));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

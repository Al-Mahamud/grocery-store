package com.haat.store.delivery;

import com.haat.store.delivery.DeliveryService.Area;
import com.haat.store.delivery.DeliveryService.Day;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery")
class DeliveryController {

    private final DeliveryService delivery;

    DeliveryController(DeliveryService delivery) {
        this.delivery = delivery;
    }

    @GetMapping("/areas")
    List<Area> areas() {
        return delivery.areas();
    }

    @GetMapping("/slots")
    List<Day> slots() {
        return delivery.days();
    }
}

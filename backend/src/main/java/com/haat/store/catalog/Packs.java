package com.haat.store.catalog;

import com.haat.store.catalog.CatalogViews.Pack;
import com.haat.store.catalog.CatalogViews.UnitPrice;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Unit-price arithmetic for packs. */
public final class Packs {

    private Packs() {
    }

    /** Price per kg, litre or piece; null for a single piece, where it would repeat the price. */
    public static UnitPrice unitPrice(int price, BigDecimal amount, String unit) {
        BigDecimal base;
        String per;
        switch (unit) {
            case "G" -> { base = amount.divide(BigDecimal.valueOf(1000)); per = "kg"; }
            case "KG" -> { base = amount; per = "kg"; }
            case "ML" -> { base = amount.divide(BigDecimal.valueOf(1000)); per = "L"; }
            case "L" -> { base = amount; per = "L"; }
            default -> { base = amount; per = "pc"; }
        }
        if (base.compareTo(BigDecimal.ONE) == 0 || ("pc".equals(per) && base.compareTo(BigDecimal.ONE) <= 0)) {
            return null;
        }
        int scaled = BigDecimal.valueOf(price).divide(base, 0, RoundingMode.HALF_UP).intValueExact();
        return new UnitPrice(scaled, per);
    }

    public static Pack pack(long id, String label, BigDecimal amount, String unit, int price, Integer compareAt,
                            int stock, int maxPerOrder) {
        return new Pack(id, label, amount.stripTrailingZeros(), unit, price, compareAt,
                unitPrice(price, amount, unit), stock, maxPerOrder);
    }
}

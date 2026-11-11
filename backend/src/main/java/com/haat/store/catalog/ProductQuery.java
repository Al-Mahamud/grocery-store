package com.haat.store.catalog;

import java.util.List;

/** Filters for the product grid. Empty lists and false mean "no filter". */
public record ProductQuery(String category, String q, List<String> brands, boolean organic, boolean local,
                           boolean deal, Sort sort, int page, int pageSize) {

    public enum Sort {
        RECOMMENDED("p.featured desc, p.sold_count desc, p.id"),
        POPULAR("p.sold_count desc, p.id"),
        PRICE_ASC("mp.min_price asc, p.id"),
        PRICE_DESC("mp.min_price desc, p.id"),
        DISCOUNT("mp.best_discount desc nulls last, p.sold_count desc, p.id"),
        NEWEST("p.created_at desc, p.id desc");

        final String orderBy;

        Sort(String orderBy) {
            this.orderBy = orderBy;
        }
    }
}

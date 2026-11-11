package com.haat.store.catalog;

import java.math.BigDecimal;
import java.util.List;

/** What the catalog API returns. */
public final class CatalogViews {

    private CatalogViews() {
    }

    /** "৳120 per kg": the pack price scaled to one kilogram, litre or piece. */
    public record UnitPrice(int price, String per) {
    }

    public record Pack(long id, String label, BigDecimal amount, String unit, int price, Integer compareAtPrice,
                       UnitPrice unitPrice, int stock, int maxPerOrder) {
    }

    public record ProductCard(long id, String slug, String name, String localName, String brand, String category,
                              String image, boolean organic, boolean local, boolean fresh, boolean weighed,
                              List<Pack> packs, boolean soldOut) {
    }

    public record CategoryNode(long id, String name, String slug, String icon, String color, String image,
                               int productCount, List<CategoryNode> children) {
    }

    public record CategoryRef(String name, String slug) {
    }

    public record FacetValue(String value, String label, int count, boolean selected) {
    }

    public record Facets(List<FacetValue> brands, int organic, int local, int deals) {
    }

    public record ProductPage(List<ProductCard> items, int page, int pageSize, long total, Facets facets) {
    }

    public record ProductDetail(long id, String slug, String name, String localName, String brand,
                                List<CategoryRef> breadcrumb, String description, String storage, String origin,
                                String image, boolean organic, boolean local, boolean fresh, boolean weighed,
                                List<Pack> packs) {
    }

    public record Suggestion(String slug, String name, String localName, String image, int price, String packLabel) {
    }

    public record Home(List<CategoryNode> categories, List<ProductCard> deals, List<ProductCard> bestSellers,
                       List<ProductCard> fresh, List<ProductCard> pantry) {
    }
}

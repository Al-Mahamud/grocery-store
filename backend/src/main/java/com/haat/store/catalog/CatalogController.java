package com.haat.store.catalog;

import com.haat.store.catalog.CatalogViews.CategoryNode;
import com.haat.store.catalog.CatalogViews.Suggestion;
import com.haat.store.catalog.CatalogViews.Home;
import com.haat.store.catalog.CatalogViews.ProductCard;
import com.haat.store.catalog.CatalogViews.ProductDetail;
import com.haat.store.catalog.CatalogViews.ProductPage;
import com.haat.store.shared.ApiException;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class CatalogController {

    private static final int MAX_PAGE_SIZE = 60;

    private final CatalogService catalog;
    private final ProductSearch search;

    CatalogController(CatalogService catalog, ProductSearch search) {
        this.catalog = catalog;
        this.search = search;
    }

    @GetMapping("/home")
    Home home() {
        return catalog.home();
    }

    @GetMapping("/categories")
    List<CategoryNode> categories() {
        return catalog.categoryTree();
    }

    @GetMapping("/categories/{slug}")
    CategoryNode category(@PathVariable String slug) {
        return catalog.category(slug);
    }

    /**
     * Product grid. {@code brand} takes comma-separated names;
     * {@code sort} is one of recommended, popular, price_asc, price_desc, discount, newest.
     */
    @GetMapping("/products")
    ProductPage products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "false") boolean organic,
            @RequestParam(defaultValue = "false") boolean local,
            @RequestParam(defaultValue = "false") boolean deal,
            @RequestParam(defaultValue = "recommended") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("request.paging", "Page must be 0 or more and page size 1 to 60.");
        }
        ProductQuery.Sort order = Arrays.stream(ProductQuery.Sort.values())
                .filter(s -> s.name().equalsIgnoreCase(sort)).findFirst()
                .orElseThrow(() -> ApiException.badRequest("request.sort", "Unknown sort order."));
        return search.search(new ProductQuery(blankToNull(category), blankToNull(q), split(brand), organic, local,
                deal, order, page, pageSize));
    }

    @GetMapping("/search/suggest")
    List<Suggestion> suggest(@RequestParam(defaultValue = "") String q) {
        return q.isBlank() ? List.of() : search.suggest(q);
    }

    @GetMapping("/products/{slug}")
    ProductDetail product(@PathVariable String slug) {
        return catalog.product(slug);
    }

    @GetMapping("/products/{slug}/related")
    List<ProductCard> related(@PathVariable String slug) {
        return catalog.related(slug, 8);
    }

    private static List<String> split(String value) {
        if (value == null) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(20)
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

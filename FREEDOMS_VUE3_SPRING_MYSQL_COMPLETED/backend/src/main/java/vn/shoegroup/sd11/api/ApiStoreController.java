package vn.shoegroup.sd11.api;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import static vn.shoegroup.sd11.api.ApiDtos.*;

@RestController
@RequestMapping("/api/store")
public class ApiStoreController {
    private final CatalogApiService catalog;
    @org.springframework.beans.factory.annotation.Value("${app.payment.bank-code:MB}") private String bankCode;
    @org.springframework.beans.factory.annotation.Value("${app.payment.bank-account:}") private String bankAccount;
    @org.springframework.beans.factory.annotation.Value("${app.payment.bank-account-name:FREEDOMS SHOE STORE}") private String bankAccountName;
    public ApiStoreController(CatalogApiService catalog) { this.catalog = catalog; }

    @GetMapping("/products")
    public List<ProductView> products(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(defaultValue = "ONLINE") String channel,
                                      @RequestParam(defaultValue = "false") boolean salesOnly) {
        boolean salesSearch = salesOnly || "sales".equalsIgnoreCase(q.trim()) || "sale".equalsIgnoreCase(q.trim());
        String textQuery = salesSearch && ("sales".equalsIgnoreCase(q.trim()) || "sale".equalsIgnoreCase(q.trim())) ? "" : q;
        return catalog.products(textQuery, channel, salesSearch, false);
    }

    @GetMapping("/products/{id}")
    public ProductView product(@PathVariable Long id, @RequestParam(defaultValue = "ONLINE") String channel) {
        return catalog.product(id, channel, false);
    }

    @GetMapping("/payment-config")
    public java.util.Map<String,String> paymentConfig() { return java.util.Map.of("bankCode", bankCode, "bankAccount", bankAccount, "bankAccountName", bankAccountName); }

    @GetMapping("/coupons")
    public List<CouponView> coupons(@RequestParam(defaultValue = "ONLINE") String channel) {
        return catalog.activeCoupons(channel);
    }
}

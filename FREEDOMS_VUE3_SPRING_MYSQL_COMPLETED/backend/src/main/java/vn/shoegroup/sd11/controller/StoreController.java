package vn.shoegroup.sd11.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.shoegroup.sd11.repository.*;
@Controller
public class StoreController {
    private final ProductRepository products; private final ProductVariantRepository variants; private final CategoryRepository categories; private final BrandRepository brands;
    public StoreController(ProductRepository products,ProductVariantRepository variants,CategoryRepository categories,BrandRepository brands){this.products=products;this.variants=variants;this.categories=categories;this.brands=brands;}
    @GetMapping("/") public String home(){ return "redirect:/app/"; }
    @GetMapping("/products") public String products(@RequestParam(defaultValue="") String q, Model model){
        model.addAttribute("products",products.searchActive(q.trim())); model.addAttribute("q",q);
        model.addAttribute("categories",categories.findByActiveTrueOrderByCategoryNameAsc());
        model.addAttribute("brands",brands.findByActiveTrueOrderByBrandNameAsc()); return "store/products";
    }
    @GetMapping("/products/{id}") public String detail(@PathVariable Long id,Model model){
        var product=products.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if(!Boolean.TRUE.equals(product.getActive())) throw new IllegalArgumentException("Sản phẩm đã ngừng kinh doanh.");
        model.addAttribute("product",product); model.addAttribute("variants",variants.findByProduct_IdAndActiveTrueOrderByIdAsc(id)); return "store/product-detail";
    }
    @GetMapping("/about") public String about(){return "store/about";}
    @GetMapping("/contact") public String contact(){return "store/contact";}
}

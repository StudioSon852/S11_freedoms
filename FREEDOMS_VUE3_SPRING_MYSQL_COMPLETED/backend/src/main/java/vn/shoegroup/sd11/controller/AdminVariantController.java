package vn.shoegroup.sd11.controller;
import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.bind.annotation.*;import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.entity.ProductVariant;import vn.shoegroup.sd11.repository.*;import java.math.BigDecimal;
@Controller
@RequestMapping("/admin/variants")
public class AdminVariantController {
    private final ProductVariantRepository variants;private final ProductRepository products;private final SizeRepository sizes;private final ColorRepository colors;
    public AdminVariantController(ProductVariantRepository variants,ProductRepository products,SizeRepository sizes,ColorRepository colors){this.variants=variants;this.products=products;this.sizes=sizes;this.colors=colors;}
    @GetMapping public String list(Model model){model.addAttribute("variants",variants.findAll());return "admin/variants";}
    @GetMapping("/new") public String form(@RequestParam(required=false) Long productId,Model model){model.addAttribute("productId",productId);addLookups(model);return "admin/variant-form";}
    @PostMapping @Transactional public String create(@RequestParam Long productId,@RequestParam(required=false) Long sizeId,@RequestParam(required=false) Long colorId,
            @RequestParam String sku,@RequestParam BigDecimal unitPrice,@RequestParam Integer stockQuantity,RedirectAttributes redirect){
        if(unitPrice==null||unitPrice.signum()<=0||stockQuantity==null||stockQuantity<0||sku==null||sku.isBlank()){
            redirect.addFlashAttribute("errorMessage","Vui lòng nhập SKU, giá hợp lệ và tồn kho không âm.");return "redirect:/admin/variants/new?productId="+productId;
        }
        ProductVariant v=new ProductVariant();v.setProduct(products.findById(productId).orElseThrow());v.setSize(sizeId==null?null:sizes.findById(sizeId).orElse(null));
        v.setColor(colorId==null?null:colors.findById(colorId).orElse(null));v.setSku(sku.trim());v.setUnitPrice(unitPrice);v.setStockQuantity(stockQuantity);v.setActive(true);variants.save(v);
        redirect.addFlashAttribute("successMessage","Đã thêm biến thể sản phẩm.");return "redirect:/admin/variants";
    }
    private void addLookups(Model model){model.addAttribute("products",products.findAll());model.addAttribute("sizes",sizes.findAllByOrderBySortOrderAsc());model.addAttribute("colors",colors.findAllByOrderByColorNameAsc());}
}

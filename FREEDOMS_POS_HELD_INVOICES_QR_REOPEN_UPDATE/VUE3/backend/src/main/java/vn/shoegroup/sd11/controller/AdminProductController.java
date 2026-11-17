package vn.shoegroup.sd11.controller;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.dto.ProductForm;
import vn.shoegroup.sd11.entity.Product;
import vn.shoegroup.sd11.entity.ProductVariant;
import vn.shoegroup.sd11.repository.*;
import java.time.LocalDateTime;
@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final ProductRepository products; private final ProductVariantRepository variants; private final CategoryRepository categories;
    private final BrandRepository brands; private final SizeRepository sizes; private final ColorRepository colors;
    public AdminProductController(ProductRepository products,ProductVariantRepository variants,CategoryRepository categories,BrandRepository brands,SizeRepository sizes,ColorRepository colors){
        this.products=products;this.variants=variants;this.categories=categories;this.brands=brands;this.sizes=sizes;this.colors=colors;
    }
    @GetMapping public String list(Model model){model.addAttribute("products",products.findAll());return "admin/products";}
    @GetMapping("/new") public String createForm(Model model){model.addAttribute("form",new ProductForm());addLookups(model);return "admin/product-form";}
    @PostMapping @Transactional public String create(@ModelAttribute ProductForm form,Model model,RedirectAttributes redirect){
        try{
            if(form.getProductName()==null||form.getProductName().isBlank()||form.getSku()==null||form.getSku().isBlank()) throw new IllegalArgumentException("Tên sản phẩm và SKU biến thể là bắt buộc.");
            if(form.getUnitPrice()==null||form.getUnitPrice().signum()<=0||form.getStockQuantity()==null||form.getStockQuantity()<0) throw new IllegalArgumentException("Giá phải lớn hơn 0 và tồn kho không được âm.");
            Product p=new Product(); p.setCategory(form.getCategoryId()==null?null:categories.findById(form.getCategoryId()).orElse(null));
            p.setBrand(form.getBrandId()==null?null:brands.findById(form.getBrandId()).orElse(null)); p.setProductName(form.getProductName().trim());
            p.setParentSku(form.getParentSku());p.setDescription(form.getDescription());p.setImageUrl(blankTo(form.getImageUrl(),"/img/hero-sneakers.jpg"));
            p.setFeatured(form.isFeatured());p.setActive(true);p.setCreatedAt(LocalDateTime.now());products.save(p);
            ProductVariant v=new ProductVariant();v.setProduct(p);v.setSize(form.getSizeId()==null?null:sizes.findById(form.getSizeId()).orElse(null));
            v.setColor(form.getColorId()==null?null:colors.findById(form.getColorId()).orElse(null));v.setSku(form.getSku().trim());
            v.setUnitPrice(form.getUnitPrice());v.setStockQuantity(form.getStockQuantity());v.setActive(true);variants.save(v);
            redirect.addFlashAttribute("successMessage","Đã tạo sản phẩm và biến thể đầu tiên.");return "redirect:/admin/products";
        }catch(RuntimeException ex){model.addAttribute("errorMessage",ex.getMessage());model.addAttribute("form",form);addLookups(model);return "admin/product-form";}
    }
    private void addLookups(Model model){model.addAttribute("categories",categories.findByActiveTrueOrderByCategoryNameAsc());model.addAttribute("brands",brands.findByActiveTrueOrderByBrandNameAsc());model.addAttribute("sizes",sizes.findAllByOrderBySortOrderAsc());model.addAttribute("colors",colors.findAllByOrderByColorNameAsc());}
    private String blankTo(String value,String fallback){return value==null||value.isBlank()?fallback:value.trim();}
}

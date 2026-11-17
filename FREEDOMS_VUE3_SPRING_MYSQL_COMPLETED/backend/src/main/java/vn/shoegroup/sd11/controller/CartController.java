package vn.shoegroup.sd11.controller;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.service.SessionCartService;
import java.math.BigDecimal;
@Controller
@RequestMapping("/cart")
public class CartController {
    private final SessionCartService cartService;
    public CartController(SessionCartService cartService){this.cartService=cartService;}
    @GetMapping public String view(HttpSession session,Model model){
        var lines=cartService.view(session,SessionCartService.CUSTOMER_CART);
        BigDecimal total=lines.stream().map(l->l.lineTotal()).reduce(BigDecimal.ZERO,BigDecimal::add);
        model.addAttribute("lines",lines);model.addAttribute("total",total);return "store/cart";
    }
    @PostMapping("/add") public String add(@RequestParam Long variantId,@RequestParam(defaultValue="1") int quantity,
            @RequestParam(required=false) String redirectTo,HttpSession session,RedirectAttributes redirect){
        try{cartService.add(session,SessionCartService.CUSTOMER_CART,variantId,quantity);redirect.addFlashAttribute("successMessage","Đã thêm sản phẩm vào giỏ hàng.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}
        return "redirect:"+("checkout".equalsIgnoreCase(redirectTo)?"/checkout":"/products");
    }
    @PostMapping("/update") public String update(@RequestParam Long variantId,@RequestParam int quantity,HttpSession session,RedirectAttributes redirect){
        try{cartService.update(session,SessionCartService.CUSTOMER_CART,variantId,quantity);}catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}
        return "redirect:/cart";
    }
    @PostMapping("/remove") public String remove(@RequestParam Long variantId,HttpSession session){cartService.remove(session,SessionCartService.CUSTOMER_CART,variantId);return "redirect:/cart";}
}

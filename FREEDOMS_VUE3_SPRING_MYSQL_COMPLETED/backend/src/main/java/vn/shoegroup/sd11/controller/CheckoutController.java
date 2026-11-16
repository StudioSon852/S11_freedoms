package vn.shoegroup.sd11.controller;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.dto.CheckoutForm;
import vn.shoegroup.sd11.entity.CustomerOrder;
import vn.shoegroup.sd11.service.OrderService;
import vn.shoegroup.sd11.service.SessionCartService;
import java.math.BigDecimal;
@Controller
@RequestMapping("/checkout")
public class CheckoutController {
    private final SessionCartService cartService; private final OrderService orderService;
    public CheckoutController(SessionCartService cartService,OrderService orderService){this.cartService=cartService;this.orderService=orderService;}
    @GetMapping public String checkout(HttpSession session,@AuthenticationPrincipal CustomUserDetails principal,Model model){
        var lines=cartService.view(session,SessionCartService.CUSTOMER_CART);
        if(lines.isEmpty()) return "redirect:/cart";
        BigDecimal total=lines.stream().map(l->l.lineTotal()).reduce(BigDecimal.ZERO,BigDecimal::add);
        model.addAttribute("lines",lines);model.addAttribute("total",total);
        CheckoutForm form=new CheckoutForm(); form.setCustomerName(principal.getUser().getFullName()); form.setCustomerPhone(principal.getUser().getPhone());
        model.addAttribute("checkoutForm",form);return "order/checkout";
    }
    @PostMapping("/submit") public String submit(@Valid @ModelAttribute CheckoutForm checkoutForm,BindingResult result,
            HttpSession session,@AuthenticationPrincipal CustomUserDetails principal,Model model,RedirectAttributes redirect){
        var lines=cartService.view(session,SessionCartService.CUSTOMER_CART);
        if(lines.isEmpty()) return "redirect:/cart";
        if(result.hasErrors()) {
            model.addAttribute("lines",lines); model.addAttribute("total",lines.stream().map(l->l.lineTotal()).reduce(BigDecimal.ZERO,BigDecimal::add));
            return "order/checkout";
        }
        try{
            CustomerOrder order=orderService.createOrder(cartService.entries(session,SessionCartService.CUSTOMER_CART),principal.getUser(),null,
                    "ONLINE",checkoutForm.getPaymentMethod(),checkoutForm.getCustomerName(),checkoutForm.getCustomerPhone(),
                    checkoutForm.getShippingAddress(),checkoutForm.getCustomerNote(),checkoutForm.getCouponCode());
            cartService.clear(session,SessionCartService.CUSTOMER_CART);
            redirect.addFlashAttribute("successMessage","Đặt hàng thành công. Mã đơn: "+order.getOrderCode());
            return "redirect:/orders/"+order.getId();
        }catch(IllegalArgumentException ex){
            model.addAttribute("errorMessage",ex.getMessage()); model.addAttribute("lines",lines);
            model.addAttribute("total",lines.stream().map(l->l.lineTotal()).reduce(BigDecimal.ZERO,BigDecimal::add));
            return "order/checkout";
        }
    }
}

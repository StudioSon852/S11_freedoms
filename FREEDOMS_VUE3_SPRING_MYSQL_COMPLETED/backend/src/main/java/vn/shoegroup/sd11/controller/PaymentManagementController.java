package vn.shoegroup.sd11.controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.repository.PaymentRepository;
import vn.shoegroup.sd11.service.OrderService;
@Controller
@RequestMapping("/employee/payments")
public class PaymentManagementController {
    private final PaymentRepository payments; private final OrderService orderService;
    public PaymentManagementController(PaymentRepository payments,OrderService orderService){this.payments=payments;this.orderService=orderService;}
    @GetMapping public String list(Model model){model.addAttribute("payments",payments.findAllByOrderByCreatedAtDesc());return "pos/payments";}
    @PostMapping("/{id}/confirm") public String confirm(@PathVariable Long id,@RequestParam(required=false) String reference,
            @AuthenticationPrincipal CustomUserDetails principal,RedirectAttributes redirect){
        try{orderService.confirmPayment(id,principal.getUser(),reference);redirect.addFlashAttribute("successMessage","Đã xác nhận thanh toán.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/employee/payments";
    }
}

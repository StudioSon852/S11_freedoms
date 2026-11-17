package vn.shoegroup.sd11.controller;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.entity.AppUser;
import vn.shoegroup.sd11.repository.ProductVariantRepository;
import vn.shoegroup.sd11.service.HeldInvoiceService;
import vn.shoegroup.sd11.service.SessionCartService;
@Controller
public class PosController {
    @Value("${app.payment.bank-code:MB}") private String bankCode;
    @Value("${app.payment.bank-account:}") private String bankAccount;
    @Value("${app.payment.bank-account-name:FREEDOMS SHOE STORE}") private String bankAccountName;
    @Value("${app.payment.bank-name:MB Bank}") private String bankName;
    private final ProductVariantRepository variants; private final SessionCartService carts; private final HeldInvoiceService held; private final vn.shoegroup.sd11.repository.CustomerOrderRepository orders; private final vn.shoegroup.sd11.repository.PaymentRepository payments; private final vn.shoegroup.sd11.service.OrderService orderService;
    public PosController(ProductVariantRepository variants,SessionCartService carts,HeldInvoiceService held, vn.shoegroup.sd11.repository.CustomerOrderRepository orders, vn.shoegroup.sd11.repository.PaymentRepository payments, vn.shoegroup.sd11.service.OrderService orderService){this.variants=variants;this.carts=carts;this.held=held;this.orders=orders;this.payments=payments;this.orderService=orderService;}
    @GetMapping("/employee") public String employee(){return "redirect:/employee/pos";}
    @GetMapping("/employee/pos") public String pos(Model model){model.addAttribute("variants",variants.findByActiveTrueOrderByIdAsc());return "pos/index";}
    @PostMapping("/employee/pos/add") public String add(@RequestParam Long variantId,@RequestParam(defaultValue="1") int quantity,HttpSession session,RedirectAttributes redirect){
        try{carts.add(session,SessionCartService.POS_CART,variantId,quantity);redirect.addFlashAttribute("successMessage","Đã thêm sản phẩm vào POS.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/employee/pos";
    }
    @GetMapping("/employee/pos/cart") public String cart(HttpSession session,Model model){model.addAttribute("lines",carts.view(session,SessionCartService.POS_CART));return "pos/cart";}
    @PostMapping("/employee/pos/cart/update") public String update(@RequestParam Long variantId,@RequestParam int quantity,HttpSession session,RedirectAttributes redirect){
        try{carts.update(session,SessionCartService.POS_CART,variantId,quantity);}catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/employee/pos/cart";
    }
    @PostMapping("/employee/pos/cart/remove") public String remove(@RequestParam Long variantId,HttpSession session){carts.remove(session,SessionCartService.POS_CART,variantId);return "redirect:/employee/pos/cart";}
    @PostMapping("/employee/pos/hold") public String hold(@RequestParam(required=false) String customerName,@RequestParam(required=false) String customerPhone,
            @RequestParam(required=false) String customerNote,@RequestParam(required=false) String couponCode,HttpSession session,
            @AuthenticationPrincipal CustomUserDetails principal,RedirectAttributes redirect){
        try{var invoice=held.create(principal.getUser(),customerName,customerPhone,customerNote,couponCode,carts.entries(session,SessionCartService.POS_CART));
            carts.clear(session,SessionCartService.POS_CART);redirect.addFlashAttribute("successMessage","Đã lưu hóa đơn chờ "+invoice.getHeldCode()+". Kho chưa bị trừ.");return "redirect:/employee/held-invoices";}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());return "redirect:/employee/pos/cart";}
    }
    @GetMapping("/employee/orders/{id}") public String posOrderDetail(@PathVariable Long id,Model model){
        var order=orders.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy đơn bán tại quầy."));
        model.addAttribute("order",order);model.addAttribute("items",orderService.getItems(id));
        var payment = payments.findFirstByOrder_IdOrderByCreatedAtDesc(id).orElse(null);
        model.addAttribute("payment",payment); model.addAttribute("bankName",bankName); model.addAttribute("bankAccount",bankAccount); model.addAttribute("bankAccountName",bankAccountName);
        String qrUrl=null;
        if(payment!=null && "BANK_TRANSFER".equals(payment.getPaymentMethod()) && bankAccount!=null && !bankAccount.isBlank()) {
            qrUrl="https://img.vietqr.io/image/"+bankCode+"-"+bankAccount+"-compact2.png?amount="+order.getTotalAmount().longValue()
                +"&addInfo="+URLEncoder.encode(order.getOrderCode(),StandardCharsets.UTF_8)+"&accountName="+URLEncoder.encode(bankAccountName,StandardCharsets.UTF_8);
        }
        model.addAttribute("qrUrl",qrUrl); return "pos/order-detail";
    }
    @GetMapping("/employee/held-invoices") public String heldList(@AuthenticationPrincipal CustomUserDetails principal,Model model){model.addAttribute("invoices",held.listFor(principal.getUser()));return "pos/held-list";}
    @GetMapping("/employee/held-invoices/{id}") public String heldDetail(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails principal,Model model){
        model.addAttribute("invoice",held.listFor(principal.getUser()).stream().filter(x->x.getId().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Không tìm thấy hóa đơn chờ.")));
        model.addAttribute("items",held.getItems(id,principal.getUser()));return "pos/held-detail";
    }
    @PostMapping("/employee/held-invoices/{id}/convert") public String convert(@PathVariable Long id,@RequestParam String paymentMethod,
            @AuthenticationPrincipal CustomUserDetails principal,RedirectAttributes redirect){
        try{var order=held.convert(id,principal.getUser(),paymentMethod);redirect.addFlashAttribute("successMessage","Đã chốt hóa đơn thành đơn "+order.getOrderCode());return "redirect:/employee/orders/"+order.getId();}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());return "redirect:/employee/held-invoices/"+id;}
    }
    @PostMapping("/employee/held-invoices/{id}/cancel") public String cancel(@PathVariable Long id,@RequestParam(required=false) String reason,
            @AuthenticationPrincipal CustomUserDetails principal,RedirectAttributes redirect){
        try{held.cancel(id,principal.getUser(),reason);redirect.addFlashAttribute("successMessage","Đã hủy hóa đơn chờ. Không phát sinh giao dịch thanh toán.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/employee/held-invoices";
    }
}

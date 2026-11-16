package vn.shoegroup.sd11.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.repository.CustomerOrderRepository;
import vn.shoegroup.sd11.repository.PaymentRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@Controller
@RequestMapping("/payment")
public class PaymentPageController {
    private final CustomerOrderRepository orders; private final PaymentRepository payments;
    @Value("${app.payment.bank-code:MB}") private String bankCode;
    @Value("${app.payment.bank-account:}") private String bankAccount;
    @Value("${app.payment.bank-account-name:FREEDOMS SHOE STORE}") private String bankAccountName;
    @Value("${app.payment.bank-name:MB Bank}") private String bankName;
    public PaymentPageController(CustomerOrderRepository orders,PaymentRepository payments){this.orders=orders;this.payments=payments;}
    @GetMapping("/{orderId}") public String view(@PathVariable Long orderId,@AuthenticationPrincipal CustomUserDetails principal,Model model){
        var order=orders.findById(orderId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if(order.getCustomer()==null || !order.getCustomer().getId().equals(principal.getUser().getId())) throw new IllegalArgumentException("Bạn không có quyền xem thanh toán này.");
        var payment=payments.findFirstByOrder_IdOrderByCreatedAtDesc(orderId).orElseThrow();
        String qrUrl=null;
        if(bankAccount!=null&&!bankAccount.isBlank()&&"BANK_TRANSFER".equals(payment.getPaymentMethod())){
            qrUrl="https://img.vietqr.io/image/"+bankCode+"-"+bankAccount+"-compact2.png?amount="+order.getTotalAmount().longValue()
                +"&addInfo="+URLEncoder.encode(order.getOrderCode(),StandardCharsets.UTF_8)
                +"&accountName="+URLEncoder.encode(bankAccountName,StandardCharsets.UTF_8);
        }
        model.addAttribute("order",order);model.addAttribute("payment",payment);model.addAttribute("qrUrl",qrUrl);
        model.addAttribute("bankName",bankName);model.addAttribute("bankAccount",bankAccount);model.addAttribute("bankAccountName",bankAccountName);
        return "order/payment";
    }
}

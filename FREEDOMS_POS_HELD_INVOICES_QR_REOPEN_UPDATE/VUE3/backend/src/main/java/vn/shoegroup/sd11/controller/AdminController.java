package vn.shoegroup.sd11.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.shoegroup.sd11.repository.*;
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final ProductRepository products; private final CustomerOrderRepository orders; private final UserRepository users; private final PaymentRepository payments; private final HeldInvoiceRepository held;
    public AdminController(ProductRepository products,CustomerOrderRepository orders,UserRepository users,PaymentRepository payments,HeldInvoiceRepository held){this.products=products;this.orders=orders;this.users=users;this.payments=payments;this.held=held;}
    @GetMapping public String dashboard(Model model){
        model.addAttribute("productCount",products.count()); model.addAttribute("orderCount",orders.count());
        model.addAttribute("userCount",users.count()); model.addAttribute("paymentCount",payments.count());
        model.addAttribute("revenue",orders.sumCompletedRevenue());
        model.addAttribute("heldCount",held.countByStatus("OPEN"));
        model.addAttribute("recentOrders",orders.findAllByOrderByCreatedAtDesc().stream().limit(8).toList()); return "admin/dashboard";
    }
}

package vn.shoegroup.sd11.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.shoegroup.sd11.repository.CustomerOrderRepository;
import vn.shoegroup.sd11.repository.HeldInvoiceRepository;
import vn.shoegroup.sd11.repository.PaymentRepository;
import vn.shoegroup.sd11.repository.ProductRepository;
@Controller
@RequestMapping("/manager")
public class ManagerController {
    private final ProductRepository products;private final CustomerOrderRepository orders;private final PaymentRepository payments;private final HeldInvoiceRepository held;
    public ManagerController(ProductRepository products,CustomerOrderRepository orders,PaymentRepository payments,HeldInvoiceRepository held){this.products=products;this.orders=orders;this.payments=payments;this.held=held;}
    @GetMapping public String dashboard(Model model){
        model.addAttribute("productCount",products.count());model.addAttribute("orderCount",orders.count());
        model.addAttribute("paymentCount",payments.count());model.addAttribute("heldCount",held.countByStatus("OPEN"));
        model.addAttribute("revenue",orders.sumCompletedRevenue());
        model.addAttribute("recentOrders",orders.findAllByOrderByCreatedAtDesc().stream().limit(8).toList());return "admin/dashboard";
    }
}

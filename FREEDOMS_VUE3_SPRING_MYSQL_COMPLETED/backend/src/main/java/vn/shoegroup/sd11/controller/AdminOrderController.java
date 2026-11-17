package vn.shoegroup.sd11.controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.repository.CustomerOrderRepository;
import vn.shoegroup.sd11.repository.OrderStatusHistoryRepository;
import vn.shoegroup.sd11.service.OrderService;
@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {
    private final CustomerOrderRepository orders; private final OrderService orderService; private final OrderStatusHistoryRepository histories;
    public AdminOrderController(CustomerOrderRepository orders,OrderService orderService,OrderStatusHistoryRepository histories){this.orders=orders;this.orderService=orderService;this.histories=histories;}
    @GetMapping public String list(Model model){model.addAttribute("orders",orders.findAllByOrderByCreatedAtDesc());return "admin/orders";}
    @GetMapping("/{id}") public String detail(@PathVariable Long id,Model model){var o=orders.findById(id).orElseThrow();model.addAttribute("order",o);model.addAttribute("items",orderService.getItems(id));model.addAttribute("history",histories.findByOrder_IdOrderByCreatedAtAsc(id));return "admin/order-detail";}
    @PostMapping("/{id}/status") public String change(@PathVariable Long id,@RequestParam String orderStatus,@RequestParam(required=false) String note,
            @AuthenticationPrincipal CustomUserDetails principal,RedirectAttributes redirect){
        try{orderService.changeOrderStatus(id,orderStatus,principal.getUser(),note);redirect.addFlashAttribute("successMessage","Đã cập nhật trạng thái đơn hàng.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/admin/orders/"+id;
    }
}

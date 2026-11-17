package vn.shoegroup.sd11.controller;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.entity.CustomerOrder;
import vn.shoegroup.sd11.repository.CustomerOrderRepository;
import vn.shoegroup.sd11.repository.PaymentRepository;
import vn.shoegroup.sd11.repository.OrderStatusHistoryRepository;
import vn.shoegroup.sd11.service.OrderService;
@Controller
@RequestMapping("/orders")
public class OrderController {
    private final CustomerOrderRepository orders; private final PaymentRepository payments; private final OrderService orderService; private final OrderStatusHistoryRepository histories;
    public OrderController(CustomerOrderRepository orders,PaymentRepository payments,OrderService orderService,OrderStatusHistoryRepository histories){this.orders=orders;this.payments=payments;this.orderService=orderService;this.histories=histories;}
    @GetMapping public String list(@AuthenticationPrincipal CustomUserDetails principal,Model model){model.addAttribute("orders",orders.findByCustomer_IdOrderByCreatedAtDesc(principal.getUser().getId()));return "order/list";}
    @GetMapping("/{id}") public String detail(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails principal,Model model){
        CustomerOrder order=orders.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if(order.getCustomer()==null || !order.getCustomer().getId().equals(principal.getUser().getId())) throw new IllegalArgumentException("Bạn không có quyền xem đơn hàng này.");
        model.addAttribute("order",order);model.addAttribute("items",orderService.getItems(id));
        model.addAttribute("payment",payments.findFirstByOrder_IdOrderByCreatedAtDesc(id).orElse(null));
        model.addAttribute("history",histories.findByOrder_IdOrderByCreatedAtAsc(id));return "order/detail";
    }
    @PostMapping("/{id}/cancel") public String cancel(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails principal,RedirectAttributes redirect){
        try{orderService.cancelCustomerOrder(id,principal.getUser());redirect.addFlashAttribute("successMessage","Đơn hàng đã được hủy. Tồn kho đã được hoàn lại.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}
        return "redirect:/orders/"+id;
    }
    @PostMapping("/{id}/transfer-submitted") public String transferSubmitted(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails principal,RedirectAttributes redirect){
        try{orderService.markTransferSubmitted(id,principal.getUser());redirect.addFlashAttribute("successMessage","Đã gửi thông báo chuyển khoản. Nhân viên sẽ xác nhận sau khi kiểm tra tiền về.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/orders/"+id;
    }
}

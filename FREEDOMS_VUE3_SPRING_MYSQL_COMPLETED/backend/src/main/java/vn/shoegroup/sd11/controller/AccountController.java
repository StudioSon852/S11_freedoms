package vn.shoegroup.sd11.controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.shoegroup.sd11.config.CustomUserDetails;
@Controller
@RequestMapping("/account")
public class AccountController {
    @GetMapping public String account(@AuthenticationPrincipal CustomUserDetails principal,Model model){model.addAttribute("user",principal.getUser());return "store/account";}
}

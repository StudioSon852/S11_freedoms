package vn.shoegroup.sd11.controller;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.dto.RegisterForm;
import vn.shoegroup.sd11.service.AuthService;
@Controller
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService){this.authService=authService;}
    @GetMapping("/login") public String login(){return "auth/login";}
    @GetMapping("/register") public String register(Model model){model.addAttribute("registerForm",new RegisterForm());return "auth/register";}
    @PostMapping("/register") public String doRegister(@Valid @ModelAttribute RegisterForm registerForm, BindingResult result, Model model, RedirectAttributes redirect){
        if(result.hasErrors()) return "auth/register";
        try { authService.register(registerForm); redirect.addFlashAttribute("successMessage","Tạo tài khoản thành công. Hãy đăng nhập để mua hàng."); return "redirect:/login"; }
        catch(IllegalArgumentException ex){model.addAttribute("errorMessage",ex.getMessage());return "auth/register";}
    }
}

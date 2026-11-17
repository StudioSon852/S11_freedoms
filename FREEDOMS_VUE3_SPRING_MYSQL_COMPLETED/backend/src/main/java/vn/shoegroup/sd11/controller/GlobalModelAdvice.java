package vn.shoegroup.sd11.controller;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.shoegroup.sd11.entity.AppUser;
import vn.shoegroup.sd11.repository.UserRepository;
@ControllerAdvice
public class GlobalModelAdvice {
    private final UserRepository users;
    public GlobalModelAdvice(UserRepository users){this.users=users;}
    @ModelAttribute public void addCurrentUser(Model model, Authentication authentication){
        if(authentication!=null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            users.findByEmailIgnoreCase(authentication.getName()).ifPresent(u -> model.addAttribute("currentUser",u));
        }
    }
}

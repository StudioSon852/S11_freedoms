package vn.shoegroup.sd11.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping("/app")
    public String appRedirect() { return "redirect:/app/"; }

    @GetMapping("/app/")
    public String appIndex() { return "forward:/app/index.html"; }
}

package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.service.RankingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final RankingService rankingService;

    public DashboardController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalEntries", rankingService.count());
        model.addAttribute("categories", rankingService.findAllCategories());
        return "dashboard";
    }
}

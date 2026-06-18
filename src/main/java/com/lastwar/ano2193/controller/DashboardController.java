package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final RankingService rankingService;

    public DashboardController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/")
    public String index() {
        log.debug("GET / → redirecting to /dashboard");
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        log.debug("GET /login");
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.debug("GET /dashboard");
        long totalEntries = rankingService.count();
        List<String> categories = rankingService.findAllCategories();
        log.trace("dashboard model: totalEntries={}, categories={}", totalEntries, categories);
        model.addAttribute("totalEntries", totalEntries);
        model.addAttribute("categories", categories);
        return "dashboard";
    }
}

package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.service.CsvService;
import com.lastwar.ano2193.service.RankingService;
import com.lastwar.ano2193.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RankingService rankingService;
    private final CsvService csvService;

    public AdminController(UserService userService, RankingService rankingService,
                           CsvService csvService) {
        this.userService = userService;
        this.rankingService = rankingService;
        this.csvService = csvService;
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/users";
    }

    // ─── User management ──────────────────────────────────────────────────────

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String role,
                              RedirectAttributes redirectAttributes) {
        if (userService.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "Username already exists: " + username);
            return "redirect:/admin/users";
        }
        Set<String> roles = switch (role) {
            case "ADMIN"      -> Set.of("ADMIN", "SUBMITTER", "VIEWER");
            case "SUBMITTER"  -> Set.of("SUBMITTER", "VIEWER");
            default           -> Set.of("VIEWER");
        };
        userService.createUser(username, password, roles);
        redirectAttributes.addFlashAttribute("success", "User created: " + username);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "User deleted.");
        return "redirect:/admin/users";
    }

    // ─── Data management ──────────────────────────────────────────────────────

    @GetMapping("/data")
    public String data(Model model) {
        model.addAttribute("entries", rankingService.findAll());
        model.addAttribute("categories", rankingService.findAllCategories());
        return "admin/data";
    }

    @PostMapping("/data/delete/{id}")
    public String deleteEntry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        rankingService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Entry deleted.");
        return "redirect:/admin/data";
    }

    @PostMapping("/data/export")
    public String exportCsv(RedirectAttributes redirectAttributes) {
        csvService.exportRankingsToCsv();
        redirectAttributes.addFlashAttribute("success", "Data exported to CSV.");
        return "redirect:/admin/data";
    }
}

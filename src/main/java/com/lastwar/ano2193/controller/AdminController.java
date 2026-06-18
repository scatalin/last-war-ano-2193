package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.service.CsvService;
import com.lastwar.ano2193.service.RankingService;
import com.lastwar.ano2193.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

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
        log.debug("GET /admin → redirecting to /admin/users");
        return "redirect:/admin/users";
    }

    // ─── User management ──────────────────────────────────────────────────────

    @GetMapping("/users")
    public String users(Model model) {
        log.debug("GET /admin/users");
        List<?> users = userService.findAll();
        log.trace("admin/users model: userCount={}", users.size());
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String role,
                              RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/users/create username={} role={}", username, role);
        if (userService.existsByUsername(username)) {
            log.debug("createUser rejected: username '{}' already exists", username);
            redirectAttributes.addFlashAttribute("error", "Username already exists: " + username);
            return "redirect:/admin/users";
        }
        Set<String> roles = switch (role) {
            case "ADMIN"      -> Set.of("ADMIN", "SUBMITTER", "VIEWER");
            case "SUBMITTER"  -> Set.of("SUBMITTER", "VIEWER");
            default           -> Set.of("VIEWER");
        };
        log.trace("createUser: username={} resolvedRoles={}", username, roles);
        userService.createUser(username, password, roles);
        redirectAttributes.addFlashAttribute("success", "User created: " + username);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/users/delete/{}", id);
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "User deleted.");
        return "redirect:/admin/users";
    }

    // ─── Data management ──────────────────────────────────────────────────────

    @GetMapping("/data")
    public String data(Model model) {
        log.debug("GET /admin/data");
        List<?> entries = rankingService.findAll();
        List<String> categories = rankingService.findAllCategories();
        log.trace("admin/data model: entries={}, categories={}", entries.size(), categories);
        model.addAttribute("entries", entries);
        model.addAttribute("categories", categories);
        return "admin/data";
    }

    @PostMapping("/data/delete/{id}")
    public String deleteEntry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/data/delete/{}", id);
        rankingService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Entry deleted.");
        return "redirect:/admin/data";
    }

    @PostMapping("/data/export")
    public String exportCsv(RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/data/export");
        csvService.exportRankingsToCsv();
        redirectAttributes.addFlashAttribute("success", "Data exported to CSV.");
        return "redirect:/admin/data";
    }
}

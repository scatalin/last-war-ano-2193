package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.config.GoogleSheetsConfig;
import com.lastwar.ano2193.model.AppSetting;
import com.lastwar.ano2193.model.CategoryInstance;
import com.lastwar.ano2193.model.CategoryTag;
import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.model.UploadCategory;
import com.lastwar.ano2193.repository.AppSettingRepository;
import com.lastwar.ano2193.service.CategoryService;
import com.lastwar.ano2193.service.CsvService;
import com.lastwar.ano2193.service.GoogleSheetsService;
import com.lastwar.ano2193.service.RankingService;
import com.lastwar.ano2193.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final RankingService rankingService;
    private final CsvService csvService;
    private final CategoryService categoryService;
    private final GoogleSheetsService googleSheetsService;
    private final GoogleSheetsConfig googleSheetsConfig;
    private final AppSettingRepository appSettings;

    public AdminController(UserService userService, RankingService rankingService,
                           CsvService csvService, CategoryService categoryService,
                           GoogleSheetsService googleSheetsService, GoogleSheetsConfig googleSheetsConfig,
                           AppSettingRepository appSettings) {
        this.userService = userService;
        this.rankingService = rankingService;
        this.csvService = csvService;
        this.categoryService = categoryService;
        this.googleSheetsService = googleSheetsService;
        this.googleSheetsConfig = googleSheetsConfig;
        this.appSettings = appSettings;
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

    @PostMapping("/data/export-sheets")
    public String exportToSheets(RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/data/export-sheets");
        if (!googleSheetsConfig.isConfigured()) {
            redirectAttributes.addFlashAttribute("error",
                    "Google Sheets is not configured. See Admin → Google Sheets.");
            return "redirect:/admin/data";
        }
        try {
            List<RankingEntry> entries = rankingService.findAll();
            int count = googleSheetsService.exportRankings(entries);
            redirectAttributes.addFlashAttribute("success",
                    "Exported " + count + " entries to Google Sheets.");
        } catch (Exception e) {
            log.warn("Google Sheets export failed", e);
            redirectAttributes.addFlashAttribute("error", "Export failed: " + e.getMessage());
        }
        return "redirect:/admin/data";
    }

    // ─── Google Sheets settings ───────────────────────────────────────────────

    @GetMapping("/sheets")
    public String sheetsSettings(Model model) {
        log.debug("GET /admin/sheets");
        model.addAttribute("config", googleSheetsConfig);
        model.addAttribute("effectiveTab", googleSheetsService.getEffectiveSheetName());
        if (googleSheetsConfig.isConfigured()) {
            try {
                model.addAttribute("availableTabs", googleSheetsService.listSheetTabs());
            } catch (Exception e) {
                log.warn("Could not fetch sheet tabs: {}", e.getMessage());
                model.addAttribute("tabFetchError", e.getMessage());
            }
        }
        return "admin/sheets";
    }

    @PostMapping("/sheets/tab")
    public String saveSheetTab(@RequestParam String tabName, RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/sheets/tab tabName={}", tabName);
        String trimmed = tabName.trim();
        AppSetting setting = appSettings.findById("sheets.tab").orElse(new AppSetting());
        setting.setKey("sheets.tab");
        setting.setValue(trimmed);
        appSettings.save(setting);
        redirectAttributes.addFlashAttribute("success", "Target sheet tab set to: " + trimmed);
        return "redirect:/admin/sheets";
    }

    @PostMapping("/sheets/test")
    public String testSheetsConnection(RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/sheets/test");
        if (!googleSheetsConfig.isConfigured()) {
            redirectAttributes.addFlashAttribute("error",
                    "Google Sheets is not configured. Set the required environment variables first.");
            return "redirect:/admin/sheets";
        }
        try {
            String title = googleSheetsService.testConnection();
            redirectAttributes.addFlashAttribute("success",
                    "Connected! Spreadsheet title: \"" + title + "\"");
        } catch (Exception e) {
            log.warn("Google Sheets test connection failed", e);
            redirectAttributes.addFlashAttribute("error", "Connection failed: " + e.getMessage());
        }
        return "redirect:/admin/sheets";
    }

    // ─── Category management ──────────────────────────────────────────────────

    @GetMapping("/categories")
    public String categories(Model model) {
        log.debug("GET /admin/categories");
        List<UploadCategory> cats = categoryService.findAll();
        Map<Long, List<CategoryTag>> tagsByCategory = cats.stream()
                .collect(Collectors.toMap(
                        UploadCategory::getId,
                        c -> categoryService.findTagsByCategoryId(c.getId())));
        model.addAttribute("categories", cats);
        model.addAttribute("instances", categoryService.findAllInstances());
        model.addAttribute("tagsByCategory", tagsByCategory);
        return "admin/categories";
    }

    @PostMapping("/categories/create")
    public String createCategory(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/categories/create name={}", name);
        String safeName = name.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (safeName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Category name is required.");
            return "redirect:/admin/categories";
        }
        if (categoryService.existsByName(safeName)) {
            redirectAttributes.addFlashAttribute("error", "Category already exists: " + safeName);
            return "redirect:/admin/categories";
        }
        UploadCategory c = new UploadCategory();
        c.setName(safeName);
        c.setDescription(description != null ? description.trim() : null);
        categoryService.save(c);
        redirectAttributes.addFlashAttribute("success", "Category created: " + safeName);
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam(required = false) String description,
                               RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/categories/edit/{}", id);
        return categoryService.findById(id).map(c -> {
            String safeName = name.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
            if (safeName.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Category name is required.");
                return "redirect:/admin/categories";
            }
            c.setName(safeName);
            c.setDescription(description != null ? description.trim() : null);
            categoryService.save(c);
            redirectAttributes.addFlashAttribute("success", "Category updated.");
            return "redirect:/admin/categories";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Category not found.");
            return "redirect:/admin/categories";
        });
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/categories/delete/{}", id);
        categoryService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Category deleted.");
        return "redirect:/admin/categories";
    }

    // ── Category instances ────────────────────────────────────────────────────

    @PostMapping("/categories/instances/create")
    public String createInstance(
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean eternal,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(defaultValue = "false") boolean singleDate,
            @RequestParam(required = false) LocalDate endDate,
            RedirectAttributes redirectAttributes) {

        log.debug("POST /admin/categories/instances/create categoryId={} name={} eternal={}", categoryId, name, eternal);
        return categoryService.findById(categoryId).map(cat -> {
            CategoryInstance inst = new CategoryInstance();
            inst.setCategory(cat);
            inst.setName(name.trim());
            inst.setEternal(eternal);
            if (!eternal) {
                inst.setStartDate(startDate);
                inst.setEndDate(singleDate ? null : endDate);
            }
            categoryService.saveInstance(inst);
            redirectAttributes.addFlashAttribute("success",
                    "Instance created: " + inst.getDisplayName());
            return "redirect:/admin/categories";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Category not found.");
            return "redirect:/admin/categories";
        });
    }

    @PostMapping("/categories/instances/delete/{id}")
    public String deleteInstance(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/categories/instances/delete/{}", id);
        categoryService.deleteInstance(id);
        redirectAttributes.addFlashAttribute("success", "Instance deleted.");
        return "redirect:/admin/categories";
    }

    // ── Category tags ─────────────────────────────────────────────────────────

    @PostMapping("/categories/{categoryId}/tags/add")
    public String addTag(@PathVariable Long categoryId,
                         @RequestParam String name,
                         RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/categories/{}/tags/add name={}", categoryId, name);
        return categoryService.findById(categoryId).map(cat -> {
            if (categoryService.countTagsByCategoryId(categoryId) >= 10) {
                redirectAttributes.addFlashAttribute("error", "Maximum 10 tags per category.");
                return "redirect:/admin/categories";
            }
            String safeName = name.trim();
            if (safeName.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Tag name is required.");
                return "redirect:/admin/categories";
            }
            int nextOrder = (int) categoryService.countTagsByCategoryId(categoryId);
            CategoryTag tag = new CategoryTag();
            tag.setCategory(cat);
            tag.setName(safeName);
            tag.setDisplayOrder(nextOrder);
            categoryService.saveTag(tag);
            redirectAttributes.addFlashAttribute("success", "Tag added: " + safeName);
            return "redirect:/admin/categories";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Category not found.");
            return "redirect:/admin/categories";
        });
    }

    @PostMapping("/categories/tags/delete/{tagId}")
    public String deleteTag(@PathVariable Long tagId, RedirectAttributes redirectAttributes) {
        log.debug("POST /admin/categories/tags/delete/{}", tagId);
        categoryService.deleteTag(tagId);
        redirectAttributes.addFlashAttribute("success", "Tag deleted.");
        return "redirect:/admin/categories";
    }
}

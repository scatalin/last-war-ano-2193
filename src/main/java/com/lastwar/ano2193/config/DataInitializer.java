package com.lastwar.ano2193.config;

import com.lastwar.ano2193.service.CategoryService;
import com.lastwar.ano2193.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds default users and categories on first startup.
 *
 * <p>Default accounts (change passwords in production!):
 * <ul>
 *   <li>{@code admin / admin123} – ADMIN + SUBMITTER + VIEWER</li>
 *   <li>{@code submitter / sub123} – SUBMITTER + VIEWER</li>
 *   <li>{@code viewer / view123} – VIEWER only</li>
 * </ul>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final CategoryService categoryService;

    public DataInitializer(UserService userService, CategoryService categoryService) {
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @Override
    public void run(String... args) {
        createIfAbsent("admin",     "admin123",  Set.of("ADMIN", "SUBMITTER", "VIEWER"));
        createIfAbsent("submitter", "sub123",    Set.of("SUBMITTER", "VIEWER"));
        createIfAbsent("viewer",    "view123",   Set.of("VIEWER"));

        categoryService.createIfAbsent("power",        "Total combat power ranking");
        categoryService.createIfAbsent("kills",        "Total kills ranking");
        categoryService.createIfAbsent("construction", "Construction speed / points ranking");
        categoryService.createIfAbsent("daily",        "Daily ranking snapshot");
        categoryService.createIfAbsent("weekly",       "Weekly ranking snapshot");
    }

    private void createIfAbsent(String username, String password, Set<String> roles) {
        log.debug("createIfAbsent: checking username={}", username);
        if (!userService.existsByUsername(username)) {
            userService.createUser(username, password, roles);
            log.info("Created default user '{}' with roles {}", username, roles);
        } else {
            log.debug("createIfAbsent: '{}' already exists – skipping", username);
        }
    }
}

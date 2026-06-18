package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/rankings")
public class RankingController {

    private static final Logger log = LoggerFactory.getLogger(RankingController.class);

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public String rankings(@RequestParam(required = false) String category, Model model) {
        log.debug("GET /rankings category={}", category);
        boolean filtered = category != null && !category.isBlank();
        List<RankingEntry> entries = filtered
                ? rankingService.findByCategory(category)
                : rankingService.findAll();
        List<String> categories = rankingService.findAllCategories();
        log.trace("rankings result: filtered={}, entries={}, availableCategories={}",
                filtered, entries.size(), categories);
        model.addAttribute("entries", entries);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        return "rankings";
    }
}

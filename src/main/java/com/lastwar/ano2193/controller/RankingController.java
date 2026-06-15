package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.service.RankingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public String rankings(@RequestParam(required = false) String category, Model model) {
        List<RankingEntry> entries = (category != null && !category.isBlank())
                ? rankingService.findByCategory(category)
                : rankingService.findAll();

        model.addAttribute("entries", entries);
        model.addAttribute("categories", rankingService.findAllCategories());
        model.addAttribute("selectedCategory", category);
        return "rankings";
    }
}

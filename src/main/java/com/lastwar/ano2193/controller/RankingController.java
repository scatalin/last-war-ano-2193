package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.repository.PhotoUploadRepository;
import com.lastwar.ano2193.service.CategoryService;
import com.lastwar.ano2193.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/rankings")
public class RankingController {

    private static final Logger log = LoggerFactory.getLogger(RankingController.class);

    private final RankingService rankingService;
    private final CategoryService categoryService;
    private final PhotoUploadRepository photoUploadRepository;

    public RankingController(RankingService rankingService,
                             CategoryService categoryService,
                             PhotoUploadRepository photoUploadRepository) {
        this.rankingService = rankingService;
        this.categoryService = categoryService;
        this.photoUploadRepository = photoUploadRepository;
    }

    @GetMapping
    public String rankings(
            @RequestParam(required = false) Long instanceId,
            Model model) {

        log.debug("GET /rankings instanceId={}", instanceId);

        // Category definitions + instance data for the two-step picker
        model.addAttribute("categories", categoryService.findAll());
        List<Map<String, Object>> instancesJson = categoryService.findAllInstances().stream()
                .sorted(Comparator.comparing(
                        i -> i.getStartDate(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",         i.getId());
                    m.put("categoryId", i.getCategory().getId());
                    m.put("label",      i.getInstanceLabel());
                    return m;
                })
                .collect(Collectors.toList());
        model.addAttribute("instancesJson", instancesJson);

        if (instanceId == null) {
            model.addAttribute("selectedInstanceId",  null);
            model.addAttribute("selectedCategoryId",  null);
            return "rankings";
        }

        categoryService.findInstanceById(instanceId).ifPresentOrElse(inst -> {
            model.addAttribute("selectedInstance",    inst);
            model.addAttribute("selectedInstanceId",  instanceId);
            model.addAttribute("selectedCategoryId",  inst.getCategory().getId());

            List<String> filenames = photoUploadRepository
                    .findByCategoryInstanceId(instanceId).stream()
                    .map(u -> u.getFilename())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<RankingEntry> entries = rankingService
                    .findBySourcePhotoPathIn(filenames).stream()
                    .sorted(Comparator.comparing(RankingEntry::getPower,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(RankingEntry::getKills,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            log.debug("rankings: instanceId={} filenames={} entries={}", instanceId, filenames.size(), entries.size());
            model.addAttribute("entries",     entries);
            model.addAttribute("photoCount",  filenames.size());
        }, () -> {
            log.warn("rankings: instanceId={} not found", instanceId);
            model.addAttribute("selectedInstanceId", null);
            model.addAttribute("selectedCategoryId", null);
        });

        return "rankings";
    }
}
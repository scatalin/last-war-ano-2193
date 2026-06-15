package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.model.PhotoUpload;
import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.repository.PhotoUploadRepository;
import com.lastwar.ano2193.service.CsvService;
import com.lastwar.ano2193.service.ImageParsingService;
import com.lastwar.ano2193.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/upload")
public class PhotoController {

    private static final Logger log = LoggerFactory.getLogger(PhotoController.class);

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final ImageParsingService imageParsingService;
    private final RankingService rankingService;
    private final PhotoUploadRepository photoUploadRepository;
    private final CsvService csvService;

    public PhotoController(ImageParsingService imageParsingService,
                           RankingService rankingService,
                           PhotoUploadRepository photoUploadRepository,
                           CsvService csvService) {
        this.imageParsingService = imageParsingService;
        this.rankingService = rankingService;
        this.photoUploadRepository = photoUploadRepository;
        this.csvService = csvService;
    }

    @GetMapping
    public String uploadForm(Model model) {
        model.addAttribute("uploads", photoUploadRepository.findAll());
        return "upload";
    }

    @PostMapping
    public String handleUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/upload";
        }

        String safeCategory = category.replaceAll("[^A-Za-z0-9_\\-]", "_");
        PhotoUpload upload = new PhotoUpload();
        upload.setOriginalFilename(file.getOriginalFilename());
        upload.setCategory(safeCategory);
        upload.setUploadedBy(userDetails.getUsername());
        upload.setUploadedAt(LocalDateTime.now());
        upload.setStatus("PROCESSING");
        photoUploadRepository.save(upload);

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            // Use UUID to prevent path-traversal / filename collisions
            String ext = getExtension(file.getOriginalFilename());
            String storedName = UUID.randomUUID() + ext;
            Path target = dir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            upload.setFilename(storedName);

            File imageFile = target.toFile();
            List<RankingEntry> entries = imageParsingService.parseImage(
                    imageFile, safeCategory, userDetails.getUsername());
            rankingService.saveAll(entries);
            csvService.exportRankingsToCsv();

            upload.setStatus("PROCESSED");
            upload.setNotes(entries.size() + " entries extracted");
            photoUploadRepository.save(upload);

            redirectAttributes.addFlashAttribute("success",
                    "Upload complete. " + entries.size() + " entries extracted.");
        } catch (IOException e) {
            log.error("File upload error", e);
            upload.setStatus("FAILED");
            upload.setNotes(e.getMessage());
            photoUploadRepository.save(upload);
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/upload";
    }

    /** Allowed image file extensions (lower-case, including the dot). */
    private static final java.util.Set<String> ALLOWED_EXTENSIONS =
            java.util.Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff", ".tif");

    /**
     * Returns a whitelisted, lower-cased file extension derived from the original filename,
     * or an empty string when the extension is absent or not on the allow-list.
     * This prevents path-injection via a crafted filename.
     */
    private static String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = filename.substring(dot).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : "";
    }
}

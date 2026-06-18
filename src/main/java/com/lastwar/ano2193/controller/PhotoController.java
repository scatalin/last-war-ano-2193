package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.model.PhotoUpload;
import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.repository.PhotoUploadRepository;
import com.lastwar.ano2193.service.CategoryService;
import com.lastwar.ano2193.service.CsvService;
import com.lastwar.ano2193.service.ImageParsingService;
import com.lastwar.ano2193.service.RankingService;
import net.sourceforge.tess4j.TesseractException;
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

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final CategoryService categoryService;

    public PhotoController(ImageParsingService imageParsingService,
                           RankingService rankingService,
                           PhotoUploadRepository photoUploadRepository,
                           CsvService csvService,
                           CategoryService categoryService) {
        this.imageParsingService = imageParsingService;
        this.rankingService = rankingService;
        this.photoUploadRepository = photoUploadRepository;
        this.csvService = csvService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String uploadForm(Model model) {
        log.debug("GET /upload");
        List<PhotoUpload> uploads = photoUploadRepository.findAll();
        log.trace("uploadForm: pastUploadCount={}", uploads.size());
        model.addAttribute("uploads", uploads);
        model.addAttribute("categories", categoryService.findAll());

        Map<String, List<RankingEntry>> entriesByFilename = new HashMap<>();
        for (PhotoUpload u : uploads) {
            if (u.getFilename() != null) {
                entriesByFilename.put(u.getFilename(),
                        rankingService.findBySourcePhotoPath(u.getFilename()));
            }
        }
        model.addAttribute("entriesByFilename", entriesByFilename);
        return "upload";
    }

    @PostMapping
    public String handleUpload(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam("category") String category,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        List<MultipartFile> nonEmpty = files.stream()
                .filter(f -> !f.isEmpty()).toList();
        log.info("POST /upload fileCount={} category={} user={}", nonEmpty.size(), category, userDetails.getUsername());

        if (nonEmpty.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one file to upload.");
            return "redirect:/upload";
        }

        String safeCategory = category.replaceAll("[^A-Za-z0-9_\\-]", "_");
        Path dir = Paths.get(uploadDir);

        int succeeded = 0, failedCount = 0, totalEntries = 0;
        List<String> failedNames = new ArrayList<>();

        for (MultipartFile file : nonEmpty) {
            String originalFilename = file.getOriginalFilename();
            log.info("handleUpload: processing file='{}' size={}B", originalFilename, file.getSize());

            PhotoUpload upload = new PhotoUpload();
            upload.setOriginalFilename(originalFilename);
            upload.setCategory(safeCategory);
            upload.setUploadedBy(userDetails.getUsername());
            upload.setUploadedAt(LocalDateTime.now());
            upload.setStatus("PROCESSING");
            photoUploadRepository.save(upload);

            try {
                Files.createDirectories(dir);

                String ext = getExtension(originalFilename);
                String storedName = UUID.randomUUID() + ext;
                Path target = dir.resolve(storedName);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                upload.setFilename(storedName);

                String rawOcrText = null;
                List<RankingEntry> entries = Collections.emptyList();
                boolean ocrFailed = false;
                try {
                    log.info("handleUpload: starting OCR for storedName={}", storedName);
                    rawOcrText = imageParsingService.extractRawText(target.toFile());
                    entries = imageParsingService.parseOcrText(
                            rawOcrText, safeCategory, userDetails.getUsername(), storedName);
                    log.info("handleUpload: OCR complete storedName={} entriesExtracted={}", storedName, entries.size());
                } catch (TesseractException e) {
                    ocrFailed = true;
                    log.warn("handleUpload: OCR failed for '{}' — rawOcrText will be null on frontend. Cause: {}",
                            storedName, e.getMessage());
                }

                upload.setRawOcrText(rawOcrText);
                rankingService.saveAll(entries);

                if (ocrFailed) {
                    upload.setStatus("FAILED");
                    upload.setNotes("OCR failed — re-parse or add entries manually");
                } else {
                    upload.setStatus("PRE_PARSED");
                    upload.setNotes(entries.size() + " entries extracted");
                    totalEntries += entries.size();
                }
                photoUploadRepository.save(upload);
                succeeded++;

            } catch (IOException e) {
                log.error("handleUpload: IO error for file='{}'", originalFilename, e);
                upload.setStatus("FAILED");
                upload.setNotes(e.getMessage());
                photoUploadRepository.save(upload);
                failedCount++;
                failedNames.add(originalFilename);
            }
        }

        csvService.exportRankingsToCsv();
        log.info("handleUpload: batch complete succeeded={} failed={} totalEntries={}", succeeded, failedCount, totalEntries);

        if (succeeded > 0) {
            redirectAttributes.addFlashAttribute("success",
                    succeeded + " file(s) uploaded — " + totalEntries + " entries extracted.");
        }
        if (failedCount > 0) {
            redirectAttributes.addFlashAttribute("error",
                    failedCount + " file(s) failed: " + String.join(", ", failedNames));
        }
        return "redirect:/upload";
    }

    @PostMapping("/reparse/{id}")
    public String reparsePhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.debug("POST /upload/reparse/{} user={}", id, userDetails.getUsername());
        Optional<PhotoUpload> opt = photoUploadRepository.findById(id);
        if (opt.isEmpty() || opt.get().getFilename() == null) {
            log.debug("reparsePhoto: id={} not found or has no stored file", id);
            redirectAttributes.addFlashAttribute("error", "Upload not found.");
            return "redirect:/upload";
        }

        PhotoUpload upload = opt.get();
        Path filePath = Paths.get(uploadDir).resolve(upload.getFilename()).normalize();
        if (!filePath.toFile().exists()) {
            log.debug("reparsePhoto: file missing path={}", filePath);
            redirectAttributes.addFlashAttribute("error", "Stored file not found on disk.");
            return "redirect:/upload";
        }

        upload.setStatus("PROCESSING");
        photoUploadRepository.save(upload);

        try {
            log.info("reparsePhoto: deleting old entries for sourcePhotoPath={}", upload.getFilename());
            rankingService.deleteBySourcePhotoPath(upload.getFilename());

            String rawOcrText = null;
            List<RankingEntry> entries = Collections.emptyList();
            boolean ocrFailed = false;
            try {
                log.info("reparsePhoto: starting OCR for storedName={}", upload.getFilename());
                rawOcrText = imageParsingService.extractRawText(filePath.toFile());
                entries = imageParsingService.parseOcrText(
                        rawOcrText, upload.getCategory(), userDetails.getUsername(), upload.getFilename());
                log.info("reparsePhoto: OCR complete storedName={} entriesExtracted={}", upload.getFilename(), entries.size());
            } catch (TesseractException e) {
                ocrFailed = true;
                log.warn("reparsePhoto: OCR failed for '{}' — rawOcrText will be null on frontend. Cause: {}",
                        upload.getFilename(), e.getMessage());
            }

            upload.setRawOcrText(rawOcrText);
            rankingService.saveAll(entries);
            csvService.exportRankingsToCsv();

            if (ocrFailed) {
                upload.setStatus("FAILED");
                upload.setNotes("OCR failed — re-parse or add entries manually");
            } else {
                upload.setStatus("REVIEW_REQUIRED");
                upload.setNotes(entries.size() + " entries extracted");
            }
            photoUploadRepository.save(upload);

            redirectAttributes.addFlashAttribute("success",
                    "Re-parse complete. " + entries.size() + " entries extracted.");
        } catch (Exception e) {
            log.error("Re-parse error for id={}", id, e);
            upload.setStatus("FAILED");
            upload.setNotes(e.getMessage());
            photoUploadRepository.save(upload);
            redirectAttributes.addFlashAttribute("error", "Re-parse failed: " + e.getMessage());
        }
        return "redirect:/upload";
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> viewImage(@PathVariable Long id) throws IOException {
        log.debug("GET /upload/image/{}", id);
        Optional<PhotoUpload> opt = photoUploadRepository.findById(id);
        if (opt.isEmpty() || opt.get().getFilename() == null) {
            return ResponseEntity.notFound().build();
        }
        PhotoUpload upload = opt.get();
        Path file = Paths.get(uploadDir).resolve(upload.getFilename()).normalize();
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = MediaType.IMAGE_PNG_VALUE;
        // No Content-Disposition header → browser renders inline
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/file/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws IOException {
        log.debug("GET /upload/file/{}", id);
        Optional<PhotoUpload> opt = photoUploadRepository.findById(id);
        if (opt.isEmpty() || opt.get().getFilename() == null) {
            log.debug("downloadFile: id={} not found or has no stored file", id);
            return ResponseEntity.notFound().build();
        }
        PhotoUpload upload = opt.get();
        Path file = Paths.get(uploadDir).resolve(upload.getFilename()).normalize();
        log.trace("downloadFile: id={} storedPath={} originalFilename={}", id, file, upload.getOriginalFilename());
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            log.debug("downloadFile: file missing or unreadable path={}", file);
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String disposition = "attachment; filename=\"" + upload.getOriginalFilename().replace("\"", "_") + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
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
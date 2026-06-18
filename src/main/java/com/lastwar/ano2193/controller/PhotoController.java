package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.model.PhotoUpload;
import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.repository.PhotoUploadRepository;
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
        log.debug("GET /upload");
        List<PhotoUpload> uploads = photoUploadRepository.findAll();
        log.trace("uploadForm: pastUploadCount={}", uploads.size());
        model.addAttribute("uploads", uploads);

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
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.debug("POST /upload originalFilename={} category={} user={} size={}",
                file.getOriginalFilename(), category, userDetails.getUsername(), file.getSize());

        if (file.isEmpty()) {
            log.debug("handleUpload: rejected – file is empty");
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/upload";
        }

        String safeCategory = category.replaceAll("[^A-Za-z0-9_\\-]", "_");
        log.trace("handleUpload: safeCategory={}", safeCategory);
        PhotoUpload upload = new PhotoUpload();
        upload.setOriginalFilename(file.getOriginalFilename());
        upload.setCategory(safeCategory);
        upload.setUploadedBy(userDetails.getUsername());
        upload.setUploadedAt(LocalDateTime.now());
        upload.setStatus("PROCESSING");
        photoUploadRepository.save(upload);
        log.trace("handleUpload: PhotoUpload saved with id={} status=PROCESSING", upload.getId());

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            String ext = getExtension(file.getOriginalFilename());
            String storedName = UUID.randomUUID() + ext;
            Path target = dir.resolve(storedName);
            log.trace("handleUpload: storing file as target={}", target);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            upload.setFilename(storedName);

            String rawOcrText = null;
            List<RankingEntry> entries = Collections.emptyList();
            try {
                log.debug("handleUpload: running OCR for storedName={}", storedName);
                rawOcrText = imageParsingService.extractRawText(target.toFile());
                log.debug("handleUpload: OCR raw output for '{}':\n{}", storedName, rawOcrText);
                entries = imageParsingService.parseOcrText(
                        rawOcrText, safeCategory, userDetails.getUsername(), storedName);
            } catch (TesseractException e) {
                log.warn("handleUpload: OCR unavailable for '{}': {}", storedName, e.getMessage());
            }
            log.trace("handleUpload: parser returned {} entries", entries.size());

            upload.setRawOcrText(rawOcrText);
            rankingService.saveAll(entries);
            csvService.exportRankingsToCsv();

            upload.setStatus("PRE_PARSED");
            upload.setNotes(entries.size() + " entries extracted");
            photoUploadRepository.save(upload);
            log.debug("handleUpload: complete storedName={} entriesExtracted={}", storedName, entries.size());

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
            log.debug("reparsePhoto: deleting old entries for sourcePhotoPath={}", upload.getFilename());
            rankingService.deleteBySourcePhotoPath(upload.getFilename());

            String rawOcrText = null;
            List<RankingEntry> entries = Collections.emptyList();
            try {
                log.debug("reparsePhoto: running OCR for storedName={}", upload.getFilename());
                rawOcrText = imageParsingService.extractRawText(filePath.toFile());
                log.debug("reparsePhoto: OCR raw output for '{}':\n{}", upload.getFilename(), rawOcrText);
                entries = imageParsingService.parseOcrText(
                        rawOcrText, upload.getCategory(), userDetails.getUsername(), upload.getFilename());
            } catch (TesseractException e) {
                log.warn("reparsePhoto: OCR unavailable for '{}': {}", upload.getFilename(), e.getMessage());
            }
            log.trace("reparsePhoto: parser returned {} entries", entries.size());

            upload.setRawOcrText(rawOcrText);
            rankingService.saveAll(entries);
            csvService.exportRankingsToCsv();

            upload.setStatus("REVIEW_REQUIRED");
            upload.setNotes(entries.size() + " entries extracted");
            photoUploadRepository.save(upload);
            log.debug("reparsePhoto: complete storedName={} entriesExtracted={}", upload.getFilename(), entries.size());

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
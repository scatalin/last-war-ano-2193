package com.lastwar.ano2193.controller;

import com.lastwar.ano2193.model.PhotoUpload;
import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.repository.PhotoUploadRepository;
import com.lastwar.ano2193.service.CsvService;
import com.lastwar.ano2193.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class RankingEntryApiController {

    private static final Logger log = LoggerFactory.getLogger(RankingEntryApiController.class);

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final RankingService rankingService;
    private final PhotoUploadRepository photoUploadRepository;
    private final CsvService csvService;

    public RankingEntryApiController(RankingService rankingService,
                                     PhotoUploadRepository photoUploadRepository,
                                     CsvService csvService) {
        this.rankingService = rankingService;
        this.photoUploadRepository = photoUploadRepository;
        this.csvService = csvService;
    }

    @PatchMapping("/entries/{id}")
    public ResponseEntity<Map<String, Object>> updateEntry(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Optional<RankingEntry> opt = rankingService.findById(id);
        if (opt.isEmpty()) {
            log.debug("updateEntry: id={} not found", id);
            return ResponseEntity.notFound().build();
        }

        RankingEntry e = opt.get();
        if (body.containsKey("rank"))        e.setRank(toInteger(body.get("rank")));
        if (body.containsKey("playerName"))  e.setPlayerName(toStr(body.get("playerName")));
        if (body.containsKey("allianceTag")) e.setAllianceTag(toStr(body.get("allianceTag")));
        if (body.containsKey("power"))       e.setPower(toLong(body.get("power")));
        if (body.containsKey("kills"))       e.setKills(toLong(body.get("kills")));
        rankingService.save(e);
        log.debug("updateEntry: id={} updated", id);
        return ResponseEntity.ok(toMap(e));
    }

    @PostMapping("/photos/{photoId}/entries")
    public ResponseEntity<Map<String, Object>> addEntry(
            @PathVariable Long photoId,
            @AuthenticationPrincipal UserDetails user) {

        Optional<PhotoUpload> photoOpt = photoUploadRepository.findById(photoId);
        if (photoOpt.isEmpty()) {
            log.debug("addEntry: photoId={} not found", photoId);
            return ResponseEntity.notFound().build();
        }

        PhotoUpload photo = photoOpt.get();
        List<RankingEntry> existing = rankingService.findBySourcePhotoPath(photo.getFilename());
        int nextRank = existing.stream()
                .mapToInt(entry -> entry.getRank() != null ? entry.getRank() : 0)
                .max().orElse(0) + 1;

        RankingEntry entry = new RankingEntry();
        entry.setRank(nextRank);
        entry.setSourcePhotoPath(photo.getFilename());
        entry.setCategory(photo.getCategory());
        entry.setSubmittedBy(user.getUsername());
        entry.setCapturedAt(LocalDateTime.now());
        rankingService.save(entry);
        csvService.exportRankingsToCsv();
        log.debug("addEntry: photoId={} new entryId={} rank={}", photoId, entry.getId(), nextRank);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(entry));
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        Optional<RankingEntry> opt = rankingService.findById(id);
        if (opt.isEmpty()) {
            log.debug("deleteEntry: id={} not found", id);
            return ResponseEntity.notFound().build();
        }
        rankingService.delete(id);
        csvService.exportRankingsToCsv();
        log.debug("deleteEntry: id={} deleted", id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long photoId) {
        Optional<PhotoUpload> opt = photoUploadRepository.findById(photoId);
        if (opt.isEmpty()) {
            log.debug("deletePhoto: photoId={} not found", photoId);
            return ResponseEntity.notFound().build();
        }
        PhotoUpload photo = opt.get();
        if (photo.getFilename() != null) {
            rankingService.deleteBySourcePhotoPath(photo.getFilename());
            try {
                Path file = Paths.get(uploadDir).resolve(photo.getFilename()).normalize();
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("deletePhoto: could not delete file for photoId={}: {}", photoId, e.getMessage());
            }
        }
        photoUploadRepository.delete(photo);
        csvService.exportRankingsToCsv();
        log.debug("deletePhoto: photoId={} deleted", photoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/photos/{photoId}/approve")
    public ResponseEntity<Map<String, Object>> approveReview(@PathVariable Long photoId) {
        Optional<PhotoUpload> opt = photoUploadRepository.findById(photoId);
        if (opt.isEmpty()) {
            log.debug("approveReview: photoId={} not found", photoId);
            return ResponseEntity.notFound().build();
        }
        PhotoUpload photo = opt.get();
        photo.setStatus("APPROVED");
        photoUploadRepository.save(photo);
        csvService.exportRankingsToCsv();
        log.debug("approveReview: photoId={} approved", photoId);
        return ResponseEntity.ok(Map.of("status", "APPROVED", "photoId", photoId));
    }

    private static Map<String, Object> toMap(RankingEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("rank", e.getRank());
        m.put("playerName", e.getPlayerName());
        m.put("allianceTag", e.getAllianceTag());
        m.put("power", e.getPower());
        m.put("kills", e.getKills());
        m.put("capturedAt", e.getCapturedAt() != null ? e.getCapturedAt().toString() : null);
        return m;
    }

    private static Integer toInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        String s = v.toString().replaceAll("[^0-9]", "");
        return s.isEmpty() ? null : Integer.parseInt(s);
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        String s = v.toString().replaceAll("[^0-9]", "");
        return s.isEmpty() ? null : Long.parseLong(s);
    }

    private static String toStr(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
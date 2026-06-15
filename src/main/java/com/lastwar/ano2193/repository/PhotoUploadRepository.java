package com.lastwar.ano2193.repository;

import com.lastwar.ano2193.model.PhotoUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PhotoUploadRepository extends JpaRepository<PhotoUpload, Long> {

    List<PhotoUpload> findByUploadedByOrderByUploadedAtDesc(String uploadedBy);

    List<PhotoUpload> findByStatus(String status);
}

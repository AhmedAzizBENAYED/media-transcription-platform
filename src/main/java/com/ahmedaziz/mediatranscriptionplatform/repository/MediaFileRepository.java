package com.ahmedaziz.mediatranscriptionplatform.repository;


import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByStatus(MediaFile.ProcessingStatus status);

    Page<MediaFile> findByStatus(MediaFile.ProcessingStatus status, Pageable pageable);

    @Query("SELECT m FROM MediaFile m WHERE m.status = :status AND m.retryCount < :maxRetries")
    List<MediaFile> findFailedFilesForRetry(MediaFile.ProcessingStatus status, int maxRetries);

    @Query("SELECT m FROM MediaFile m WHERE m.status = :status AND m.uploadedAt BETWEEN :startDate AND :endDate")
    List<MediaFile> findByStatusAndDateRange(MediaFile.ProcessingStatus status,
                                             LocalDateTime startDate,
                                             LocalDateTime endDate);

    @Query("SELECT COUNT(m) FROM MediaFile m WHERE m.status = :status")
    Long countByStatus(MediaFile.ProcessingStatus status);

    Optional<MediaFile> findByFilename(String filename);
}

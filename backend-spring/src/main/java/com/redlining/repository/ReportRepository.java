package com.redlining.repository;

import com.redlining.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

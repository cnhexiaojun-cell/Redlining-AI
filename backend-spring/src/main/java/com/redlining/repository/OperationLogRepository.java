package com.redlining.repository;

import com.redlining.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findByUserId(Long userId, Pageable pageable);

    Page<OperationLog> findByModule(String module, Pageable pageable);

    Page<OperationLog> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);
}

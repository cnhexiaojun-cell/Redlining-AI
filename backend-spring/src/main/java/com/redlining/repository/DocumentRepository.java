package com.redlining.repository;

import com.redlining.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Document> findByIdAndUserId(String id, Long userId);
}

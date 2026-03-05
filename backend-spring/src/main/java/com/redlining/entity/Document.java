package com.redlining.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents", indexes = {
    @Index(columnList = "user_id")
})
public class Document {

    @Id
    @Column(updatable = false, nullable = false, length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "minio_key", nullable = false, length = 512)
    private String minioKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMinioKey() {
        return minioKey;
    }

    public void setMinioKey(String minioKey) {
        this.minioKey = minioKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

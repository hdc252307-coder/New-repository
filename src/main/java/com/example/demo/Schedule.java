package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SCHEDULE")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // スケジュール名は機微情報を含みやすいため、DBには暗号化して保存する。
    @Convert(converter = StringEncryptConverter.class)
    @Column(length = 1024)
    private String title;

    // 説明文も同様に暗号化して、DB上での平文露出を防ぐ。
    @Convert(converter = StringEncryptConverter.class)
    @Column(length = 5000)
    private String description;

    // DB 既存制約に合わせて、SCHEDULE.username 列は APP_USER.id を参照する。
    // 列名は username のままだが実体はユーザーIDの外部キーとして扱う。
    @ManyToOne
    @JoinColumn(name = "username", referencedColumnName = "id")
    private User user;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Schedule() {}

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


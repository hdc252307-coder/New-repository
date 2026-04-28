package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Trash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;      // 所有者
    private String type;          // "task" or "schedule"
    private Long originalId;      // 元のID
    // ゴミ箱内データも復元時まで保護できるよう、暗号化して保存する。
    @Convert(converter = StringEncryptConverter.class)
    @Column(length = 1024)
    private String title;         // 表示用タイトル
    private LocalDateTime deletedAt;

    // ▼ Task の復元に必要なフィールド
    private LocalDate dueDate;
    private Integer priority;
    private Integer estimatedMinutes;
    @Convert(converter = StringEncryptConverter.class)
    @Column(length = 5000)
    private String description;
    private Boolean done;
    private LocalDateTime taskCompletedAt;

    // ★ 完全復元用（Task）
    private LocalDateTime taskCreatedAt;
    private LocalDateTime taskUpdatedAt;

    // ▼ Schedule の復元に必要なフィールド
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    // ★ 完全復元用（Schedule）
    private LocalDateTime scheduleCreatedAt;
    private LocalDateTime scheduleUpdatedAt;

    // --- Getter / Setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getOriginalId() { return originalId; }
    public void setOriginalId(Long originalId) { this.originalId = originalId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getDone() { return done; }
    public void setDone(Boolean done) { this.done = done; }

    public LocalDateTime getTaskCompletedAt() { return taskCompletedAt; }
    public void setTaskCompletedAt(LocalDateTime taskCompletedAt) { this.taskCompletedAt = taskCompletedAt; }

    public LocalDateTime getTaskCreatedAt() { return taskCreatedAt; }
    public void setTaskCreatedAt(LocalDateTime taskCreatedAt) { this.taskCreatedAt = taskCreatedAt; }

    public LocalDateTime getTaskUpdatedAt() { return taskUpdatedAt; }
    public void setTaskUpdatedAt(LocalDateTime taskUpdatedAt) { this.taskUpdatedAt = taskUpdatedAt; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public LocalDateTime getScheduleCreatedAt() { return scheduleCreatedAt; }
    public void setScheduleCreatedAt(LocalDateTime scheduleCreatedAt) { this.scheduleCreatedAt = scheduleCreatedAt; }

    public LocalDateTime getScheduleUpdatedAt() { return scheduleUpdatedAt; }
    public void setScheduleUpdatedAt(LocalDateTime scheduleUpdatedAt) { this.scheduleUpdatedAt = scheduleUpdatedAt; }
}

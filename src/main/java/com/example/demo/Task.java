package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TASK")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    // ユーザーごとのプライバシー保護のため、DB保存時は暗号化して保持する。
    @Convert(converter = StringEncryptConverter.class)
    @Column(length = 1024)
    private String title;

    private LocalDate dueDate;

    private Integer priority;

    private Integer estimatedMinutes;

    // 詳細テキストは平文だと漏えいリスクが高いため、透過暗号化して保存する。
    @Convert(converter = StringEncryptConverter.class)
    @Column(length = 5000)
    private String description;

    private Boolean done;
    private LocalDateTime completedAt;

    /** 表示色プリセット（default / blue / teal …）。暗号化不要。 */
    @Column(name = "color_key", length = 32)
    private String colorKey;

    // ★ LocalDateTime に統一（ここが重要）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task() {}

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

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

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getColorKey() { return colorKey; }
    public void setColorKey(String colorKey) { this.colorKey = colorKey; }
}

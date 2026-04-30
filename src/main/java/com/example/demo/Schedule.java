package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SCHEDULE")
public class Schedule {

    public static final String RECURRENCE_NONE = "none";
    public static final String RECURRENCE_WEEKLY = "weekly";
    public static final String RECURRENCE_MONTHLY = "monthly";

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

    @Column(name = "all_day")
    private Boolean allDay;

    @Column(name = "recurrence_type", length = 16)
    private String recurrenceType;

    @Column(name = "recurrence_interval")
    private Integer recurrenceInterval;

    @Column(name = "recurrence_until")
    private LocalDateTime recurrenceUntil;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Schedule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Boolean getAllDay() { return allDay; }
    public void setAllDay(Boolean allDay) { this.allDay = allDay; }

    public String getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(String recurrenceType) { this.recurrenceType = recurrenceType; }

    public Integer getRecurrenceInterval() { return recurrenceInterval; }
    public void setRecurrenceInterval(Integer recurrenceInterval) { this.recurrenceInterval = recurrenceInterval; }

    public LocalDateTime getRecurrenceUntil() { return recurrenceUntil; }
    public void setRecurrenceUntil(LocalDateTime recurrenceUntil) { this.recurrenceUntil = recurrenceUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isRecurring() {
        return RECURRENCE_WEEKLY.equals(recurrenceType) || RECURRENCE_MONTHLY.equals(recurrenceType);
    }
}


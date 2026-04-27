package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // User で検索（Service 用）
    List<Schedule> findByUser(User user);

    // 月カレンダー用（開始日時が月内にあるもの）
    List<Schedule> findByUserUsernameAndStartDateTimeBetween(
        String username,
        LocalDateTime start,
        LocalDateTime end
);

}

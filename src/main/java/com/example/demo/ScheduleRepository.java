package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 期間と重なるスケジュール（rangeEnd は排他的）。
     * 月をまたぐ終日・時刻指定の両方を拾う。
     */
    @Query("SELECT s FROM Schedule s WHERE s.user.username = :username "
            + "AND s.startDateTime < :rangeEnd AND s.endDateTime >= :rangeStart")
    List<Schedule> findOverlapping(
            @Param("username") String username,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );

    List<Schedule> findByEndDateTimeBefore(LocalDateTime cutoff);

}

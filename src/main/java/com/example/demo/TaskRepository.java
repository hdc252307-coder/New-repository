package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // ★ deleted フラグ廃止 → findByUsername のみで OK
    List<Task> findByUsername(String username);

    // 月カレンダー用
    List<Task> findByUsernameAndDueDateBetween(
            String username,
            LocalDate start,
            LocalDate end
    );

    // おすすめタスク用（未完了タスク）
    List<Task> findByUsernameAndDoneFalse(String username);

    // 1日のタスク取得（DayController 用）
    List<Task> findByUsernameAndDueDate(String username, LocalDate dueDate);
}

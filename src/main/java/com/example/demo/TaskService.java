package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

// ★ 追加：Map を使うための import
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    /**
     * おすすめタスクを3件返す。
     * ・priority が高いほど加点
     * ・締切が近いほど加点
     * ・estimatedMinutes が短いほど加点
     * ・dueDate が null の場合はスコア低めに設定
     */
    public List<Task> getRecommendedTasks(String username) 
    
    {
        List<Task> tasks = taskRepository.findByUsernameAndDoneFalse(username);
        LocalDate today = LocalDate.now();

        return tasks.stream()
                .map(t -> {
                    double score = calculateScore(t, today);
                    return new TaskScore(t, score);
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(3)
                .map(TaskScore::task)
                .toList();
    }

    /**
     * スコア計算ロジック（NPE防止・暴走防止済み）
     */
    private double calculateScore(Task t, LocalDate today) 
    
    {
        double score = t.getPriority() * 10;

        // dueDate が null の場合は締切要素を弱めにする
        if (t.getDueDate() != null) 
        
        
        {
            long daysLeft = ChronoUnit.DAYS.between(today, t.getDueDate());

            // 過去日付は「締切超過」として強めに加点（ただし暴走防止）
            if (daysLeft < 0) 
            
            
            {
                score += 40; // 締切超過タスクは優先度高い
            } else if (daysLeft <= 7) 
            {
                score += (7 - daysLeft) * 5; // 近いほど加点
            }
        } else {
            // dueDate が無いタスクは優先度低め
            score -= 10;
        }

        // estimatedMinutes が短いほど加点
        score -= (t.getEstimatedMinutes() / 30.0);

        return score;
    }

    /**
     * ★ カレンダー用：日付ごとにタスクをまとめる
     * String → List<Task> の Map を返す
     * calendar.html の taskMap[date.toString()] で使われる
     */
    public Map<String, List<Task>> mapTasksByDate(List<Task> tasks) 
    
    {
        Map<String, List<Task>> map = new HashMap<>();

        for (Task task : tasks) 

        

        

        {
            LocalDate date = task.getDueDate();
            if (date == null) continue; // null 安全

            String key = date.toString(); // ★ LocalDate → String に統一

            map.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
        }

        return map;
    }

    /**
     * Task とスコアをまとめる record（Java 16+）
     */
    private record TaskScore(Task task, double score) {}
}

package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    /**
     * 今日やるべきおすすめタスクを最大3件返す。
     * 1) 期限が今日以前（期限切れ + 今日期限）を優先
     * 2) 同条件では priority が高い順
     * 3) 3件に満たない場合は、将来期限が近い順で補完
     */
    public List<Task> getRecommendedTasks(String username) {
        List<Task> tasks = taskRepository.findByUsernameAndDoneFalse(username);
        LocalDate today = LocalDate.now();
        Comparator<Task> comparator = recommendedComparator(today);

        List<Task> mustDoToday = tasks.stream()
                .filter(t -> t.getDueDate() != null && !t.getDueDate().isAfter(today))
                .sorted(comparator)
                .limit(3)
                .toList();

        if (mustDoToday.size() >= 3) {
            return mustDoToday;
        }

        List<Task> upcoming = tasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isAfter(today))
                .sorted(comparator)
                .limit(3 - mustDoToday.size())
                .toList();

        List<Task> recommended = new ArrayList<>(mustDoToday);
        recommended.addAll(upcoming);
        return recommended;
    }

    private Comparator<Task> recommendedComparator(LocalDate today) {
        return Comparator
                .comparingInt((Task t) -> {
                    LocalDate due = t.getDueDate();
                    if (due == null) return 3;
                    if (due.isBefore(today)) return 0;
                    if (due.isEqual(today)) return 1;
                    return 2;
                })
                .thenComparing(Task::getPriority, Comparator.reverseOrder())
                .thenComparing(
                        Task::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * ★ カレンダー用：日付ごとにタスクをまとめる
     * String → List<Task> の Map を返す
     * calendar.html の taskMap[date.toString()] で使われる
     */
    public Map<String, List<Task>> mapTasksByDate(List<Task> tasks) {
        Map<String, List<Task>> map = new HashMap<>();

        for (Task task : tasks) {
            LocalDate date = task.getDueDate();
            if (date == null) continue; // null 安全

            String key = date.toString(); // ★ LocalDate → String に統一

            map.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
        }

        return map;
    }

    /**
     * 期限なしタスクを「作成日」のセルにだけ載せる（帯表示と重複させない）。
     */
    public Map<String, List<Task>> mapUndatedTasksByCreatedDay(List<Task> tasks, LocalDate gridStart, LocalDate gridEnd) {
        Map<String, List<Task>> map = new HashMap<>();
        for (Task task : tasks) {
            if (task.getDueDate() != null) {
                continue;
            }
            if (task.getCreatedAt() == null) {
                continue;
            }
            LocalDate cd = task.getCreatedAt().toLocalDate();
            if (cd.isBefore(gridStart) || cd.isAfter(gridEnd)) {
                continue;
            }
            map.computeIfAbsent(cd.toString(), k -> new ArrayList<>()).add(task);
        }
        return map;
    }

    /**
     * 未完了かつ期限ありタスクを「作成日〜期日」で週ごとに分割し、同週内は 7 列グリッドの 1 本の帯として繋げる。
     * {@code dates} は表示グリッド先頭からの連続日（通常 35 日）。
     */
    public List<List<TaskCalendarBandPlacement>> buildTaskBandPlacementsByWeek(List<Task> tasks, List<LocalDate> dates) {
        int n = dates.size();
        if (n == 0) {
            return List.of();
        }
        LocalDate gridStart = dates.get(0);
        LocalDate gridEnd = dates.get(n - 1);
        int numWeeks = (n + 6) / 7;

        List<List<RawBand>> rawByWeek = new ArrayList<>();
        for (int w = 0; w < numWeeks; w++) {
            rawByWeek.add(new ArrayList<>());
        }

        for (Task t : tasks) {
            if (Boolean.TRUE.equals(t.getDone())) {
                continue;
            }
            if (t.getDueDate() == null || t.getCreatedAt() == null) {
                continue;
            }
            LocalDate c = t.getCreatedAt().toLocalDate();
            LocalDate d = t.getDueDate();
            if (d.isBefore(gridStart) || c.isAfter(gridEnd)) {
                continue;
            }
            LocalDate es = c.isBefore(gridStart) ? gridStart : c;
            LocalDate ee = d.isAfter(gridEnd) ? gridEnd : d;
            String color = TaskColorKeys.normalize(t.getColorKey());

            for (int w = 0; w < numWeeks; w++) {
                int idx0 = w * 7;
                int idx6 = Math.min(idx0 + 6, n - 1);
                LocalDate weekStart = dates.get(idx0);
                LocalDate weekEnd = dates.get(idx6);
                LocalDate segS = es.isBefore(weekStart) ? weekStart : es;
                LocalDate segE = ee.isAfter(weekEnd) ? weekEnd : ee;
                if (segS.isAfter(segE)) {
                    continue;
                }
                int idxS = (int) ChronoUnit.DAYS.between(gridStart, segS);
                int idxE = (int) ChronoUnit.DAYS.between(gridStart, segE);
                int colStart = idxS % 7;
                int colEnd = idxE % 7;
                rawByWeek.get(w).add(new RawBand(
                        t.getId(), t.getTitle(), color, colStart, colEnd, t.getCreatedAt()));
            }
        }

        List<List<TaskCalendarBandPlacement>> out = new ArrayList<>();
        for (int w = 0; w < numWeeks; w++) {
            out.add(assignLanes(rawByWeek.get(w)));
        }
        return out;
    }

    private static List<TaskCalendarBandPlacement> assignLanes(List<RawBand> raw) {
        if (raw.isEmpty()) {
            return List.of();
        }
        raw.sort(Comparator
                .comparingInt((RawBand r) -> r.colStart)
                .thenComparing(r -> r.createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(r -> r.colEnd - r.colStart));

        List<Integer> laneLastEnd = new ArrayList<>();
        List<TaskCalendarBandPlacement> placed = new ArrayList<>();

        for (RawBand seg : raw) {
            int chosenLane = -1;
            for (int L = 0; L < laneLastEnd.size(); L++) {
                if (seg.colStart > laneLastEnd.get(L)) {
                    chosenLane = L;
                    laneLastEnd.set(L, seg.colEnd);
                    break;
                }
            }
            if (chosenLane < 0) {
                laneLastEnd.add(seg.colEnd);
                chosenLane = laneLastEnd.size() - 1;
            }
            placed.add(new TaskCalendarBandPlacement(
                    seg.taskId, seg.title, seg.colorKey, seg.colStart, seg.colEnd, chosenLane));
        }
        return placed;
    }

    private static final class RawBand {
        final Long taskId;
        final String title;
        final String colorKey;
        final int colStart;
        final int colEnd;
        final LocalDateTime createdAt;

        RawBand(Long taskId, String title, String colorKey, int colStart, int colEnd, LocalDateTime createdAt) {
            this.taskId = taskId;
            this.title = title;
            this.colorKey = colorKey;
            this.colStart = colStart;
            this.colEnd = colEnd;
            this.createdAt = createdAt;
        }
    }
}

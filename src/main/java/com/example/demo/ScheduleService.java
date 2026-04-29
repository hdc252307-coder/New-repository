package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    // 一覧取得（ユーザー紐づけ）
    public List<Schedule> findByUser(User user) 
    
    {
        return scheduleRepository.findByUser(user);
    }

    // 新規作成
    public Schedule createSchedule(
            User user,
            String title,
            String description,
            LocalDateTime start,
            LocalDateTime end
    ) 
    
    {
        Schedule schedule = new Schedule();
        schedule.setUser(user);
        schedule.setTitle(title);
        schedule.setDescription(description);
        schedule.setStartDateTime(start);
        schedule.setEndDateTime(end);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());

        return scheduleRepository.save(schedule);
    }

    // 更新
    public Schedule updateSchedule(
            Long id,
            String title,
            String description,
            LocalDateTime start,
            LocalDateTime end
    ) 
    
    {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        schedule.setTitle(title);
        schedule.setDescription(description);
        schedule.setStartDateTime(start);
        schedule.setEndDateTime(end);
        schedule.setUpdatedAt(LocalDateTime.now());

        return scheduleRepository.save(schedule);
    }

    // 1件取得
    public Schedule findById(Long id) 
    
    {
        return scheduleRepository.findById(id).orElse(null);
    }

    /**
     * カレンダー用：日付ごとにスケジュールをまとめる
     */
    public Map<String, List<Schedule>> mapSchedulesByDate(List<Schedule> schedules) 
    
    {
        Map<String, List<Schedule>> map = new HashMap<>();

        for (Schedule s : schedules) {
            if (s.getStartDateTime() == null || s.getEndDateTime() == null) {
                continue;
            }

            LocalDate start = s.getStartDateTime().toLocalDate();
            LocalDate end = ScheduleAllDaySupport.toInclusiveEndDate(
                    s.getStartDateTime(), s.getEndDateTime(), s.getAllDay());
            if (end.isBefore(start)) {
                continue;
            }
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                String key = d.toString();
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }
        }

        return map;
    }

    /**
     * 予定を「開始日〜終了日（日単位）」で週ごとに分割し、同週内は 7 列グリッドの 1 本の帯として繋げる。
     * {@code dates} は表示グリッド先頭からの連続日（通常 35 日）。
     */
    public List<List<ScheduleCalendarBandPlacement>> buildScheduleBandPlacementsByWeek(
            List<Schedule> schedules,
            List<LocalDate> dates
    ) {
        int n = dates.size();
        if (n == 0) {
            return List.of();
        }
        LocalDate gridStart = dates.get(0);
        LocalDate gridEnd = dates.get(n - 1);
        int numWeeks = (n + 6) / 7;

        List<List<RawScheduleBand>> rawByWeek = new ArrayList<>();
        for (int w = 0; w < numWeeks; w++) {
            rawByWeek.add(new ArrayList<>());
        }

        for (Schedule s : schedules) {
            if (s.getStartDateTime() == null || s.getEndDateTime() == null) {
                continue;
            }
            LocalDateTime st = s.getStartDateTime();
            LocalDateTime en = s.getEndDateTime();
            if (en.isBefore(st)) {
                continue;
            }
            LocalDate c = st.toLocalDate();
            LocalDate d = ScheduleAllDaySupport.toInclusiveEndDate(st, en, s.getAllDay());
            if (d.isBefore(c)) {
                continue;
            }
            if (d.isBefore(gridStart) || c.isAfter(gridEnd)) {
                continue;
            }
            LocalDate es = c.isBefore(gridStart) ? gridStart : c;
            LocalDate ee = d.isAfter(gridEnd) ? gridEnd : d;

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
                rawByWeek.get(w).add(new RawScheduleBand(
                        s.getId(), s.getTitle(), colStart, colEnd, s.getCreatedAt()));
            }
        }

        List<List<ScheduleCalendarBandPlacement>> out = new ArrayList<>();
        for (int w = 0; w < numWeeks; w++) {
            out.add(assignScheduleLanes(rawByWeek.get(w)));
        }
        return out;
    }

    private static List<ScheduleCalendarBandPlacement> assignScheduleLanes(List<RawScheduleBand> raw) {
        if (raw.isEmpty()) {
            return List.of();
        }
        raw.sort(Comparator.comparingInt((RawScheduleBand r) -> r.colStart)
                .thenComparing(r -> r.createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(r -> r.colEnd - r.colStart));

        List<Integer> laneLastEnd = new ArrayList<>();
        List<ScheduleCalendarBandPlacement> placed = new ArrayList<>();

        for (RawScheduleBand seg : raw) {
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
            placed.add(new ScheduleCalendarBandPlacement(
                    seg.scheduleId, seg.title, seg.colStart, seg.colEnd, chosenLane));
        }
        return placed;
    }

    private static final class RawScheduleBand {
        final Long scheduleId;
        final String title;
        final int colStart;
        final int colEnd;
        final LocalDateTime createdAt;

        RawScheduleBand(Long scheduleId, String title, int colStart, int colEnd, LocalDateTime createdAt) {
            this.scheduleId = scheduleId;
            this.title = title;
            this.colStart = colStart;
            this.colEnd = colEnd;
            this.createdAt = createdAt;
        }
    }
}

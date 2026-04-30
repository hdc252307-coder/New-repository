package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
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

    public List<Schedule> findExpandedOverlapping(
            String username,
            LocalDateTime rangeStart,
            LocalDateTime rangeEndExclusive
    ) {
        List<Schedule> baseSchedules = scheduleRepository.findByUserUsername(username);
        return expandSchedulesForRange(baseSchedules, rangeStart, rangeEndExclusive);
    }

    public List<Schedule> expandSchedulesForRange(
            List<Schedule> schedules,
            LocalDateTime rangeStart,
            LocalDateTime rangeEndExclusive
    ) {
        List<Schedule> expanded = new ArrayList<>();
        for (Schedule s : schedules) {
            if (s.getStartDateTime() == null || s.getEndDateTime() == null) {
                continue;
            }
            LocalDateTime baseStart = s.getStartDateTime();
            LocalDateTime baseEnd = s.getEndDateTime();
            if (!s.isRecurring()) {
                if (overlaps(baseStart, baseEnd, rangeStart, rangeEndExclusive)) {
                    expanded.add(s);
                }
                continue;
            }

            String recurrenceType = normalizeRecurrenceType(s.getRecurrenceType());
            int interval = normalizeRecurrenceInterval(s.getRecurrenceInterval());
            LocalDateTime recurrenceLimit = s.getRecurrenceUntil();
            if (recurrenceLimit == null) {
                recurrenceLimit = rangeEndExclusive.plusMonths(12);
            }

            boolean allDay = Boolean.TRUE.equals(s.getAllDay());
            long allDaySpanDays = 0;
            Duration timedDuration = Duration.ZERO;
            if (allDay) {
                LocalDate inclusiveEnd = ScheduleAllDaySupport.toInclusiveEndDate(baseStart, baseEnd, true);
                allDaySpanDays = ChronoUnit.DAYS.between(baseStart.toLocalDate(), inclusiveEnd);
            } else {
                timedDuration = Duration.between(baseStart, baseEnd);
            }

            LocalDateTime occStart = baseStart;
            int anchorDayOfMonth = baseStart.getDayOfMonth();
            LocalTime anchorTime = baseStart.toLocalTime();
            int monthlyStep = 0;
            int guard = 0;
            while (!occStart.isAfter(recurrenceLimit) && occStart.isBefore(rangeEndExclusive) && guard < 1000) {
                LocalDateTime occEnd;
                if (allDay) {
                    LocalDate occEndDate = occStart.toLocalDate().plusDays(allDaySpanDays);
                    occEnd = occEndDate.atTime(LocalTime.MAX);
                } else {
                    occEnd = occStart.plus(timedDuration);
                }
                if (overlaps(occStart, occEnd, rangeStart, rangeEndExclusive)) {
                    expanded.add(cloneAsOccurrence(s, occStart, occEnd));
                }
                if (Schedule.RECURRENCE_WEEKLY.equals(recurrenceType)) {
                    occStart = occStart.plusWeeks(interval);
                } else if (Schedule.RECURRENCE_MONTHLY.equals(recurrenceType)) {
                    monthlyStep += interval;
                    YearMonth targetMonth = YearMonth.from(baseStart).plusMonths(monthlyStep);
                    int day = Math.min(anchorDayOfMonth, targetMonth.lengthOfMonth());
                    occStart = LocalDateTime.of(targetMonth.atDay(day), anchorTime);
                } else {
                    break;
                }
                guard++;
            }
        }
        return expanded;
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
        schedule.setRecurrenceType(Schedule.RECURRENCE_NONE);
        schedule.setRecurrenceInterval(1);
        schedule.setRecurrenceUntil(null);
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
                        s.getId(), s.getTitle(), s.getStartDateTime().toLocalDate().toString(),
                        colStart, colEnd, s.getCreatedAt()));
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
                    seg.scheduleId, seg.title, seg.occurrenceStartDate, seg.colStart, seg.colEnd, chosenLane));
        }
        return placed;
    }

    private static final class RawScheduleBand {
        final Long scheduleId;
        final String title;
        final String occurrenceStartDate;
        final int colStart;
        final int colEnd;
        final LocalDateTime createdAt;

        RawScheduleBand(
                Long scheduleId,
                String title,
                String occurrenceStartDate,
                int colStart,
                int colEnd,
                LocalDateTime createdAt
        ) {
            this.scheduleId = scheduleId;
            this.title = title;
            this.occurrenceStartDate = occurrenceStartDate;
            this.colStart = colStart;
            this.colEnd = colEnd;
            this.createdAt = createdAt;
        }
    }

    public static String normalizeRecurrenceType(String recurrenceType) {
        if (Schedule.RECURRENCE_WEEKLY.equals(recurrenceType)) {
            return Schedule.RECURRENCE_WEEKLY;
        }
        if (Schedule.RECURRENCE_MONTHLY.equals(recurrenceType)) {
            return Schedule.RECURRENCE_MONTHLY;
        }
        return Schedule.RECURRENCE_NONE;
    }

    public static int normalizeRecurrenceInterval(Integer interval) {
        if (interval == null || interval <= 0) {
            return 1;
        }
        return interval;
    }

    private static boolean overlaps(
            LocalDateTime start,
            LocalDateTime end,
            LocalDateTime rangeStart,
            LocalDateTime rangeEndExclusive
    ) {
        return start.isBefore(rangeEndExclusive) && !end.isBefore(rangeStart);
    }

    private static Schedule cloneAsOccurrence(Schedule source, LocalDateTime occStart, LocalDateTime occEnd) {
        Schedule clone = new Schedule();
        clone.setId(source.getId());
        clone.setUser(source.getUser());
        clone.setTitle(source.getTitle());
        clone.setDescription(source.getDescription());
        clone.setStartDateTime(occStart);
        clone.setEndDateTime(occEnd);
        clone.setAllDay(source.getAllDay());
        clone.setRecurrenceType(source.getRecurrenceType());
        clone.setRecurrenceInterval(source.getRecurrenceInterval());
        clone.setRecurrenceUntil(source.getRecurrenceUntil());
        clone.setCreatedAt(source.getCreatedAt());
        clone.setUpdatedAt(source.getUpdatedAt());
        return clone;
    }
}

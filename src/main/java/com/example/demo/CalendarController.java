package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.example.demo.security.MyUserDetails;

@Controller
public class CalendarController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/calendar")
    public String calendar(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            Model model
    ) 
    
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();

        LocalDate today = LocalDate.now();
        if (year == null) year = today.getYear();
        if (month == null) month = today.getMonthValue();

        YearMonth yearMonth = YearMonth.of(year, month);

        YearMonth prev = yearMonth.minusMonths(1);
        YearMonth next = yearMonth.plusMonths(1);

        List<LocalDate> dates = new ArrayList<>();
        LocalDate firstDay = yearMonth.atDay(1);
        int dayOfWeek = firstDay.getDayOfWeek().getValue();
        LocalDate start = firstDay.minusDays(dayOfWeek % 7);
        int daySlots = 35;
        LocalDate gridEnd35 = start.plusDays(daySlots - 1L);
        if (gridEnd35.isBefore(yearMonth.atEndOfMonth())) {
            daySlots = 42;
        }

        for (int i = 0; i < daySlots; i++) {
            dates.add(start.plusDays(i));
        }

        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        List<Task> activeTasks = taskRepository.findByUsernameAndDoneFalse(username).stream()
                .sorted(Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<List<LocalDate>> weeks = splitWeeks(dates);
        List<List<TaskCalendarBandPlacement>> taskBandWeeks =
                taskService.buildTaskBandPlacementsByWeek(activeTasks, dates);
        List<Integer> bandRowCounts = new ArrayList<>();
        for (List<TaskCalendarBandPlacement> w : taskBandWeeks) {
            if (w.isEmpty()) {
                bandRowCounts.add(0);
            } else {
                int maxLane = w.stream().mapToInt(TaskCalendarBandPlacement::lane).max().orElse(0);
                bandRowCounts.add(maxLane + 1);
            }
        }

        // スケジュール：月と重なるものすべて（終日の複数日・前月から続く予定も含む）
        LocalDateTime monthStart = startOfMonth.atStartOfDay();
        LocalDateTime monthEndExclusive = endOfMonth.plusDays(1).atStartOfDay();
        List<Schedule> monthlySchedules = scheduleService.findExpandedOverlapping(username, monthStart, monthEndExclusive)
                .stream()
                .sorted(Comparator.comparing(Schedule::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<List<ScheduleCalendarBandPlacement>> scheduleBandWeeks =
                scheduleService.buildScheduleBandPlacementsByWeek(monthlySchedules, dates);
        List<Integer> scheduleBandRowCounts = new ArrayList<>();
        for (List<ScheduleCalendarBandPlacement> w : scheduleBandWeeks) {
            if (w.isEmpty()) {
                scheduleBandRowCounts.add(0);
            } else {
                int maxLane = w.stream().mapToInt(ScheduleCalendarBandPlacement::lane).max().orElse(0);
                scheduleBandRowCounts.add(maxLane + 1);
            }
        }

        model.addAttribute("title", yearMonth.getYear() + "年" + yearMonth.getMonthValue() + "月");
        model.addAttribute("yearMonth", yearMonth);
        model.addAttribute("dates", dates);
        model.addAttribute("weeks", weeks);
        model.addAttribute("taskBandWeeks", taskBandWeeks);
        model.addAttribute("bandRowCounts", bandRowCounts);
        model.addAttribute("scheduleBandWeeks", scheduleBandWeeks);
        model.addAttribute("scheduleBandRowCounts", scheduleBandRowCounts);
        model.addAttribute("prevMonth", prev);
        model.addAttribute("nextMonth", next);

        Map<String, List<Task>> taskMap = taskService.mapUndatedTasksByCreatedDay(
                activeTasks, dates.get(0), dates.get(dates.size() - 1));
        for (LocalDate d : dates) {
            taskMap.putIfAbsent(d.toString(), new ArrayList<>());
        }
        model.addAttribute("taskMap", taskMap);
        model.addAttribute("dailyTotalCountMap", buildDailyTotalCountMap(activeTasks, monthlySchedules, dates));

        model.addAttribute("recommendedTasks", taskService.getRecommendedTasks(username));
        model.addAttribute("username", username);

        return "calendar";
    }

    private static List<List<LocalDate>> splitWeeks(List<LocalDate> dates) {
        List<List<LocalDate>> weeks = new ArrayList<>();
        for (int i = 0; i < dates.size(); i += 7) {
            weeks.add(new ArrayList<>(dates.subList(i, Math.min(i + 7, dates.size()))));
        }
        return weeks;
    }

    private static Map<String, Integer> buildDailyTotalCountMap(
            List<Task> activeTasks,
            List<Schedule> schedules,
            List<LocalDate> dates
    ) {
        if (dates.isEmpty()) {
            return Map.of();
        }
        LocalDate gridStart = dates.get(0);
        LocalDate gridEnd = dates.get(dates.size() - 1);
        Map<String, Integer> countMap = new java.util.HashMap<>();
        for (LocalDate d : dates) {
            countMap.put(d.toString(), 0);
        }

        for (Task t : activeTasks) {
            if (Boolean.TRUE.equals(t.getDone())) {
                continue;
            }
            if (Boolean.TRUE.equals(t.getQuickTodo())) {
                // クイックToDoはカレンダー（セル表示・+N集計）に出さない。
                continue;
            }
            if (t.getDueDate() != null && t.getCreatedAt() != null) {
                LocalDate from = t.getCreatedAt().toLocalDate();
                LocalDate to = t.getDueDate();
                if (to.isBefore(gridStart) || from.isAfter(gridEnd)) {
                    continue;
                }
                LocalDate s = from.isBefore(gridStart) ? gridStart : from;
                LocalDate e = to.isAfter(gridEnd) ? gridEnd : to;
                for (int i = 0; i <= ChronoUnit.DAYS.between(s, e); i++) {
                    String key = s.plusDays(i).toString();
                    countMap.put(key, countMap.getOrDefault(key, 0) + 1);
                }
            } else if (t.getDueDate() == null && t.getCreatedAt() != null) {
                LocalDate day = t.getCreatedAt().toLocalDate();
                if (!day.isBefore(gridStart) && !day.isAfter(gridEnd)) {
                    String key = day.toString();
                    countMap.put(key, countMap.getOrDefault(key, 0) + 1);
                }
            }
        }

        for (Schedule s : schedules) {
            if (s.getStartDateTime() == null || s.getEndDateTime() == null) {
                continue;
            }
            LocalDate from = s.getStartDateTime().toLocalDate();
            LocalDate to = ScheduleAllDaySupport.toInclusiveEndDate(
                    s.getStartDateTime(), s.getEndDateTime(), s.getAllDay());
            if (to == null || to.isBefore(from)) {
                continue;
            }
            if (to.isBefore(gridStart) || from.isAfter(gridEnd)) {
                continue;
            }
            LocalDate ds = from.isBefore(gridStart) ? gridStart : from;
            LocalDate de = to.isAfter(gridEnd) ? gridEnd : to;
            for (int i = 0; i <= ChronoUnit.DAYS.between(ds, de); i++) {
                String key = ds.plusDays(i).toString();
                countMap.put(key, countMap.getOrDefault(key, 0) + 1);
            }
        }

        return countMap;
    }

}

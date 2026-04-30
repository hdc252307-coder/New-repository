package com.example.demo;

import com.example.demo.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
public class DayController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/calendar/day")
    public String dayDetail(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam("date") String dateStr,
            Model model
    ) 
    
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();
        LocalDate date = parseDate(dateStr);

        // その日に表示すべきタスク:
        // 1) 期限なしは「作成日 == 当日」
        // 2) 期限ありは「作成日〜期日」に当日が含まれる
        List<Task> tasks = taskRepository.findByUsernameAndDoneFalse(username).stream()
                .filter(t -> isTaskVisibleOnDate(t, date))
                .sorted(createdAtComparator())
                .toList();

        // その日と重なるスケジュール（終日の複数日・時刻指定の跨ぎ）
        List<Schedule> schedules = scheduleService.findExpandedOverlapping(
                username,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        ).stream()
                .sorted(Comparator.comparing(Schedule::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        model.addAttribute("date", date);
        model.addAttribute("tasks", tasks);
        model.addAttribute("schedules", schedules);
        model.addAttribute("username", username);

        return "day";
    }

    private Comparator<Task> createdAtComparator() {
        return Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private boolean isTaskVisibleOnDate(Task task, LocalDate date) {
        if (task.isQuickTodo()) {
            if (task.getDueDate() != null) {
                return date.isEqual(task.getDueDate());
            }
            if (task.getCreatedAt() != null) {
                return date.isEqual(task.getCreatedAt().toLocalDate());
            }
            return false;
        }
        if (task.getCreatedAt() == null) {
            return task.getDueDate() != null && task.getDueDate().isEqual(date);
        }
        LocalDate created = task.getCreatedAt().toLocalDate();
        LocalDate due = task.getDueDate();

        if (due == null) {
            return created.isEqual(date);
        }
        if (due.isBefore(created)) {
            return false;
        }
        return !date.isBefore(created) && !date.isAfter(due);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("date is required");
        }
        return LocalDate.parse(dateStr.trim().replace('/', '-'));
    }
}

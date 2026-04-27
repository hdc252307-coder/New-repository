package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

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

        for (int i = 0; i < 35; i++) 

        

        {
            dates.add(start.plusDays(i));
        }

        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // ★ タスク（旧構造のままでOK）
        List<Task> monthlyTasks =
                taskRepository.findByUsernameAndDueDateBetween(username, startOfMonth, endOfMonth);

        // ★ スケジュール（新構造に合わせて修正）
        List<Schedule> monthlySchedules =
                // User.username での絞り込みは既存の derived query を利用する。
                scheduleRepository.findByUserUsernameAndStartDateTimeBetween(
                        username,
                        startOfMonth.atStartOfDay(),
                        endOfMonth.plusDays(1).atStartOfDay().minusNanos(1)
                );

        model.addAttribute("title", yearMonth.getYear() + "年" + yearMonth.getMonthValue() + "月");
        model.addAttribute("yearMonth", yearMonth);
        model.addAttribute("dates", dates);
        model.addAttribute("prevMonth", prev);
        model.addAttribute("nextMonth", next);

        model.addAttribute("taskMap", taskService.mapTasksByDate(monthlyTasks));

        // ★ 新構造でも mapSchedulesByDate はそのまま使える
        model.addAttribute("scheduleMap", scheduleService.mapSchedulesByDate(monthlySchedules));

        model.addAttribute("recommendedTasks", taskService.getRecommendedTasks(username));

        return "calendar";
    }

}

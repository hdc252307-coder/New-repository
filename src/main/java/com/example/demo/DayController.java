package com.example.demo;

import com.example.demo.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
public class DayController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @GetMapping("/calendar/day")
    public String dayDetail(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam("date") String dateStr,
            Model model
    ) 
    
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();
        LocalDate date = LocalDate.parse(dateStr);

        // その日のタスク（旧構造のままでOK）
        List<Task> tasks = taskRepository.findByUsernameAndDueDate(username, date);

        // その日のスケジュール（新構造に合わせて修正）
        List<Schedule> schedules =
                // User.username での絞り込みは既存の derived query を利用する。
                scheduleRepository.findByUserUsernameAndStartDateTimeBetween(
                        username,
                        date.atStartOfDay(),
                        date.atTime(LocalTime.MAX)
                );

        model.addAttribute("date", date);
        model.addAttribute("tasks", tasks);
        model.addAttribute("schedules", schedules);

        return "day";
    }
}

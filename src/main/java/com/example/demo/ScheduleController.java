package com.example.demo;

import com.example.demo.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
public class ScheduleController {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TrashService trashService;

    // 一覧（カレンダー）
    @GetMapping("/schedule")
    public String scheduleList(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model
    ) 
    
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();

        LocalDate today = LocalDate.now();
        int y = (year != null) ? year : today.getYear();
        int m = (month != null) ? month : today.getMonthValue();

        LocalDate start = LocalDate.of(y, m, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<Schedule> schedules = scheduleRepository.findByUserUsernameAndStartDateTimeBetween(
        username,
        start.atStartOfDay(),
        end.atTime(LocalTime.MAX)
        );

        model.addAttribute("schedules", schedules);
        model.addAttribute("year", y);
        model.addAttribute("month", m);

        return "schedule-list";
    }

    // 新規作成画面
    @GetMapping("/schedule/new")
    public String newSchedule(
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    ) 
    
    {
        if (userDetails == null) return "redirect:/login";

        model.addAttribute("schedule", new Schedule());
        return "schedule_form";
    }

    // 新規作成処理
    @PostMapping("/schedule/new")
    public String addSchedule(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime
    ) 
            
    {
        if (userDetails == null) return "redirect:/login";

        Schedule schedule = new Schedule();
        schedule.setTitle(title);
        schedule.setDescription(description);
        // DB制約に合わせて User エンティティを保存し、FK には user.id が入る。
        schedule.setUser(userDetails.getUser());
        schedule.setStartDateTime(LocalDateTime.parse(startDateTime));
        schedule.setEndDateTime(LocalDateTime.parse(endDateTime));
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());

        scheduleRepository.save(schedule);

        return "redirect:/calendar";
    }

    // 編集画面
    @GetMapping("/schedule/{id}/edit")
    public String editSchedule(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    ) 
    
    {
        if (userDetails == null) return "redirect:/login";

        Schedule schedule = scheduleRepository.findById(id).orElse(null);
        if (schedule == null) return "redirect:/calendar";

        // ★ 所有者チェック（Task と完全同じ）
        if (!schedule.getUser().getUsername().equals(userDetails.getUser().getUsername())) 
        
        
        {
            return "redirect:/calendar";
        }

        model.addAttribute("schedule", schedule);

        return "schedule_edit";
    }

    // 誤ってテンプレート名URL（/schedule_edit or /schedule_edit.html）に来た場合の救済。
    // 正規ルートは /schedule/{id}/edit なので、Whitelabel を避けるためカレンダーへ戻す。
    @GetMapping({"/schedule_edit", "/schedule_edit.html"})
    public String redirectInvalidScheduleEditPath() 
    
    {
        return "redirect:/calendar";
    }

    // 更新処理
    @PostMapping("/schedule/{id}/update")
    public String updateSchedule(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime
    ) 
            
    {
        if (userDetails == null) return "redirect:/login";

        Schedule schedule = scheduleRepository.findById(id).orElse(null);
        if (schedule == null) return "redirect:/calendar";

        // ★ 所有者チェック（Task と完全同じ）
        if (!schedule.getUser().getUsername().equals(userDetails.getUser().getUsername())) 
        
        
        {
            return "redirect:/calendar";
        }

        schedule.setTitle(title);
        schedule.setDescription(description);
        schedule.setStartDateTime(LocalDateTime.parse(startDateTime));
        schedule.setEndDateTime(LocalDateTime.parse(endDateTime));
        schedule.setUpdatedAt(LocalDateTime.now());

        scheduleRepository.save(schedule);

        return "redirect:/calendar";
    }

    // 削除（TrashService に委譲）
    @PostMapping("/schedule/{id}/delete")
    public String deleteSchedule(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails
    ) 
            
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();
        Schedule schedule = scheduleRepository.findById(id).orElse(null);
        if (schedule == null) return "redirect:/calendar";
        if (!schedule.getUser().getUsername().equals(username)) return "redirect:/calendar";
        trashService.moveToTrash("schedule", id, username);

        return "redirect:/calendar";
    }
}

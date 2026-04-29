package com.example.demo;

import com.example.demo.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEndExclusive = end.plusDays(1).atStartOfDay();
        List<Schedule> schedules = scheduleRepository.findOverlapping(username, rangeStart, rangeEndExclusive);

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
            @RequestParam(required = false) String allDay,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String startDateTime,
            @RequestParam(required = false) String endDateTime
    ) {
        if (userDetails == null) return "redirect:/login";

        boolean isAllDay = "true".equalsIgnoreCase(allDay) || "on".equalsIgnoreCase(allDay);
        LocalDateTime start;
        LocalDateTime end;
        try {
            if (isAllDay) {
                if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                    return "redirect:/schedule/new";
                }
                LocalDateTime[] range = ScheduleAllDaySupport.normalizeAllDayRange(
                        LocalDate.parse(startDate),
                        LocalDate.parse(endDate));
                start = range[0];
                end = range[1];
            } else {
                if (startDateTime == null || startDateTime.isBlank()
                        || endDateTime == null || endDateTime.isBlank()) {
                    return "redirect:/schedule/new";
                }
                start = LocalDateTime.parse(startDateTime);
                end = LocalDateTime.parse(endDateTime);
                if (end.isBefore(start)) {
                    return "redirect:/schedule/new";
                }
            }
        } catch (Exception e) {
            return "redirect:/schedule/new";
        }

        Schedule schedule = new Schedule();
        schedule.setTitle(title);
        schedule.setDescription(description);
        schedule.setUser(userDetails.getUser());
        schedule.setStartDateTime(start);
        schedule.setEndDateTime(end);
        schedule.setAllDay(isAllDay);
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
        model.addAttribute("allDayStartDate",
                schedule.getStartDateTime() != null ? schedule.getStartDateTime().toLocalDate() : null);
        model.addAttribute("allDayEndDate", ScheduleAllDaySupport.toInclusiveEndDate(
                schedule.getStartDateTime(), schedule.getEndDateTime(), schedule.getAllDay()));

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
            @RequestParam(required = false) String allDay,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String startDateTime,
            @RequestParam(required = false) String endDateTime
    ) {
        if (userDetails == null) return "redirect:/login";

        Schedule schedule = scheduleRepository.findById(id).orElse(null);
        if (schedule == null) return "redirect:/calendar";

        if (!schedule.getUser().getUsername().equals(userDetails.getUser().getUsername())) {
            return "redirect:/calendar";
        }

        boolean isAllDay = "true".equalsIgnoreCase(allDay) || "on".equalsIgnoreCase(allDay);
        LocalDateTime start;
        LocalDateTime end;
        try {
            if (isAllDay) {
                if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                    return "redirect:/calendar";
                }
                LocalDateTime[] range = ScheduleAllDaySupport.normalizeAllDayRange(
                        LocalDate.parse(startDate),
                        LocalDate.parse(endDate));
                start = range[0];
                end = range[1];
            } else {
                if (startDateTime == null || startDateTime.isBlank()
                        || endDateTime == null || endDateTime.isBlank()) {
                    return "redirect:/calendar";
                }
                start = LocalDateTime.parse(startDateTime);
                end = LocalDateTime.parse(endDateTime);
                if (end.isBefore(start)) {
                    return "redirect:/calendar";
                }
            }
        } catch (Exception e) {
            return "redirect:/calendar";
        }

        schedule.setTitle(title);
        schedule.setDescription(description);
        schedule.setStartDateTime(start);
        schedule.setEndDateTime(end);
        schedule.setAllDay(isAllDay);
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

package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ScheduleService scheduleService;

    public List<AppNotification> getNotifications(String username) {
        List<AppNotification> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<Task> overdueTasks = taskRepository.findByUsernameAndDoneFalse(username).stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(Task::getDueDate))
                .limit(3)
                .toList();
        for (Task t : overdueTasks) {
            out.add(new AppNotification(
                    "warn",
                    "期限切れタスク: " + t.getTitle() + "（期限 " + t.getDueDate() + "）",
                    "/tasks/" + t.getId()
            ));
        }

        List<Schedule> upcomingSchedules = scheduleService.findExpandedOverlapping(
                username, now, now.plusHours(24)).stream()
                .filter(s -> s.getStartDateTime() != null && !s.getStartDateTime().isBefore(now))
                .sorted(Comparator.comparing(Schedule::getStartDateTime))
                .limit(3)
                .toList();
        for (Schedule s : upcomingSchedules) {
            String when = Boolean.TRUE.equals(s.getAllDay())
                    ? s.getStartDateTime().toLocalDate().toString() + " 終日"
                    : s.getStartDateTime().toLocalDate() + " " + s.getStartDateTime().toLocalTime().withSecond(0).withNano(0);
            out.add(new AppNotification(
                    "info",
                    "24時間以内の予定: " + s.getTitle() + "（" + when + "）",
                    "/schedule/" + s.getId() + "/edit"
            ));
        }
        return out;
    }
}

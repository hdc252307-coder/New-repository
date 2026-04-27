package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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

        for (Schedule s : schedules) 

        

        

        {
            if (s.getStartDateTime() == null) continue;

            LocalDate date = s.getStartDateTime().toLocalDate();
            String key = date.toString();

            map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        return map;
    }
}

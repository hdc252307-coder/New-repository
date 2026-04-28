package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrashService {

    @Autowired
    private TrashRepository trashRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;  // スケジュール復元時に所有者を解決する

    // ▼ 削除一覧を取得
    public List<Trash> findByUsername(String username) 
    
    {
        return trashRepository.findByUsername(username);
    }

    // ▼ タスク or スケジュールを削除一覧に移動
    public void moveToTrash(String type, Long id, String username) 
    
    {
        Trash trash = new Trash();
        trash.setUsername(username);
        trash.setType(type);
        trash.setOriginalId(id);
        trash.setDeletedAt(LocalDateTime.now());

        if (type.equals("task")) 

        

        

        {
            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            if (!task.getUsername().equals(username)) {
                throw new RuntimeException("Forbidden");
            }

            trash.setTitle(task.getTitle());
            trash.setDueDate(task.getDueDate());
            trash.setPriority(task.getPriority());
            trash.setEstimatedMinutes(task.getEstimatedMinutes());
            trash.setDescription(task.getDescription());
            trash.setDone(task.getDone());
            trash.setTaskCompletedAt(task.getCompletedAt());

            trash.setTaskCreatedAt(task.getCreatedAt());
            trash.setTaskUpdatedAt(task.getUpdatedAt());

            taskRepository.deleteById(id);

        } else if (type.equals("schedule")) 

        {
            Schedule schedule = scheduleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Schedule not found"));
            if (!schedule.getUser().getUsername().equals(username)) {
                throw new RuntimeException("Forbidden");
            }

            trash.setTitle(schedule.getTitle());
            trash.setDescription(schedule.getDescription());
            trash.setStartDateTime(schedule.getStartDateTime());
            trash.setEndDateTime(schedule.getEndDateTime());

            trash.setScheduleCreatedAt(schedule.getCreatedAt());
            trash.setScheduleUpdatedAt(schedule.getUpdatedAt());

            scheduleRepository.deleteById(id);
        }

        trashRepository.save(trash);
    }

    // ▼ 削除一覧から復元
    public void restore(Long trashId, String username) 
    
    {
        Trash trash = trashRepository.findById(trashId)
                .orElseThrow(() -> new RuntimeException("Trash not found"));
        if (!trash.getUsername().equals(username)) {
            throw new RuntimeException("Forbidden");
        }

        if (trash.getType().equals("task")) 

        

        

        {
            Task task = new Task();
            task.setTitle(trash.getTitle());
            task.setUsername(trash.getUsername());
            task.setDueDate(trash.getDueDate());
            task.setPriority(trash.getPriority());
            task.setEstimatedMinutes(trash.getEstimatedMinutes());
            task.setDescription(trash.getDescription());
            task.setDone(trash.getDone());
            task.setCompletedAt(trash.getTaskCompletedAt());

            task.setCreatedAt(trash.getTaskCreatedAt());
            task.setUpdatedAt(trash.getTaskUpdatedAt());

            taskRepository.save(task);

        } else if (trash.getType().equals("schedule")) 

        {
            Schedule schedule = new Schedule();
            schedule.setTitle(trash.getTitle());
            schedule.setDescription(trash.getDescription());
            schedule.setStartDateTime(trash.getStartDateTime());
            schedule.setEndDateTime(trash.getEndDateTime());

            // 既存FK制約に合わせ、APP_USER.id を持つ User を再関連付けして復元する。
            User user = userRepository.findByUsername(trash.getUsername());
            schedule.setUser(user);

            schedule.setCreatedAt(trash.getScheduleCreatedAt());
            schedule.setUpdatedAt(trash.getScheduleUpdatedAt());

            scheduleRepository.save(schedule);
        }

        trashRepository.deleteById(trashId);
    }

    // ▼ 完全削除
    public void deleteForever(Long trashId, String username) 
    
    {
        Trash trash = trashRepository.findById(trashId)
                .orElseThrow(() -> new RuntimeException("Trash not found"));
        if (!trash.getUsername().equals(username)) {
            throw new RuntimeException("Forbidden");
        }
        trashRepository.deleteById(trashId);
    }

    public List<Trash> findExpiredTrash(LocalDateTime cutoff) {
        return trashRepository.findByDeletedAtLessThanEqual(cutoff);
    }

    public void moveExpiredSchedulesToTrash(LocalDateTime now) {
        List<Schedule> expiredSchedules = scheduleRepository.findByEndDateTimeBefore(now);
        for (Schedule schedule : expiredSchedules) {
            moveToTrash("schedule", schedule.getId(), schedule.getUser().getUsername());
        }
    }
}

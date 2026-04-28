package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataCleanupScheduler {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TrashService trashService;

    @Scheduled(cron = "0 * * * * *")
    public void runCleanupJobs() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        // 期限切れ予定は即時でゴミ箱へ移す。
        trashService.moveExpiredSchedulesToTrash(now);

        // 完了から7日経過したタスクを物理削除する。
        List<Task> expiredCompletedTasks =
                taskRepository.findByDoneTrueAndCompletedAtLessThanEqual(sevenDaysAgo);
        if (!expiredCompletedTasks.isEmpty()) {
            taskRepository.deleteAll(expiredCompletedTasks);
        }

        // ゴミ箱投入から7日経過したデータを物理削除する。
        List<Trash> expiredTrash = trashService.findExpiredTrash(sevenDaysAgo);
        for (Trash trash : expiredTrash) {
            trashService.deleteForever(trash.getId(), trash.getUsername());
        }
    }
}

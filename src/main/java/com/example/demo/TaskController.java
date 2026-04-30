package com.example.demo;

import com.example.demo.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Controller
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TrashService trashService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    // 一覧
    @GetMapping("/tasks")
    public String taskList(
            @RequestParam(defaultValue = "priority") String sort,
            @RequestParam(value = "showOnboarding", required = false, defaultValue = "false") boolean showOnboardingRequested,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    )
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();

        // ★ deleted フラグ廃止 → 全タスク取得（Trash に移動したものは物理削除されている）
        List<Task> tasks = taskRepository.findByUsernameAndDoneFalse(username);

        tasks = tasks.stream()
                .sorted(taskDisplayComparator())
                .toList();

        model.addAttribute("tasks", tasks);
        model.addAttribute("username", username);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("recommendedTasks", taskService.getRecommendedTasks(username));
        model.addAttribute("notifications", notificationService.getNotifications(username));
        User currentUser = userRepository.findByUsername(username);
        boolean firstOpen = currentUser != null && !Boolean.TRUE.equals(currentUser.getOnboardingCompleted());
        model.addAttribute("showOnboarding", firstOpen || showOnboardingRequested);

        return "task-list";
    }

    @PostMapping("/tasks/onboarding/complete")
    @ResponseBody
    public ResponseEntity<Void> completeOnboarding(@AuthenticationPrincipal MyUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        String username = userDetails.getUser().getUsername();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setOnboardingCompleted(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    // 新規作成画面
    @GetMapping("/tasks/new")
    public String newTask(
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    )
    {
        if (userDetails == null) return "redirect:/login";

        model.addAttribute("task", new Task());
        return "task-new";
    }

    // 新規作成処理
    @PostMapping("/tasks/add")
    public String addTask(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String dueDate,
            @RequestParam int priority,
            @RequestParam int estimatedMinutes,
            @RequestParam(required = false, defaultValue = "default") String colorKey
    )
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setEstimatedMinutes(estimatedMinutes);

        if (dueDate != null && !dueDate.isEmpty())
        {
            task.setDueDate(LocalDate.parse(dueDate));
        }

        task.setDone(false);
        task.setCompletedAt(null);
        task.setColorKey(TaskColorKeys.normalize(colorKey));
        task.setUsername(username);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        taskRepository.save(task);

        return "redirect:/tasks";
    }

    // 詳細画面
    @GetMapping("/tasks/{id}")
    public String taskDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    )
    {
        if (userDetails == null) return "redirect:/login";

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        if (!task.getUsername().equals(userDetails.getUser().getUsername())) return "redirect:/tasks";

        model.addAttribute("task", task);
        return "task-detail";
    }

    // 編集画面
    @GetMapping("/tasks/{id}/edit")
    public String editTask(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    )
    {
        if (userDetails == null) return "redirect:/login";

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        if (!task.getUsername().equals(userDetails.getUser().getUsername())) return "redirect:/tasks";

        model.addAttribute("task", task);
        return "task-edit";
    }

    // 編集更新
    @PostMapping("/tasks/{id}/update")
    public String updateTask(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String dueDate,
            @RequestParam int priority,
            @RequestParam int estimatedMinutes,
            @RequestParam(required = false, defaultValue = "default") String colorKey
    )
    {
        if (userDetails == null) return "redirect:/login";

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        if (!task.getUsername().equals(userDetails.getUser().getUsername())) return "redirect:/tasks";

        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setEstimatedMinutes(estimatedMinutes);
        task.setDueDate(LocalDate.parse(dueDate));
        task.setColorKey(TaskColorKeys.normalize(colorKey));
        task.setUpdatedAt(LocalDateTime.now());

        taskRepository.save(task);

        return "redirect:/tasks/" + id;
    }

    // ★ 新方式：TrashService による削除
    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails
    )
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        if (!task.getUsername().equals(username)) return "redirect:/tasks";

        trashService.moveToTrash("task", id, username);

        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/toggle-complete")
    public String toggleComplete(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails
    ) {
        if (userDetails == null) return "redirect:/login";

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        if (!task.getUsername().equals(userDetails.getUser().getUsername())) return "redirect:/tasks";

        boolean done = Boolean.TRUE.equals(task.getDone());
        task.setDone(!done);
        task.setCompletedAt(done ? null : LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        return "redirect:/tasks";
    }

    @GetMapping("/tasks/completed")
    public String completedTasks(
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUser().getUsername();

        model.addAttribute("tasks", taskRepository.findByUsernameAndDoneTrue(username));
        model.addAttribute("username", username);
        return "task-completed";
    }

    private Comparator<Task> taskDisplayComparator() {
        return Comparator
                .comparing(Task::getPriority, Comparator.reverseOrder())
                .thenComparing(Task::getEstimatedMinutes, Comparator.reverseOrder())
                .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}

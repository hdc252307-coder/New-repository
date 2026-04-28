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
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TrashService trashService;

    // 一覧
    @GetMapping("/tasks")
    public String taskList(
            @RequestParam(defaultValue = "priority") String sort,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    )
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();

        // ★ deleted フラグ廃止 → 全タスク取得（Trash に移動したものは物理削除されている）
        List<Task> tasks = taskRepository.findByUsernameAndDoneFalse(username);

        // ソートロジックは現在未使用。必要になったら再導入する。
        model.addAttribute("tasks", tasks);
        model.addAttribute("username", username);
        model.addAttribute("recommendedTasks", taskService.getRecommendedTasks(username));

        return "task-list";
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
            @RequestParam int estimatedMinutes
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
            @RequestParam int estimatedMinutes
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
}

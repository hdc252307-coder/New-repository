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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private static final Set<String> ALLOWED_SORTS = Set.of("priority", "due", "created", "title", "estimated");

    // 一覧
    @GetMapping("/tasks")
    public String taskList(
            @RequestParam(defaultValue = "priority") String sort,
            @RequestParam(required = false) String colorKey,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false, defaultValue = "all") String due,
            @RequestParam(value = "showOnboarding", required = false, defaultValue = "false") boolean showOnboardingRequested,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    )
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();

        // ★ deleted フラグ廃止 → 全タスク取得（Trash に移動したものは物理削除されている）
        LocalDate today = LocalDate.now();
        String normalizedSort = normalizeSort(sort);
        String normalizedDue = normalizeDueFilter(due);
        Integer priorityFilter = parsePriorityFilter(priority);
        String colorFilterNormalized = normalizeColorFilterParam(colorKey);

        List<Task> allActive = taskRepository.findByUsernameAndDoneFalse(username);
        long quickTodoCount = allActive.stream().filter(Task::isQuickTodo).count();

        List<Task> tasks = allActive.stream()
                .filter(t -> !t.isQuickTodo())
                .filter(t -> matchesDueFilter(t, normalizedDue, today))
                .filter(t -> priorityFilter == null || priorityFilter.equals(t.getPriority()))
                .filter(t -> colorFilterNormalized == null
                        || TaskColorKeys.normalize(t.getColorKey()).equals(colorFilterNormalized))
                .sorted(comparatorForSort(normalizedSort))
                .toList();

        model.addAttribute("tasks", tasks);
        model.addAttribute("quickTodoCount", quickTodoCount);
        model.addAttribute("username", username);
        model.addAttribute("today", today);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("filterColorKey", colorKey != null ? colorKey : "");
        model.addAttribute("filterPriority", priority != null ? priority : "");
        model.addAttribute("filterDue", normalizedDue);
        model.addAttribute("recommendedTasks", taskService.getRecommendedTasks(username));
        model.addAttribute("notifications", notificationService.getNotifications(username));
        User currentUser = userRepository.findByUsername(username);
        // 新規登録のみ false。既存ユーザーで列が null のときは「未完了」と誤判定しない（毎回チュートリアル表示の防止）
        boolean firstOpen = currentUser != null && Boolean.FALSE.equals(currentUser.getOnboardingCompleted());
        model.addAttribute("showOnboarding", firstOpen || showOnboardingRequested);
        model.addAttribute("onboardingAutoOpen", firstOpen);
        model.addAttribute("showOnboardingRequested", showOnboardingRequested);

        return "task-list";
    }

    /** クイックToDo専用（軽量メモのみ触りたい人向け） */
    @GetMapping("/tasks/quick")
    public String quickTodoPage(
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String username = userDetails.getUser().getUsername();
        List<Task> quickTodos = taskRepository.findByUsernameAndDoneFalse(username).stream()
                .filter(Task::isQuickTodo)
                .sorted(Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        model.addAttribute("quickTodos", quickTodos);
        model.addAttribute("quickTodoReturnTo", "/tasks/quick");
        model.addAttribute("username", username);
        model.addAttribute("notifications", notificationService.getNotifications(username));
        return "quick-todos";
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

        model.addAttribute("username", userDetails.getUser().getUsername());
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
            @RequestParam(required = false) Integer estimatedMinutes,
            @RequestParam(required = false, defaultValue = "default") String colorKey
    )
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setEstimatedMinutes(normalizeEstimatedMinutes(estimatedMinutes));

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
        task.setQuickTodo(false);

        taskRepository.save(task);

        return "redirect:/tasks";
    }

    /** タイトル＋任意の期限だけの軽量ToDo */
    @PostMapping("/tasks/quick/add")
    public String addQuickTodo(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam String title,
            @RequestParam(required = false) String dueDate
    ) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String trimmed = title != null ? title.trim() : "";
        if (trimmed.isEmpty()) {
            return "redirect:/tasks/quick";
        }
        String username = userDetails.getUser().getUsername();
        LocalDateTime now = LocalDateTime.now();
        Task task = new Task();
        task.setTitle(trimmed);
        task.setDescription(null);
        task.setPriority(3);
        task.setEstimatedMinutes(15);
        task.setColorKey(TaskColorKeys.DEFAULT);
        task.setDone(false);
        task.setCompletedAt(null);
        task.setUsername(username);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setQuickTodo(true);
        if (dueDate != null && !dueDate.isBlank()) {
            try {
                task.setDueDate(LocalDate.parse(dueDate.trim()));
            } catch (Exception e) {
                task.setDueDate(null);
            }
        }
        taskRepository.save(task);
        return "redirect:/tasks/quick";
    }

    // 詳細画面
    @GetMapping("/tasks/{id}")
    public String taskDetail(
            @PathVariable Long id,
            @RequestParam(required = false) String returnTo,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    )
    {
        if (userDetails == null) return "redirect:/login";

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        if (!task.getUsername().equals(userDetails.getUser().getUsername())) return "redirect:/tasks";

        model.addAttribute("username", userDetails.getUser().getUsername());
        model.addAttribute("task", task);
        model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
        return "task-detail";
    }

    // 編集画面
    @GetMapping("/tasks/{id}/edit")
    public String editTask(
            @PathVariable Long id,
            @RequestParam(required = false) String returnTo,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    )
    {
        if (userDetails == null) return "redirect:/login";

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        if (!task.getUsername().equals(userDetails.getUser().getUsername())) return "redirect:/tasks";

        model.addAttribute("username", userDetails.getUser().getUsername());
        model.addAttribute("task", task);
        model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
        return "task-edit";
    }

    // 編集更新
    @PostMapping("/tasks/{id}/update")
    public String updateTask(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String dueDate,
            @RequestParam int priority,
            @RequestParam(required = false) Integer estimatedMinutes,
            @RequestParam(required = false, defaultValue = "default") String colorKey,
            @RequestParam(required = false) String returnTo
    )
    {
        if (userDetails == null) return "redirect:/login";

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        if (!task.getUsername().equals(userDetails.getUser().getUsername())) return "redirect:/tasks";

        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setEstimatedMinutes(normalizeEstimatedMinutes(estimatedMinutes));
        if (dueDate != null && !dueDate.isBlank()) {
            task.setDueDate(LocalDate.parse(dueDate.trim()));
        } else {
            task.setDueDate(null);
        }
        task.setColorKey(TaskColorKeys.normalize(colorKey));
        task.setUpdatedAt(LocalDateTime.now());

        taskRepository.save(task);

        String defaultAfterUpdate = task.isQuickTodo() ? "/tasks/quick" : "/tasks/" + id;
        return buildRedirect(returnTo, defaultAfterUpdate);
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

        return task.isQuickTodo() ? "redirect:/tasks/quick" : "redirect:/tasks";
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

        return task.isQuickTodo() ? "redirect:/tasks/quick" : "redirect:/tasks";
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

    /**
     * カレンダー帯のドラッグ用。作成日時・期限を同じ日数だけずらし、帯の長さ（日数）は維持する。
     */
    @PostMapping("/tasks/{id}/move")
    @ResponseBody
    public ResponseEntity<Void> moveTask(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam String targetDate,
            @RequestParam String segmentStartDate
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        if (!task.getUsername().equals(userDetails.getUser().getUsername())) {
            return ResponseEntity.status(403).build();
        }
        if (Boolean.TRUE.equals(task.getDone())) {
            return ResponseEntity.badRequest().build();
        }
        if (task.getCreatedAt() == null || task.getDueDate() == null) {
            return ResponseEntity.badRequest().build();
        }
        LocalDate target;
        LocalDate segment;
        try {
            target = LocalDate.parse(targetDate);
            segment = LocalDate.parse(segmentStartDate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        long deltaDays = ChronoUnit.DAYS.between(segment, target);
        if (deltaDays == 0) {
            return ResponseEntity.ok().build();
        }
        if (Math.abs(deltaDays) > 4000) {
            return ResponseEntity.badRequest().build();
        }

        task.setCreatedAt(task.getCreatedAt().plusDays(deltaDays));
        task.setDueDate(task.getDueDate().plusDays(deltaDays));
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        return ResponseEntity.ok().build();
    }

    private Comparator<Task> taskDisplayComparator() {
        return Comparator
                .comparing(Task::getPriority, Comparator.reverseOrder())
                .thenComparing(Task::getEstimatedMinutes, Comparator.reverseOrder())
                .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "priority";
        }
        String key = sort.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_SORTS.contains(key) ? key : "priority";
    }

    private static String normalizeDueFilter(String due) {
        if (due == null || due.isBlank()) {
            return "all";
        }
        String key = due.trim().toLowerCase(Locale.ROOT);
        if ("all".equals(key) || "overdue".equals(key) || "soon".equals(key) || "none".equals(key)) {
            return key;
        }
        return "all";
    }

    private static Integer parsePriorityFilter(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        try {
            int p = Integer.parseInt(priority.trim());
            if (p == 1 || p == 3 || p == 5) {
                return p;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return null;
    }

    private static Integer normalizeEstimatedMinutes(Integer estimatedMinutes) {
        if (estimatedMinutes == null || estimatedMinutes < 1) {
            return null;
        }
        return estimatedMinutes;
    }

    /**
     * @return null のとき色で絞り込まない。非 null のときは {@link TaskColorKeys#normalize(String)} 済みのキー。
     */
    private static String normalizeColorFilterParam(String colorKey) {
        if (colorKey == null || colorKey.isBlank()) {
            return null;
        }
        return TaskColorKeys.normalize(colorKey);
    }

    private static boolean matchesDueFilter(Task t, String dueFilter, LocalDate today) {
        LocalDate d = t.getDueDate();
        return switch (dueFilter) {
            case "overdue" -> d != null && d.isBefore(today);
            case "soon" -> d != null && !d.isBefore(today) && !d.isAfter(today.plusDays(7));
            case "none" -> d == null;
            default -> true;
        };
    }

    private String buildRedirect(String returnTo, String defaultPath) {
        String safePath = sanitizeReturnTo(returnTo);
        return "redirect:" + (safePath != null ? safePath : defaultPath);
    }

    private static String sanitizeReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return null;
        }
        String trimmed = returnTo.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return null;
        }
        return trimmed;
    }

    private Comparator<Task> comparatorForSort(String normalizedSort) {
        return switch (normalizedSort) {
            case "due" -> Comparator
                    .comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Task::getPriority, Comparator.reverseOrder())
                    .thenComparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "created" -> Comparator
                    .comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Task::getPriority, Comparator.reverseOrder());
            case "title" -> Comparator
                    .comparing((Task t) -> t.getTitle() == null ? "" : t.getTitle().toLowerCase(Locale.ROOT))
                    .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "estimated" -> Comparator
                    .comparing(Task::getEstimatedMinutes, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Task::getPriority, Comparator.reverseOrder());
            default -> taskDisplayComparator();
        };
    }
}

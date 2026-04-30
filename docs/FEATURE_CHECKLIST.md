# Feature Checklist (Current Audit)

Updated: 2026-04-29
Scope: App-wide (`auth`, `task`, `schedule`, `calendar`, `trash`, `guide`)

## 1) Authentication / Session Retention

- **Implemented**
  - Form login and logout flow
  - Remember-me persistent token support
  - Authenticated users visiting `/login` are redirected to `/tasks`
- **Need verification**
  - Production HTTPS + reverse proxy behavior for remember-me
- **References**
  - `src/main/java/com/example/demo/security/SecurityConfig.java`
  - `src/main/java/com/example/demo/LoginController.java`
  - `src/main/resources/templates/login.html`

## 2) Task Management

- **Implemented**
  - Task CRUD, complete toggle, completed list
  - Task color key and list card color reflection
  - Recommendation logic ("today-focused")
- **Need verification**
  - `sort` request parameter behavior and UI expectation alignment
- **References**
  - `src/main/java/com/example/demo/TaskController.java`
  - `src/main/java/com/example/demo/TaskService.java`
  - `src/main/resources/templates/task-list.html`

## 3) Schedule Management

- **Implemented**
  - Schedule create/edit/delete
  - All-day and timed input handling
  - All-day same-day correction and compatibility handling
- **Gap / missing**
  - `/schedule` returns `schedule-list`, but `schedule-list.html` template is missing
- **References**
  - `src/main/java/com/example/demo/ScheduleController.java`
  - `src/main/java/com/example/demo/ScheduleService.java`
  - `src/main/resources/templates/schedule_form.html`
  - `src/main/resources/templates/schedule_edit.html`

## 4) Calendar / Day Detail

- **Implemented**
  - Task and schedule band rendering
  - Overflow indication (`+N`) for crowded days
  - Day detail includes date-range based task visibility
- **Need verification**
  - Whether fixed 35-day grid is acceptable for months requiring 6 weeks (42 cells)
- **References**
  - `src/main/java/com/example/demo/CalendarController.java`
  - `src/main/java/com/example/demo/DayController.java`
  - `src/main/resources/templates/calendar.html`
  - `src/main/resources/templates/day.html`

## 5) Trash / Auto Cleanup

- **Implemented**
  - Move task/schedule to trash
  - Restore and permanent delete
  - Scheduled cleanup for expired data
- **Need decision**
  - Keep current schedule cleanup cadence (`@Scheduled` every minute) or add grace period
- **References**
  - `src/main/java/com/example/demo/TrashController.java`
  - `src/main/java/com/example/demo/TrashService.java`
  - `src/main/java/com/example/demo/DataCleanupScheduler.java`

## 6) New User Guidance

- **Implemented**
  - Static usage guide page and navigation
- **Not implemented**
  - Screenshot-based onboarding flow for newly registered users (first-time only + replay entry)
- **References**
  - `src/main/resources/templates/usage-guide.html`
  - `src/main/java/com/example/demo/HomeController.java`

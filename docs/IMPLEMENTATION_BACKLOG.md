# Implementation Backlog (Prioritized)

Updated: 2026-04-29

## High Priority

1. **Onboarding flow for newly registered users**
   - Status: Not implemented
   - Why: Explicit remaining requirement; user adoption impact is high.
   - Target area:
     - `src/main/java/com/example/demo/RegisterController.java`
     - `src/main/resources/templates/task-list.html`
     - `src/main/resources/templates/usage-guide.html`

2. **Define and lock production acceptance criteria in team process**
   - Status: Partially done (checklist created), process not yet institutionalized
   - Why: Prevents regressions and "works local only" uncertainty.
   - Target area:
     - `docs/PROD_ACCEPTANCE_CHECKLIST.md`
     - `docs/PROD_RUNBOOK.md`

## Medium Priority

3. **`/schedule` list route/template consistency**
   - Status: Gap
   - Why: `ScheduleController` returns `schedule-list`, but template is absent.
   - Options:
     - Create `schedule-list.html`, or
     - Redirect `/schedule` to `/calendar` if list screen is not needed
   - Target area:
     - `src/main/java/com/example/demo/ScheduleController.java`
     - `src/main/resources/templates/`

4. **Task list `sort` parameter contract**
   - Status: Unclear/undocumented behavior
   - Why: UI and backend expectation mismatch risk
   - Target area:
     - `src/main/java/com/example/demo/TaskController.java`
     - `src/main/resources/templates/task-list.html`

5. **Calendar month grid policy (35 vs 42 cells)**
   - Status: Undecided
   - Why: Edge-month display consistency
   - Target area:
     - `src/main/java/com/example/demo/CalendarController.java`
     - `src/main/resources/templates/calendar.html`

## Low Priority

6. **Cleanup scheduler cadence tuning**
   - Status: Implemented as every minute; business rule confirmation pending
   - Why: Operational load and retention UX tradeoff
   - Target area:
     - `src/main/java/com/example/demo/DataCleanupScheduler.java`

7. **Remove or refactor legacy/duplicate remember token fields in `User`**
   - Status: Potential tech debt
   - Why: Current remember-me relies on `persistent_logins`; entity fields may be legacy
   - Target area:
     - `src/main/java/com/example/demo/User.java`
     - `src/main/java/com/example/demo/UserRepository.java`

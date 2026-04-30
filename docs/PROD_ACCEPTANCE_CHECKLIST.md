# Production Acceptance Checklist

Updated: 2026-04-29
Goal: Verify release readiness for recent core changes.

## A. Pre-check (Environment)

- [ ] `.env.prod` has `APP_CRYPTO_KEY` and `SPRING_DATASOURCE_PASSWORD`
- [ ] Production URL is HTTPS
- [ ] DB migration policy for this release is confirmed
- [ ] `persistent_logins` table exists (or auto-creates at startup)

## B. Login Retention (Remember-me)

1. **Positive flow**
   - [ ] Login with "ログイン情報を保持する" checked
   - [ ] Close browser fully and reopen
   - [ ] Access `/tasks` directly
   - [ ] User remains authenticated

2. **Route behavior**
   - [ ] Access `/login` while retained-authenticated
   - [ ] App redirects to `/tasks`

3. **Logout behavior**
   - [ ] Execute logout
   - [ ] Access `/tasks` again
   - [ ] App redirects to `/login`

## C. Calendar / Schedule Acceptance

- [ ] Task and schedule bands render in monthly calendar
- [ ] Overflow indicator appears on crowded days (`+N`)
- [ ] Day detail shows tasks by date-range logic
- [ ] All-day schedule with same start/end date remains one-day behavior

## D. Trash / Auto Cleanup Acceptance

- [ ] Expired schedules move to trash as expected
- [ ] Restore from trash works for task and schedule
- [ ] Permanent delete works and respects ownership

## E. Operational Logs (only when debugging)

Use temporary DEBUG only for diagnosis, then revert to INFO:

- `logging.level.com.example.demo.security.RememberMeObservationInterceptor`
- `logging.level.org.springframework.security.web.authentication.rememberme`

Expected remember-me success signals:

- "Remember-me cookie accepted"
- "SecurityContextHolder populated with remember-me token"

## F. Rollback / Safety Notes

- [ ] Confirm backup exists before production rollout
- [ ] Keep previous deploy artifact and environment snapshot
- [ ] If auth anomalies occur, disable remember-me temporarily and redeploy

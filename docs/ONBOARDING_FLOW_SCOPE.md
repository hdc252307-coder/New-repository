# New User Onboarding Flow Scope

Updated: 2026-04-29
Objective: Define a screenshot-based usage explanation flow for newly registered users.

## 1. In Scope (Must Have)

1. **First-login onboarding trigger**
   - Show onboarding only for newly registered users at first task-list visit.
2. **Screenshot-based step flow**
   - 3 to 5 steps, each with screenshot + one short explanatory sentence.
3. **Skip / complete behavior**
   - User can close onboarding.
   - Completion/skip is persisted per user.
4. **Replay entry point**
   - User can open onboarding again from guide/help entry.

## 2. Out of Scope (for this phase)

- Personalized recommendations inside onboarding
- Analytics dashboard integration
- A/B testing variations
- Multi-language localization

## 3. Proposed UX Contract

- Step 1: Task list overview
- Step 2: Task creation/edit basics
- Step 3: Calendar and band view
- Step 4: Schedule creation and all-day usage
- Step 5: Trash restore and safe deletion

Display rules:
- Show modal/carousel on first login only
- "次回から表示しない" option included
- "使い方" or guide page offers replay

## 4. Data/State Options (Decision Needed)

Choose one persistence strategy:

1. **DB flag on User** (recommended)
   - Example: `onboardingSeenAt` or `onboardingCompleted` boolean
   - Pros: stable across devices and sessions
2. **Session-only**
   - Pros: quick, no schema change
   - Cons: does not meet first-login persistent requirement

## 5. Technical Touchpoints (Expected)

- Controllers
  - `src/main/java/com/example/demo/RegisterController.java`
  - `src/main/java/com/example/demo/TaskController.java`
- Templates
  - `src/main/resources/templates/task-list.html`
  - `src/main/resources/templates/usage-guide.html`
- Static assets
  - `src/main/resources/static/` (screenshots + onboarding JS/CSS)
- Entity (if DB flag chosen)
  - `src/main/java/com/example/demo/User.java`

## 6. Acceptance Criteria

- New user sees onboarding exactly once after registration
- Existing users do not get forced onboarding
- Replay from guide/help works
- Closing onboarding does not break task-list interaction
- Mobile viewport remains usable (no blocked primary actions)

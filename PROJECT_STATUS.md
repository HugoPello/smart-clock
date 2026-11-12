# Project status

Legend: `[x]` done and verified · `[ ]` pending. A phase is done when tests pass, lint is clean and its PR is merged.

## Phase 0 — Setup
- [x] `CLAUDE.md` with project rules
- [x] `PROJECT_STATUS.md` (this file)
- [x] `.gitignore` (secrets, build output, personal notes)
- [x] Gradle skeleton (Kotlin DSL, version catalog, Compose "hello" screen) — build, tests and lint pass locally
- [x] Lint strict (`warningsAsErrors`) and clean: 0 issues
- [x] Dependabot for Gradle and GitHub Actions
- [x] GitHub Actions CI workflow written (build + unit tests + lint)
- [ ] CI green on the PR
- [ ] README with badges (screenshot/GIF comes in phase 5)
- [ ] PR merged

## Phase 1 — Pure calculation
- [ ] `AlarmCalculator`: alarm = arrival − preparation − travel time
- [ ] Shift limits (−30 / +15 min) and user window
- [ ] Anti-oscillation threshold
- [ ] Fallback to original time when there is no traffic data
- [ ] Unit tests for all edge cases
- [ ] PR merged

## Phase 2 — Persistence + UI
- [ ] Room entity/DAO for one alarm (arrival, preparation, destination)
- [ ] ViewModel exposing UI state (StateFlow)
- [ ] Compose screen to edit and show the alarm
- [ ] PR merged

## Phase 3 — Real alarm
- [ ] Schedule with `AlarmManager.setAlarmClock`
- [ ] Exact-alarm permission flow
- [ ] Reschedule on `BOOT_COMPLETED`
- [ ] Ringing screen/notification
- [ ] Verified on a device/emulator
- [ ] PR merged

## Phase 4 — Traffic
- [ ] API key in `local.properties` → `BuildConfig`
- [ ] Routes API client (`TRAFFIC_AWARE`)
- [ ] Exact alarm before ring → worker → recalculate → reschedule
- [ ] Network failure keeps original time
- [ ] PR merged

## Phase 5 — Polish
- [ ] Screenshot/GIF at the top of the README
- [ ] Repo topics
- [ ] Final cleanup
- [ ] PR merged

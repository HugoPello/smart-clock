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
- [x] CI green on the PR (reported by author)
- [x] README with badges (screenshot/GIF comes in phase 5)
- [x] PR merged (#1)

## Phase 1 — Pure calculation
- [x] `calculateAlarm`: alarm = arrival − preparation − travel time
- [x] Shift limits (−30 / +15 min), configurable by the user (`AlarmLimits`)
- [x] Anti-oscillation threshold (3 min, works across midnight)
- [x] Fallback when there is no traffic data (keeps original or current alarm)
- [x] Unit tests for all edge cases (14, passing locally; lint clean)
- [x] CI green and PR merged (#3)

## Phase 2 — Persistence + UI
- [x] Room entity/DAO for one alarm (arrival, preparation, usual travel, destination)
- [x] Repository interface + manual DI (`SmartClockApp`)
- [x] ViewModel exposing UI state (StateFlow)
- [x] Compose screen to edit and show the alarm
- [x] Unit tests: ViewModel (fake repository) + entity mapper (22 total, passing locally; lint clean)
- [x] Verified on emulator (Pixel_10a): edit, rotate, save, force-stop and reopen keeps saved values; unsaved changes are discarded (reported by author)
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

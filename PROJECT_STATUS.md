# Project status

Legend: `[x]` done and verified · `[ ]` pending. A phase is done when tests pass, lint is clean and its PR is merged.

## Phase 0 — Setup
- [x] `CLAUDE.md` with project rules (local only since phase 4, gitignored)
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
- [x] PR merged (#4)

## Phase 3 — Real alarm
- [x] Schedule with `AlarmManager.setAlarmClock` through a single `AlarmSync` entry point (+ pure `nextOccurrence`, DST-safe)
- [x] On/off switch: Room v1 → v2 migration (`enabled` column), schema export enabled (`app/schemas/`)
- [x] Exact-alarm permission flow, plus notification (Android 13+) and full-screen intent (Android 14+) checks
- [x] Reschedule on `BOOT_COMPLETED`, app update, time/time zone change, permission granted and app start
- [x] Ringing notification (alarm stream, insistent, 10 min timeout) + full-screen `RingingActivity` with Dismiss
- [x] Unit tests: `nextOccurrence`, `AlarmSync`, ViewModel scheduling (36 total, passing locally; lint clean)
- [x] Verified on emulator (Pixel_10a, API 37), checked by Claude via adb:
  - phase 2 data survived the v1 → v2 migration, alarm starts off
  - permission cards shown; "Allow" opens the exact-alarm settings; after granting, the alarm is registered as an alarm clock
  - rang on time with the screen off: ringing screen shown, alarm-usage audio playing; Dismiss removed the notification and stopped the sound
  - next day's alarm scheduled right after ringing; still scheduled after a reboot without opening the app
- [ ] Not verified yet: secure lock screen (PIN), time zone change, Dismiss from the notification, heads-up while the phone is in use
- [x] CI green and PR merged (#5)

## Phase 4 — Traffic
- [x] API key in `local.properties` → `BuildConfig` (empty on CI → no traffic data, build still works)
- [x] Routes API client (Retrofit + OkHttp + kotlinx.serialization, `TRAFFIC_AWARE`, field mask, Android app identity headers)
- [x] Exact alarm 45 min before the original ring → WorkManager worker → `calculateAlarm` → reschedule
- [x] Network/API failure keeps original time (retries temporary errors, then stores `NO_TRAFFIC_DATA`)
- [x] Starting point field + traffic adjustment table: Room v2 → v3 with `@AutoMigration`
- [x] Unit tests: `planAlarm`, `TrafficCheck`, Routes client with MockWebServer (61 total, passing locally; lint clean)
- [x] Verified on emulator (Pixel_10a, API 37), checked by Claude via adb:
  - phase 3 data survived the v2 → v3 auto-migration
  - without an API key: the check ran, stored "no traffic data", alarm kept its original time, UI shows it
  - check alarm fired on time with the screen off in light Doze and the worker ran
- [ ] Not verified: deep Doze (the emulator won't enter it with an alarm clock less than 1 h away)
- [x] CI green and PR merged (#6)

### Phase 4b — Switch traffic provider to TomTom
- [x] Google Routes client removed (needs a Google Cloud billing account)
- [x] TomTom client: Geocoding (`search/2/geocode`) for both addresses + Routing (`calculateRoute`, traffic, `departAt`)
- [x] Key renamed to `TOMTOM_API_KEY` in `local.properties`
- [x] Travel time rounded up to whole minutes (alarm lands on an exact minute)
- [x] Traffic line hidden while editing; Save clears text field focus
- [x] Unit tests with MockWebServer (64 total, passing locally; lint clean)
- [x] Real TomTom call verified on the emulator with the author's key, checked by Claude via adb:
  - Puerta del Sol → Ciudad Universitaria: ~15 min, check took ~2 s
  - usual trip 15 min: change below threshold, alarm not moved
  - usual trip 5 min: alarm moved 10 min earlier, rescheduled at the exact minute
  - invented destination: "no traffic data" on the first attempt, original time kept
- [x] CI green and PR merged (#7)

## Phase 5 — Polish
- [x] README rewritten (UTF-16 → UTF-8, TomTom instead of Google, how it works, architecture, tests, limitations)
- [x] Screenshots + GIF at the top of the README: real app states with real Madrid traffic (demo-mode status bar)
- [x] Big clock shows the time that will actually ring; the line below explains the usual time and the traffic shift
- [x] Final cleanup: template backup rules replaced by an explicit, documented choice; no leftover TODOs or debug logs
- [x] Release build (R8) verified on the emulator, signed locally with the debug key: Room and a real TomTom call work
- [x] Time zone change verified: `TIMEZONE_CHANGED` rescheduled the alarm without opening the app
- [x] Unit tests: 65 total, passing locally; lint clean
- [x] Repo topics (set by the author on GitHub)
- [x] CI green and PR merged

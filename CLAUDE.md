# CLAUDE.md — smart-clock

Native Android alarm clock that adjusts the alarm time to real traffic before it rings.

## Stack
- Kotlin, Jetpack Compose, Room, WorkManager, MVVM.
- Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`). All versions live there.
- Traffic: Google Routes API (`TRAFFIC_AWARE`). Fallbacks considered: TomTom, HERE.

## Domain rules
- User inputs: **arrival time** + **preparation time** + destination. The alarm time is derived:
  `alarm = arrival - preparation - travelTime`.
- Shift limits vs the original alarm: at most **30 min earlier** and **15 min later**, plus a user-defined window.
- Network/API failure → keep the original alarm time. Never fail silently into "no alarm".
- Anti-oscillation: only move the alarm if the change exceeds a threshold.
- All of the above lives in a **pure function** (no Android imports) with unit tests.
- Scheduling: `AlarmManager.setAlarmClock` (survives Doze). Reschedule on `BOOT_COMPLETED`. Request exact-alarm permission.
- Traffic query is triggered by an exact alarm that enqueues a worker (WorkManager alone is not exact).

## MVP scope
One alarm, one fixed destination, one traffic query, calculation + limits. Nothing else until the MVP works.

## Working method
- One branch + one PR per phase (`phase-N-name`). Phase is done when: unit tests pass, lint is clean, PR merged.
- Track progress only in `PROJECT_STATUS.md`. Don't mark anything done that wasn't verified.
- CI (GitHub Actions) runs build + unit tests + lint on every PR.
- **The author makes all commits, pushes and PRs.** Claude leaves changes uncommitted and proposes how to split them into commits, with Conventional Commits messages.

## Secrets
- API keys go in `local.properties` (gitignored) and are injected through `BuildConfig`.
- Never commit keys, never print them in logs.

## Learning mode
The author is a DAM student and wants to understand every piece:
- Explain new concepts briefly before using them, with a link to official docs.
- Leave small, well-scoped parts (pure logic, tests) for the author to write; review them like a PR.
- Close each phase with a short explanation + 2-3 interview-style questions.
- `docs/LEARNING.md` is personal and gitignored — never commit it.

## Commands
```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android lint
```

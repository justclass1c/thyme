# Phase F — Background Execution

Reference doc. **You write every line; I guide.** Tasks are described, never
handed over as copyable code — when stuck, the pattern to imitate is named,
not pasted.

Phase E is **closed** (all device checks reported passing 2026-10-20):
state machine incl. skip, cycle UI (rounds counter, phase headings, skip
button), settings layer. This doc carries its facts forward as §1 and lays
out Phase F's work in build order. The timestamp model (2.1) is the
foundation — everything else in F rewrites around it, which is why it is
first even though it's "plumbing".

---

## 1. Current state — done & verified (carried from Phase E)

### 1.1 Models & settings layer
- `TimerPhase`: `FOCUS`, `SHORT_BREAK`, `LONG_BREAK`.
- `TimerSession`: `timerPhase`, `completedRounds` (+ F adds `endTime`).
- `TimerSettings`: `autoStartNextPhase = false`, `longBreaksEnabled = true`,
  `targetWorkMinutesEnabled = false`, `targetWorkMinutes = 0`.
- DataStore: four keys mapped; write-side stores raw (decision #12);
  single read-repair coerces `rounds 1..100`.
- Validation lives only in the settings VM (clamps; no repo-side edits).

### 1.2 The transition contract (survives F2 — the mechanics rewire, the table does not)

| Phase completing | Condition | Next phase | `completedRounds` |
|---|---|---|---|
| `FOCUS` | enabled && `rounds >= target − 1` | `LONG_BREAK` | `0` |
| `FOCUS` | enabled, below that | `SHORT_BREAK` | `+1` |
| `FOCUS` | long breaks disabled | `SHORT_BREAK` | `0` (decision #6) |
| `SHORT_BREAK` | — | `FOCUS` | unchanged |
| `LONG_BREAK` | — | `FOCUS` | unchanged (already `0`) |

Skip contract (decision #7 / #2-rev): `FOCUS` → `SHORT_BREAK` uncounted,
counter untouched; breaks → `FOCUS`; **unconditional** — no fresh-phase
no-op, no guard. `advancePhase()`'s FOCUS branch is gated on the
completion premise (F2: `isRunning && endTime <= now`, named
`isExpired`): expired arm runs trigger evaluation, interrupted arm is
landing-only. Skip's landing is compute-only.

### 1.3 UI layer (as landed after 2.1's display tail)
- `PomodoroScreen`: headings, control row (reset/pause-play,
  always-visible skip, settings), reset no-ops at paused-full.
- `PomodoroProgress`: ring + time + clock + rounds text counter gated
  on `showRounds`; ring and time render from the frame-clock value
  (`rememberTime`: booking-keyed effect, paused binds the repo
  snapshot — see 2.1 item 1 for the landed shape).
- `PomodoroViewModel`: three-way `combine(session, clockText,
  settings)` — real-now remaining while running (paused binds the
  snapshot), real `endTime` passed through; `stateIn(WhileSubscribed(
  5_000))`; four events routed to the repo. `_clockText` minute loop
  stays.
- `PomodoroUiState`: raw fields + `endTime`; derived `timeText`/
  `progress` removed — the display layer owns derivation.

### 1.4 File map — what each file is for

Layer logic: `domain` states the guarantees (pure models/contracts, no
Android imports), `data` implements them and owns runtime state,
`features` binds one screen end-to-end via UDF, `ui` is pure look, `di`
is wiring only, and manifest/`ThymeApplication` register the OS-visible
components (activities, services, receivers, channels).

**Domain — the contracts (`app/src/main/java/com/class1c/thyme/domain/`)**
| File | Purpose |
|---|---|
| `model/TimerPhase.kt` | the three-phase enum |
| `model/TimerSession.kt` | current timer state; Phase F adds `endTime` — **the** file to edit in 2.1's first step |
| `model/TimerSettings.kt` | settings shape users configure |
| `repository/TimerRepository.kt` | the timer contract (`start`/`pause`/`reset`/`skip` + session flow); interfaces change here *first* whenever the contract itself grows |
| `repository/SettingsRepository.kt` | the settings contract the VMs consume |

**Data — the implementations (`data/`)**
| File | Purpose |
|---|---|
| `repository/InMemoryTimerRepository.kt` | **the brain**: single source of timer truth (the `_session` flow), transitions, skip, F2's writers rework, F4's schedule/cancel hooks, F8's catch-up |
| `repository/OfflineSettingsRepository.kt` | implements `SettingsRepository` on top of the DataStore |
| `datastore/TimerSettingsDataSource.kt` | raw key↔value mapping + the read-repair; F is silent here (no new settings keys) |

**DI — the wiring (`di/`)**
| File | Purpose |
|---|---|
| `TimerModule.kt` | binds `TimerRepository` → implementation; F4/F8 may add the alarm-scheduler binding here |
| `SettingsModule.kt` | binds `SettingsRepository` |
| `AppModule.kt` | `@ApplicationScope` qualifier + shared singletons |

**Features — one screen's vertical slice (`features/pomodoro/`)**
| File | Purpose |
|---|---|
| `PomodoroScreen.kt` | the visible state machine; F2.2's FGS start/stop seam lives in its control-row clicks |
| `PomodoroProgress.kt` | ring + time + clock + rounds counter; **the frame-clock countdown producer** (booking-keyed effect, paused = snapshot binding) |
| `PomodoroViewModel.kt` | combine → UIState (real-now remaining, `endTime` pass-through); F8's re-entry catch-up hook |
| `PomodoroEvent.kt` | the four user intents (immutable, sealed) |
| `PomodoroUiState.kt` | the screen's render contract |
| `PomodoroRoute.kt` | collects state, hands to Screen, hosts the VM via Hilt |
| `PomodoroPreviews.kt` | design-time snapshots |
| `settings/*` | the settings screen slice: `ViewModel` (clamps/validation owner), `Screen`, `Route`, `UIState`, `Event`, plus `popups/` |

**UI — pure look (`ui/`)**
| File | Purpose |
|---|---|
| `theme/{Theme,Color,Type}.kt` | ThymeTheme (Type.kt has `poppinsFamily`) |
| `icons/*.kt` | hand-drawn `ImageVector`s (Refresh, PlayArrow, Pause, FastForward, Settings) |

**App-level — OS registration**
| File | Purpose |
|---|---|
| `MainActivity.kt` | the single Activity host + global padding insets |
| `ThymeApplication.kt` | `@HiltAndroidApp`; **F2.5's chime channel gets created here** (2.2's countdown channel landed service-local — see §2.2 status) |
| `navigation/{AppNavHost,Route,NavigationBar}.kt` | type-safe nav wiring |
| `AndroidManifest.xml` | every OS-visible component declaration: F2.2's service + permissions, 2.3's receiver, F6/F7 permissions |
| `res/xml/*`, `res/values/*` | backup rules, strings/colors/themes |
| Gradle (`app/build.gradle.kts`, `gradle/libs.versions.toml`) | dependency truth; §5 carry items live here |

### 1.5 Phase F task → file map (one row per task; paths relative to `app/src/main/java/com/class1c/thyme/` unless stated)

| Task | Files | State |
|---|---|---|
| **2.1 — timestamp model** | `domain/model/TimerSession.kt` · `data/repository/InMemoryTimerRepository.kt` · `features/pomodoro/PomodoroViewModel.kt` · `features/pomodoro/PomodoroUiState.kt`; display tail: `features/pomodoro/PomodoroProgress.kt` | ✅ landed |
| **2.1 item 2 — receiver skeleton** | **new** `alarm/TimerAlarmReceiver.kt` · `app/src/main/AndroidManifest.xml` · `domain/repository/TimerRepository.kt` · `data/repository/InMemoryTimerRepository.kt` | ✅ landed 2026-10-23 (battery green) — status in §2.1 item 2 |
| **2.2 — F3 foreground service** | **new** `services/TimerForegroundService.kt` (user's folder name — recorded deviation from `service/`) · `features/pomodoro/PomodoroScreen.kt` · `AndroidManifest.xml` · (optional) `ui/icons/*` | ✅ landed 2026-10-23 (battery green) — status in §2.2 |
| **2.3 — F4 alarms (full)** | `alarm/TimerAlarmReceiver.kt` (grows) · `AndroidManifest.xml` · `data/repository/InMemoryTimerRepository.kt` · `di/TimerModule.kt` (only if an alarm-scheduler abstraction is introduced) · `features/pomodoro/settings/PomodoroSettingsScreen.kt` | ⏳ |
| **2.4 — F8 catch-up** | `data/repository/InMemoryTimerRepository.kt` · `alarm/TimerAlarmReceiver.kt` · `features/pomodoro/PomodoroViewModel.kt` · (optional) `features/pomodoro/PomodoroProgress.kt` | ✅ landed 2026-10-24 (battery green) — status in §2.4 |
| **2.5 — F5 chime** | (final shape) `alarm/TimerAlarmReceiver.kt` · `ThymeApplication.kt` · (optional) `app/src/main/res/values/strings.xml` | ✅ landed 2026-10-24 (battery green); **final shape reached 2026-10-25 — bell lives in the receiver behind the Boolean gate** — status + addenda 1–3 in §2.5 |
| **2.6 — F6 POST_NOTIFICATIONS** | `features/pomodoro/PomodoroScreen.kt` (seam) · `features/pomodoro/settings/PomodoroSettingsScreen.kt` (mirror) · `AndroidManifest.xml` (pre-paid) | ✅ **landed 2026-10-25** — seam + settings-row mirror + batteries green (fresh-install prompt, denial patience, grant round-trip) — status in §2.6 |
| **2.7 — F7 HyperOS onboarding** | `features/pomodoro/settings/PomodoroSettingsScreen.kt` · `AndroidManifest.xml` · `app/src/main/res/values/strings.xml` | ⏳ **battery-exemption row landed 2026-10-25** (request dialog + live mirror; Play-lint accepted) — autostart row, exact-alarm row, fallback copy remaining; status in §2.7 |

Package placement: **new packages sit beside the existing ones at the
`com.class1c.thyme` level** — `alarm/` and `service/` are OS entry
points, so they belong *neither to `data/`* (not state) *nor to
`features/`* (not a screen). The files inside them are rows of the
table; a task may reuse files across rows (e.g. the receiver). This
section is the directory-level answer; each §2.x table still carries
the *why* per file.

---

## 2. Phase F build order

### 2.1 F2 — the timestamp model (variant B locked; demolition remains)

**Files:** `domain/model/TimerSession.kt` →
`data/repository/InMemoryTimerRepository.kt` →
`features/pomodoro/PomodoroViewModel.kt` (+ `PomodoroUiState.kt` at the
demolition). Nothing else: the repo's four signatures are unchanged,
the UI screens don't know the difference, `TimerSettings*` is silent.

| File | Why this file |
|---|---|
| `domain/model/TimerSession.kt` | The model is where the contract starts: `endTime` lives on the session, and F2-B's invariants (constant `isRunning` default, sentinel `0`) are statements about the *data class* — nothing below it can state them |
| `data/repository/InMemoryTimerRepository.kt` | The brain owns `_session` and every writer; the pair discipline, the freeze, the landing ternary — all are **state transitions**, so they live in the one file that owns state |
| `repository/TimerRepository.kt` | Interfaces-first rule (kept here for history: the four signatures did NOT change — deliberate contract stability) |
| `PomodoroViewModel.kt` | The combine is the projection layer: real-now remaining while running, snapshot binding while paused, `endTime` pass-through for the display layer |
| `PomodoroUiState.kt` | The render contract grew the field the display consumes (`endTime`) and lost what the display layer took over (`timeText`/`progress`) |
| — why *not* the screens | The screens read fields; the 2.1 rewrite changed no field names they use. Where a consumer *did* need to learn something (the frame loop), it lives in `PomodoroProgress.kt` — see item 1 |

**Model contract (F2-B, locked):**

- `endTime: Long = 0L` (epoch ms). `0` = "no appointment booked"
  (pristine session, auto-start-OFF landing). While running it names
  the booked appointment; while paused it is **kept** as stale
  fiction — nobody honors it (resume re-books from the snapshot).
- `isRunning`: stored constructor param, **constant** default `false`.
  Never a computed default: Kotlin re-evaluates expression defaults
  inside every `.copy()` that omits them (a paused timer silently
  self-arms). Running ⇔ `isRunning`; paused ⇔ `!isRunning` —
  regardless of what `endTime` holds.
- `remainingSeconds`: live only while paused (the pause-time
  snapshot). While running it is stale and nobody writes it.
- Remaining math: `ceil((endTime − now) / 1000.0).coerceAtLeast(0)` —
  **ceil**, applied at the division (ceil-after-Long-trunc is a
  no-op): the display hits `00:00` exactly at the real completion
  instant — the same moment 2.3's alarm fires. The `now` inside is
  always the **real clock read at evaluation time** — never a value
  carried through a flow (a flow-carried `now` answers "when was this
  recorded," not "when is now" — the stale-tick +1-on-unpause lesson).
- `pause()` = **the last tick**: one final conversion at the pause
  instant authors the snapshot; post-ticker there is no other author
  between runs (a freeze-less pause leaves the landing's stale value:
  paused display shows full, resume restarts from full).
- Completion premise — `isExpired` = the fenced
  `isRunning && endTime <= now`, computed at call sites (name
  lesson: a deadline passing is a fact, not an error; `completed`
  rejected because a skip also *ends* a phase). No consumer reads a
  paused session's fiction `endTime`.

**Gate table — every consumer asks the running-test first:**

| Question | Rule |
|---|---|
| Running? | `isRunning == true` (never infer it from `endTime`) |
| Completion pending? | `isRunning && endTime <= now` |
| Still in the future? | `isRunning && endTime > now` (no-op guard) |
| Paused? | `!isRunning` → no-op everywhere, before any time comparison |

**Pair discipline — any writer touching `isRunning` writes both
halves in the same atomic `copy()`; a half-update is a bug:**

| Op | Semantics |
|---|---|
| `start()` | resume, one pair: `endTime = now + remainingSnapshot`, `isRunning = true`; guards `isRunning → return` (double-tap idempotency) and `remainingSeconds <= 0` (exhausted-paused) |
| `pause()` | entry guard `!isRunning → return`; freeze `remainingSeconds` = ceil-form of `it.endTime` + `coerceAtLeast(0)`; `isRunning = false`; `endTime` kept (fiction) |
| `reset()` | `endTime = 0`, `isRunning = false`, `remainingSeconds = totalSeconds`; counter/phase untouched |
| `skip()` | just `advancePhase()`; no cancel; FOCUS && !`isExpired` ⇒ SHORT uncounted |
| completion | only via 2.3's receiver → `advancePhase()`; `!isRunning → no-op` AND `endTime > now → no-op` (idempotent vs double-fire, sentinel-0, fiction-endTime) |
| landing | auto-start ON ⇒ `endTime = now + durationFor(next)`, `isRunning = true`; OFF ⇒ `endTime = 0`, `isRunning = false`, paused-full — one atomic update |

Consumers read running-ness only through `session.isRunning` — the
one access path that keeps a future variant-A swap (derive the flag) a
one-file model change.

**Status (2026-10-21, demolition verified in play):** repo landed at
97 lines — `start()` books the pair (keeps the `remainingSeconds <= 0`
guard); `pause()` is exactly the freeze (entry guard, ceil-form,
coerce, fiction `endTime` kept); `reset()` zeroes its pair; the
landing is the single atomic pair-ternary; ticker, cancel dances and
`timerJob` all swept — keep `CoroutineScope`/`@ApplicationScope` (the
settings collector) and `ceil`. Sweep-list correction recorded:
**`launch` stays** (the VM's clock loop keeps using it). VM landed too,
with two play-found fixes: the ceil-form over raw epoch ms (a
`tick / 1000` unit mix was caught in review) and — **real now inside
the combine**, `System.currentTimeMillis()` at emission time, not the
tick's recorded value (the stale-now lesson above: fresh bookings
overshot by however stale the tick was — the +1 on unpause).
Kept deviation: `clockText` remains its own minute-level flow —
building it from the tick was rejected as unneeded complexity.

Why no rollback to the E-era coroutine (settled 2026-10-21): as a
display pulse it would have been fine — every display artifact here
is sampling physics, pulse-home-independent — but it held the
*authority* job that F2 removes. Kept in-process, it would produce the
2.3 double-advance (in-process landing vs the OS alarm's landing both
arriving), turn F8 catch-up into a drifting-counter reconciliation
instead of a deadline subtraction, and race every pause-freeze. The
move was a **downgrade and relocation**: authority to the timestamp
model (act-authored bookings, single writer per break), pulse demoted
to a display staffer.

**Remaining (do not reorder):**

1. ✅ **Demolition sitting — done.** Repo + VM verified 2026-10-21;
   display tail landed 2026-10-22 as the user's **self-built** frame
   loop (doc record amended to match the landed shape — the
   assistant's reference variant is superseded). Interim battery green:
   pristine paused-full; crisp per-second boundaries (~1.0 s first
   displayed second); pause frozen / resume exact with no +1 flash /
   double-pause intact; pause→reset → paused-full with the pair
   zeroed; skip on a live phase lands per the settings ternary;
   skip-at-`00:00` = manual stand-in — completed arm rolls and counts.
   Knowingly broken **until the receiver skeleton**: automatic roll at
   the deadline and auto-start chains.

   The landed display architecture (canonical record):
   - **Producer:** `rememberTime` in `PomodoroProgress.kt` —
     `LaunchedEffect` **keyed on `state.endTime`** (a fresh booking
     restarts the effect with a fresh capture), body looping
     unconditionally: recompute
     `ceil((endTime − real now)/1000.0) → carrier write` then
     `withFrameNanos {}`. Sub-frame-accurate transitions; rides vsync
     wake-ups; suspends free when unrendered.
   - **Pause-kill = composition structure:** the effect lives inside
     `if (state.isRunning)` — on pause the recomposition drops the
     node, and *that* is the cancellation. Variant-B subtlety worth
     its line in history: pause does **not** change `endTime`, so a
     key change cannot observe the pause; only the wrapper can.
   - **Two dead-ends kept as lessons:** never key an effect on the
     value it writes — when the recomputed integer repeats within a
     second the write is a no-change event, nothing restarts, the
     heartbeat dies mid-second; and `while (state.x)` inside the body
     reads the *captured* state — always-true whenever the effect is
     alive, so lifetime is managed structurally (key + wrapper), never
     by a closure condition.
   - **Paused:** `return state.remainingSeconds` directly — the
     binding of the repo's freeze. No remembered state, no clock read,
     no `endTime` math (paused consumers don't touch fiction; the
     snapshot is a contract field with three consumers: paused
     display, `start()`'s re-booking, the `<= 0` guard).
   - **One producer, two consumers:** ring and time text both read the
     same frame-carried value. Units lessons: the ring API takes a
     fraction in [0, 1] (a `*100` percentage was a live bug caught in
     review); integer division collapses mid-run ratios —
     `toFloat()` before the division.
   - Sweeps: `_tick` deleted (its heartbeat role superseded by the
     frame loop); the consumerless derived `timeText`/`progress`
     props removed from `PomodoroUiState` (one source of derivation
     now lives in the display layer); `endTime` passes through the
     UIState for the loop.
2. ⏳ **Receiver skeleton (§2.3's first bolt — its own sitting):**
   manifest registration; the receiver's one job = fire
   `advancePhase()` through the completion guards (`!isRunning` /
   `endTime > now` no-ops); alarm schedule now + cancel on
   pause/reset, **gated on the pair** (never `endTime > 0` alone).
   **Build order, four steps:** (1) the interface entry on
   `TimerRepository` — the gated public method the receiver will call
   (contracts change first); (2) the repo implementation: the two
   interval guards + private `advancePhase()`, and `syncAlarm()` after
   every writer (arm iff `isRunning`, else cancel — the pair decides);
   (3) the receiver class per **§2.3's shape checklist** (class,
   `@AndroidEntryPoint`, injected interface field, one-line
   `onReceive`); (4) the manifest claims (`.alarm.TimerAlarmReceiver`,
   `exported="false"`, the permission). Nothing in the receiver
   decides — §2.3's mental model governs.
   Chime (2.5), exact-alarm permission fallback, onboarding rows come
   later. Then the full §3 battery.

   **Status (2026-10-23 — landed & battery green):** all four steps
   wired: interface entry `completeExpiredPhase()` with both interval
   guards; `syncAlarm()` called after **all four writers** (cancel
   if paused, else the SDK≥31+`!canScheduleExactAlarms()` → inexact /
   else exact decision tree); the receiver as §2.3's five-line thunk;
   manifest receiver + permission. Battery record (POCO 2602BPC18G,
   Android 16, HyperOS 3.0), via `dumpsys alarm`/appops receipts:
   **box 4 ran itself out of order** — the fresh install holds
   default-deny special access, so the fallback arm was exercised
   first: `window=+44s997ms`, no `exactAllowReason`, completion
   arrived ~45 s late and landed fine, zero crashes; the alarm
   history also showed each pause/reset write cancelling and resume
   re-arming (box 2's machinery, visible live). After toggling
   special access (HyperOS hides the row — found via Settings search
   "Alarms & reminders", or `REQUEST_SCHEDULE_EXACT_ALARM`): grant
   `allow`, and the arm's fingerprint flipped to
   `window=0 exactAllowReason=permission flags=0x5` — box 1 on-time
   rolls, chained deadlines advancing (box 3's shape). Lesson logged
   for 2.6/2.7: the toggle genuinely needs guiding users to; the app
   can open the screen itself (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`),
   shell cannot pass a package as caller. The in-source `TODO:
   understand this better` over `syncAlarm()` may quiet once §2.3's
   full sitting runs.
3. ✅ **F8-A's real `catchUp()` (§2.4) — landed 2026-10-24, battery
   green** (record in §2.4).
   Scope honesty: catch-up covers frozen-survivor wakes only; a
   hard-killed process cold-starts pristine (deferred persistence,
   §5).

---

### 2.2 F3 — foreground service + the countdown notification

**Files:** `features/pomodoro/PomodoroScreen.kt` (tap-start/stop seam) +
**new** `services/TimerForegroundService.kt` (package
`com.class1c.thyme.services`) + `AndroidManifest.xml` (service
declaration + type + permissions) + `ui/icons` only if you draw a
notification icon variant. The repo stays untouched — the service is an
observer of the session flow, not an editor of it.

| File | Why this file |
|---|---|
| `features/pomodoro/PomodoroScreen.kt` | `startForegroundService` is only legal as a **user act** — the control-row click handlers are the single place "user intent" and "OS service start" meet. The repo must stay UI-free, so the seam sits in the screen |
| **new** `services/TimerForegroundService.kt` | One OS-visible role earns one component: process keeper + notification renderer. Own file/package (`services/`) because it is neither state (`data/`) nor a screen (`features/`) |
| `AndroidManifest.xml` | OS registration lives nowhere else: the service entry, its `foregroundServiceType`, the two permissions |
| `ui/icons` (only if a variant is drawn) | The notification's small icon; untouched otherwise |
| — why *not* the repo/VM | Single-actor discipline: the service only **observes** the session flow and re-renders; no contract grows, no state is written by it |

The FGS keeps the process out of the cached bucket so the phase can
actually *pass* in the background. It is a display shell, nothing more —
every decision still lives in the repo/receiver (**single-actor
discipline**: service renders, never decides).

- **Sibling warning** (the same trap the receiver sitting exposed): the
  Service is **also an OS-instantiated component** — a class declared
  in the manifest, not a runtime construct the app creates. The chain:
  *user act → `startForegroundService()` → the service's
  `onStartCommand`* — the screen only *starts* the declared class; no
  code ever "builds" the service object.

- **Manifest:** a `Service` bound to a constant id/notification channel;
  `foregroundServiceType="specialUse"` with `FOREGROUND_SERVICE` +
  `FOREGROUND_SERVICE_SPECIAL_USE` permissions (14+ requirement per F).
- **Start only from a user tap.** The screen's Start (and the
  play-tap-resume path) calls `startForegroundService` + immediately
  `startForeground` inside `onStartCommand` (5-second window — miss it and
  the OS crashes you). A `LocalContext`-based effect in the control-row
  click handling is the natural seam; the repo stays UI-free.
- **Continuing needs nothing.** Auto-start transitions, skip-to-running,
  resume — all change state *inside the running service's lifetime*, so
  the service simply re-renders its notification; no new
  background-start happens. This is why "start from tap" doesn't fight
  auto-start: the tap was one single entry, phases roll within.
- **Notification:** channel created once; content = phase label + note,
  `setWhen(endTime)` + `setUsesChronometer(true)` → the **OS** renders
  the countdown, ticking seconds without any per-second app work (this is
  your "show remaining work/break time" ask — it was locked as F3 all
  along). `setOnlyAlertOnce(true)` so phase changes don't re-buzz.
- **Stop:** pause tap, run end (auto-start OFF landing), and reset —
  `stopForeground(STOP_FOREGROUND_REMOVE)`. If the process freezes anyway,
  F4's alarm is the belt over this suspenders.
- **Trap:** service updates for auto-start must be driven from *state
  observation* (it can collect the session flow), not from new intents —
  one source, re-render on change.

**Status (2026-10-23 — landed & battery green):** all pieces in place.

- **Shape as landed:** `TimerForegroundService` (`@AndroidEntryPoint`,
  injected `TimerRepository` interface); a service-life scope declared
  as a plain `val` property (`SupervisorJob() + Dispatchers.Default`,
  the §DI precedent, cancelled in `onDestroy` — declaration-time init
  is safe here precisely because it references nothing injected);
  `onCreate` = `super` FIRST (Hilt injection lands inside that call —
  launching the collector before it risks reading an un-injected
  `lateinit`), then the channel, then **one** `scope.launch` collector
  over the session flow (never-returning `collect` hosted by the
  scope: the observer's life ends with the service's by cancellation;
  GlobalScope and the injected `@ApplicationScope` were both rejected
  — the former un-cancellable, the latter outliving restart cycles).
- **Render per emission:** private `render(session)` builds a fresh
  notification under the constant id `1` — any build-once field dies
  at construction (`lateinit` before injection) or goes stale at the
  first phase roll (notifications freeze at `build()`). The trio =
  `setWhen(endTime)` + `setUsesChronometer(true)` +
  `setChronometerCountDown(true)`; `setOnlyAlertOnce(true)`.
- **Verbs in their homes:** `startForegroundService(Intent)` exists
  only in the control-row click lambdas (user act; after routing the
  repo event); `startForeground(1, render)` is the mandatory *entry
  ritual* in `onStartCommand` (unconditional, before any state check —
  the 5-second law), returning `START_NOT_STICKY`; the collector
  running-branch does `notify(1, render(s))` (updates go to the
  NotificationManager under the same id), the else-branch owns every
  stop (`stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()`).
  One-way flow: the screen only ever **starts** — stopping is the
  service's own observation decision.
- **Battery, five boxes green:** start tap shows a countdown that
  descends in sync with the ring; pause → notification vanishes
  (REMOVE semantics); skip/auto-chain re-renders with no new
  foreground start; swipe-from-recents leaves it gone for good
  (`START_NOT_STICKY`); screen-off countdown keeps descending — the
  F3 payoff itself.
- **Review catches (kept as lessons):** the construction-time
  `val notification = ...` (a renamed, disguised repeat of the same
  bug — field initializers run before injection); `onStartCommand`
  calling itself instead of returning `NOT_STICKY`;
  early-return-before-promote (violates the 5-second law);
  super-ordering (§ above); collector calling `startForeground`
  instead of `notify`.
- **Deviations recorded:** folder `services/` (doc said `service/`);
  the countdown channel created service-locally in `onCreate`
  (self-contained idempotent choice; `ThymeApplication` remains the
  2.5 chime-channel home); platform `Notification.Builder` flavor
  (`NotificationCompat` optional, not adopted); `IMPORTANCE_LOW` +
  title-as-phase-label named as the follow-up polish row.

### 2.3 F4 — exact alarms (The completion actor)

**Files:** **new** `alarm/TimerAlarmReceiver.kt` + `AndroidManifest.xml`
(receiver registration + `SCHEDULE_EXACT_ALARM` permission) +
`data/repository/InMemoryTimerRepository.kt` (schedule/cancel calls from
start/pause/reset/skip/completion paths) + `di/TimerModule.kt` (if you
introduce an alarm-scheduler abstraction, bind it here) +
`features/pomodoro/settings/PomodoroSettingsScreen.kt` (the
exact-alarm special-access onboarding row, folds into 2.7).

| File | Why this file |
|---|---|
| **new** `alarm/TimerAlarmReceiver.kt` | The **completion actor**: the only component the OS wakes to deliver a completion. Its own file/package because it is an OS entry point — a receiver cannot be "just a repo method", it needs a woken-process entry |
| `AndroidManifest.xml` | Two manifest facts make the alarm system find and trust it: the receiver registration and `SCHEDULE_EXACT_ALARM` |
| `domain/repository/TimerRepository.kt` | Interfaces-first rule: the receiver's one call is a new public promise on the contract |
| `data/repository/InMemoryTimerRepository.kt` | The gated completion entry + the arm/disarm programming. Arm/disarm must **derive from the pair**, and only the repo owns the pair and its writers — a `syncAlarm()` after each writer keeps schedule-ness a projection of session truth |
| `di/TimerModule.kt` | Only if an alarm-scheduler *abstraction* is introduced; injecting `@ApplicationContext` directly into the repo needs no DI change |
| `PomodoroSettingsScreen.kt` | `canScheduleExactAlarms()` false-path users need the special-access onboarding row (folds into 2.7) |

**Mental model first** (this sitting taught it the hard way, so it
lives on the page now): the receiver is **a class the OS instantiates
from the manifest registration** — never a function the app calls, and
never a composable. When it fires, the process may hold no screen, no
VM, no `PomodoroUIState` — the repo singleton is the only truth that
exists in that moment. The receiver's whole role is a **dumb thunk**:
it carries no state, decides nothing, and makes exactly one call into
the repo's gated entry, letting the session flow carry everything
else.

- **Shape checklist (five lines, nothing more):** a class directly
  extending `BroadcastReceiver`; `@AndroidEntryPoint` (the same
  entry-point mechanism as your ViewModel — the woken process gets
  Hilt's installed graph answering); one `@Inject lateinit var` field
  of the **`TimerRepository` interface** (contract, never the impl —
  the binding supplies it); one overridden `onReceive(context,
  intent)` whose body is a single call to the gated repo entry; zero
  Compose imports, ever. Catch-up (2.4) plugs into the repo call,
  chime/vibrate (2.5) into this same receiver moment — later.
- **`setExactAndAllowWhileIdle`** scheduled at every moment a phase gains
  a future `endTime` (start, resume, skip, auto-start chain) — **gated
  on the pair, never `endTime > 0` alone**: under F2-B a paused session
  keeps its fiction `endTime`, so that test no longer implies running.
  On pause/reset, **cancel** the pending alarm explicitly — under B,
  pause keeps the timestamp, so the explicit cancel is the only thing
  that un-arms a paused run; the next phase's alarm must not fire from
  a dead one.
- **Permission:** `SCHEDULE_EXACT_ALARMS` is no longer default-granted vs
  targetSdk 37 — check `canScheduleExactAlarms()` and fall back to plain
  `setAndAllowWhileIdle` (inexact: completion may be delayed minutes) plus
  an onboarding row (folds into 2.7) linking to the special-access
  settings screen.
- **Traps:** same `PendingIntent` request code so cancel-overwrite works;
  the receiver waking a *frozen* process is normal (the OS spins the
  process up to run it — that's exactly the moment Hilt's singleton
  session is already loaded, no work needed); the receiver must never
  touch composables — routes through repo singleton only. The skeleton
  sitting's named catches: **(a)** the composable/anonymous-object
  inversion — an `object : BroadcastReceiver()` built inside a runtime
  construct is *not* the manifest-registered receiver; the OS builds
  its own from the declared class, so *declare the class*. **(b)**
  `PomodoroUIState` as a parameter — a screen projection has no
  existence at fire time (no VM, no subscriber passed it in); the
  receiver holds only what Hilt injected. **(c)** `advancePhase()`
  stays private — the public gated entry (`completeExpiredPhase()` on
  the interface, interfaces-first rule) is where **both interval
  guards** live: `!isRunning → return` and `endTime > now → return`;
  the receiver stays one line. **(d)** equality guards don't exist:
  two independent clocks never compare equal by luck — the fired
  deadline is already a few ms in the past, and inexact-fallback
  delivery can be minutes late; the premise is the fenced interval
  `isRunning && endTime <= now`.

### 2.4 F8-A — fast-forward on wake (LOCKED: decision = A)

**Files:** `data/repository/InMemoryTimerRepository.kt` (the catch-up
loop beside `advancePhase()`) + `alarm/TimerAlarmReceiver.kt` (hook 1)
+ `features/pomodoro/PomodoroViewModel.kt` (hook 2: init re-entry call).
Optionally `features/pomodoro/PomodoroProgress.kt` if you adopt the
"phases moved forward" caption.

| File | Why this file |
|---|---|
| `data/repository/InMemoryTimerRepository.kt` | Catch-up is pure **state math over timestamps**: consume every expired phase via the same landing machinery. Belongs beside `advancePhase()`, which it reuses |
| `alarm/TimerAlarmReceiver.kt` | Hook 1 — the receiver can be invoked into a process carrying a stale session (missed alarms, frozen survivor) |
| `features/pomodoro/PomodoroViewModel.kt` | Hook 2 — app re-entry: the VM init is the session's first collector in a warm process |
| `PomodoroProgress.kt` (optional) | The "phases moved forward while away" caption is presentation, not state |

Where a stale `endTime` exists (frozen-survivor state, alarms missed
or fully dead device), the contract is:

- **Replay the past in faith of the timestamps:** while
  `session.isExpired` (the fenced premise — `isRunning && endTime <= now`;
  the §2.1 trap table is what keeps a *paused* session out of this
  loop): apply a completion. Each application moves the
  clock forward per the 1.2 table; the loop exits when a future `endTime`
  exists (auto-start ON: wake mid-cycle, running) or when the landing is
  paused-full (auto-start OFF: exactly like waking up having missed the
  bell once).
- **One chime total per catch-up** — 2.5's chime belongs to the *reentry
  notification moment*, not to each simulated completion.
- **Hook points (both):** the alarm receiver (it can be invoked into a
  stale process) **and** a reentry call from the app side (VM init —
  first collector of the session). The `endTime > now → no-op` completion
  guard from 2.1 makes both hook-points co-exist without double-advance.
- Pristine session after a hard kill: no stale timestamp → no-op — the
  gap is the deferred persistence item, not this feature's defect.
- Optional screen caption ("phases moved forward while away") — taste,
  not required; record either way.

**Status (2026-10-24 — landed & battery green on the test device):**

- **Shape as landed:** a private `catchUp()` beside `advancePhase()`
  — a `while` loop over the fenced premise, **recomputed fresh on
  every pass** (`isRunning && endTime <= System.currentTimeMillis()`,
  a new clock read each time, never a carried local), each pass
  applying one completion by delegating to the **same
  `advancePhase()` machinery**. No separate "should it continue?"
  logic exists: the loop's exit is the landing ternary itself —
  auto-start ON arms a future `endTime` (wake mid-cycle, running),
  auto-start OFF writes `endTime = 0` + paused-full, and either kills
  the premise.
- **Why fresh-condition (the loop lesson):** evaluating the premise
  once into a local before the loop would mean looping on the opening
  instant's answer while `advancePhase()` rewrites `endTime` under it
  — every later pass tests stale fiction. Deadline-based regimes
  recompute their deadline at every decision point; the loop head is
  a decision point.
- **Single public gate:** `completeExpiredPhase()` remains the only
  public entry on the interface — it re-checks both interval guards
  at the call moment, then invokes `catchUp()` (stays private). Both
  hooks are single calls into it: hook 1 (receiver) — **unchanged**,
  still the one-line thunk, it already pointed at the gated entry;
  hook 2 (VM init) — a first-statement call inside the existing
  `viewModelScope.launch`, so the init block reads
  catch-up-once + clock-loop-only. The two hooks co-exist without
  double-advance for free: the second call's premise is already false.
- **One-chime-per-catch-up — honest structural note:** a burst of N
  completions produces one session emission per pass (each
  `advancePhase()` writes a fresh copy). StateFlow conflation usually
  collapses the burst to at most one *observed* emission, but the
  "exactly once" guarantee is **not structural** — it is an outcome
  of emission mechanics. The structural guarantee arrives with 2.5's
  receiver handover (one call → one answer → one bell). Noted, not
  chased this phase.
- **Scope honored, unchanged:** a hard-killed process cold-starts
  pristine — no stale timestamp to replay, so no-op; the deferred
  persistence item remains the owner of that gap.

### 2.5 F5 — chime + vibrate

**Files:** `alarm/TimerAlarmReceiver.kt` (the only chime site) +
`ThymeApplication.kt` (notification channels created once at
`onCreate`, sound/vibration configured) + optionally
`res/values/strings.xml` for the completion texts.

| File | Why this file |
|---|---|
| `alarm/TimerAlarmReceiver.kt` | Single-actor rule: only the completion actor's moment may chime — the FGS never does, even mid-foreground |
| `ThymeApplication.kt` | Channels are process-once setup, so they belong at app `onCreate` |
| `res/values/strings.xml` (optional) | The completion texts, if composed text prefixes the system sound |

- Fires **only** from the receiver (2.3) — the single-actor rule again:
  the FGS never chimes, even mid foreground. In-app screen visuals fall
  out of the session flow naturally (the VM replaces UI on the session
  update).
- Modern channel with sound + `NotificationChannel.enableVibration`;
  system default alarm sound first (`RingtoneManager` type alarm) —
  composing a custom sound is a later taste.

**Status (2026-10-24 — landed & battery green, as a service-side
experiment):**

Landed shape **deviates from the table above**: the chime did not go
into the receiver — it lives in the foreground service's collector.
Recorded as a deliberate experiment with a scheduled migration; the
table's final shape stands. The service version's known gaps: it goes
silent when the process is frozen or the service already stopped —
exactly the hard moments the alarm exists for — and its
chime-per-catch-up is conflation luck, not structure (§2.4's note).
Neither gap is this-phase chase material; both buy their fixes on the
receiver handover below.

- **Two channels, one constants home:** `ThymeApplication`'s
  companion owns `PHASES_CHANNEL_ID` (`"ringtone"`,
  `IMPORTANCE_DEFAULT` — sound + vibration, no heads-up banner) and
  `TIMER_CHANNEL_ID` (`"timer"` — the countdown card's silent-ish
  home). One manager (the phone-wide notification console) is fetched
  **locally by each component** that posts — nothing shares the
  object, only the name constants travel between files. Any component
  may post onto any channel; the pairing that must match is the id
  string at registration == the id string at post.
- **Channel settings are read at registration:** the phases channel's
  vibration (`enableVibration(true)`) and sound (`setSound(
  RingtoneManager.getDefaultUri(TYPE_RINGTONE),
  AUDIO_ATTRIBUTES_DEFAULT)`) are set **before**
  `createNotificationChannel` — edits to the object after
  registration do nothing. Recorded deviation: the doc-planned system
  **alarm** sound landed as the system **ringtone** sound (deliberate
  choice this sitting); TODO carried: let the user choose
  alarm-vs-ringtone. Practical debug rule learned: channel settings
  belong to the **first** registration, so fixing sound/vibration in
  code means **bumping the channel id** — otherwise the system keeps
  the original registration.
- **Trigger = state comparison, never a clock check.** Two rejected
  attempts, kept as lessons: (1) `endTime <= now` in the collector
  — at a real completion the collector's next message already shows
  the **new** phase with a **future** deadline, so the past-deadline
  instant happens inside the repo between emissions; the check either
  never fires or fires at an unrelated mid-run moment. (2)
  text-vs-text — only works while each phase's label happens to be a
  distinct string (two breaks sharing one line would kill it), makes
  presentation drive machinery, and smuggled a mutation into a
  supposedly pure mapping. Landed detector: a nullable
  `currentPhase` memory in the service; ring iff
  `currentPhase != null && s.timerPhase != currentPhase` (null guard
  ordered first — its second half alone is true at the first message
  and rings a phantom bell at every service start). Renderers are
  now pure label maps; the collector owns every write to the memory.
- **The memory-write placement bug (this sitting's slogan bug):** the
  assignment `currentPhase = s.timerPhase` first lived **inside the
  ring-`if`** — a guard that needs a non-null memory to open, fed by
  the assignment buried behind it → the memory stayed null forever
  and **no bell ever rang**. Fix: the assignment runs on **every
  message** as its own statement after the ring-`if`; the first
  message records silently (that silence is the same null guard's
  anti-phantom job).
- **Ordering rule — ring before stop:** the ring step runs
  unconditionally **before** the running/else split. The auto-start-
  OFF landing is `isRunning = false`; rung first, its bell posts and
  only afterwards does the else-branch remove the countdown card
  (`stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()`) — the
  bell card (id 2) is an independent post and survives the removal.
  Supporting rule kept: the service reads **state, never settings**
  — auto-start's outcome is already baked into `isRunning` by the
  repo's landing; the service never re-asks.
- **Cards:** countdown id 1 keeps the chronometer trio +
  `setOnlyAlertOnce`; bell id 2 (on `PHASES_CHANNEL_ID`) is a plain
  card, no chronometer — title "Phase has changed.", the new phase's
  label as text. Both renderers take the collector's own `s` — the
  habit of reaching into `timerRepository.session.value` inside a
  renderer was chased out of both builders.

Battery (POCO 2602BPC18G, Android 16, HyperOS 3.0;
`POST_NOTIFICATIONS` verified `granted=true` on device): a live
transition rings exactly once with the new phase's text; pause → no
bell, card vanishes; resume → no bell, card returns correctly
anchored; reset → silent; service/timer start → silent (no phantom
from the null-memory guard); auto-start-OFF landing → bell rings,
then the countdown card disappears.

**Open items (recorded; 2.5 not fully closed until these clear):**
1. ~~Receiver handover~~ ✅ **closed 2026-10-25 (addendum 3): the
   bell's final shape — receiver-side, gated on the Boolean.**
2. **Sound user choice** (TODO sits on the sound line in
   `ThymeApplication`) — the only 2.5 item left.

**Addendum (2026-10-25 — the popup sitting; every open verifier
above is now resolved or reworded):**

- **Heads-up popup landed on the bell card:** the phases channel's
  importance raised to `IMPORTANCE_HIGH`. Plainly: the importance
  scale is numbered in the phone's own receipts — **3 = default**
  (shade + sound, no popup), **4 = high** (adds a "banner that
  slides down over whatever is on screen, then retreats"). The bell
  card now pops; the countdown card stays quiet as designed.
- **The id-versioning rule justified itself twice in one sitting.**
  (1) The first attempt raised the importance *without bumping the
  id*: `dumpsys notification` proved the phone still enforcing the
  Monday-era registration (`mImportance=3`) — declaring a category
  whose id already exists is a no-op; only a **fresh id** petitions
  fresh settings. Restated for every future channel change: code
  fixes to a channel's settings always mean a new id. (2) The
  countdown channel had meanwhile been promoted to popup level **on
  the phone** (its receipt: `mOriginalImp=3` beside
  `mImportance=4` + the user-locked flag) — and a setting a human
  changed on the phone outranks code forever. The code-side escape
  was again the fresh id (`"Timer"`), re-petitioning the code's own
  default-importance settings. Final landed ids: `"Ringtone3"`
  (bell, HIGH) / `"Timer"` (countdown, default).
- **Sound settled at the notification chime.** A trial used the
  `TYPE_RINGTONE` constant and played the **incoming-call melody**
  — the constants select different system sounds:
  `TYPE_RINGTONE` = the phone-call ring, `TYPE_NOTIFICATION` = the
  short shade chime, `TYPE_ALARM` = the alarm tone (this block's
  original bullet). Landed: `TYPE_NOTIFICATION`; the TODO reworded
  to "let the user choose notification or ringtone".
- **`android.permission.VIBRATE` added to the manifest** — the last
  open verifier cleared; sitting reported green.
- **Ghost channels remain registered on the device** (the retired
  ids `"ringtone"`, `"Ringtone"`, and whatever preceded) — nothing
  posts to them anymore, but they still show in the phone's
  settings list. Candidates for the one-line
  `deleteNotificationChannel` sweep; cosmetic, recorded so the
  list of dead categories can't start a future debugging session.

**Addendum 2 (2026-10-25, later — the tap-to-open sitting):**

- **Both cards now open the app on tap.** Each renderer builds the
  open-app envelope **at render time** (a legal moment, per the
  timing rule below): the app's own launch intent — **fetched**
  from the package manager (`getLaunchIntentForPackage`), not
  hand-constructed against `MainActivity::class.java`, so the
  manifest's launcher registration stays the single source of the
  entry-screen fact — wrapped by `PendingIntent.getActivity`
  (`FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`; request code 1 shared
  by both cards, legitimate because the destination is identical).
  Terminology pair taught this sitting: a plain `Intent` is the
  message you mail yourself now; a `PendingIntent` is a pre-signed
  envelope handed to the phone, which may mail it later with your
  signature. Bell card `setAutoCancel(true)` (tap = open + clear);
  the countdown card's `autoCancel` is a dead flag — a foreground
  card is system-pinned while its service lives, un-swipeable and
  un-tappable-away — noted inert, not removed.
- **The crash lesson — third meeting of the declaration-time
  family, so the rule is now final-form:** property initializers
  run inside the constructor, **before the framework attaches the
  context and before Hilt injects**. Any declaration-level `val`
  reading `applicationContext`, building Android objects, or
  touching injected fields crashes the construction ("Unable to
  create service": null-resources NPE, or
  `UninitializedPropertyAccessException`) — this sitting produced
  device receipts of all three shapes in one day. **Class-level
  `val`s may reference only things constructible from nothing
  (the service's scope); everything else is built inside
  `onCreate` after `super`, or at render time.** Its sibling catch:
  `MainActivity()` hand-built at declaration level — the OS is the
  only legitimate constructor of an activity, and the husk was
  only being used as a class label, which never needs an instance.
  Deleted.
- **Review catches (principles over accidents):** the fetch's
  package argument appeared as the literal string
  `"com.class1c.thyme"` — a second copy of a build-config fact;
  the fix pattern is the context property
  (`applicationContext.packageName`), and one renderer still
  carried the literal at record time. On the shelf: both
  renderers duplicate the same ten-line envelope — the
  label-map-shaped candidate for one small private helper.
- **New API taught: the package manager** — the app-registry clerk
  (`context.packageManager`, distinct from `getSystemService`'s
  system-services desk): queries installed apps including one's
  own; `getLaunchIntentForPackage` returns the launcher-shaped
  intent or null (unreachable for an app with a launcher entry);
  `applicationContext.packageName` is the self-identity, never
  typed by hand.

**Addendum 3 (2026-10-25, evening — the receiver handover; 2.5's
final shape):**

- The open item is closed: the bell moved from the service's
  watcher to **`TimerAlarmReceiver`, gated on a Boolean answer**.
  Contracts-first ran in the order it preaches — interface
  (`completeExpiredPhase(): Boolean`) → implementation → receiver →
  service shrunk — and the two-week collection of bell bugs left
  with the machinery it lived in (the slogan placement bug, the
  phantom-start bell, the null-memory trap: gone **by deletion**,
  not by fix).
- **The Boolean's semantics, recorded precisely:** the answer is
  "the completion premise held at the moment of the call" — which
  guarantees `catchUp()` consumed at least one phase (its loop
  requires that same premise). The no-op guard paths (paused /
  future deadline / not running) answer false; the receiver rings
  only on true — stale or early alarm wakes stay silent with zero
  memory needed.
- **Receiver landed shape:** `onReceive` unwraps the passed-in
  context first (`null → return`), fetches the notification console
  from it, and the whole body is *one call → one conditional → post
  card 2 on the phases channel*. The card builder moved over from
  the service, now taking the message's own session **and the
  passed context**; the tap-to-open envelope rebuilds with
  `context.packageName` (the drift-free form). The dumb-thunk
  survives: the receiver decides nothing about timer state; the
  VM's re-entry call keeps ignoring the Boolean — only the receiver
  consumes the answer (the single-actor rule, honored at the call
  sites).
- **Service shrunk back to the one-job shape:** the watcher renders
  the countdown card while running, removes + stops otherwise — no
  ring block, no phase memory, no null guard, no completion
  builder.
- **Verified lesson 1 — the stateless receiver is structural, not
  stylistic:** the first attempt resurrected the service's
  remember-and-compare detector (a `currentPhase` field) inside the
  receiver, and silently never rang: the OS constructs a **fresh
  receiver object per delivery** and discards it after the call —
  a field cannot survive to become a memory, so `currentPhase` was
  null at every wake and its guard could never open. The Boolean
  made the memory unnecessary; the "receiver carries no state" rule
  was proven on device, not just read from the page.
- **Verified lesson 2 — a receiver is not a Context:** the "how do
  I get `applicationContext`" struggle produced
  `val applicationContext = ThymeApplication()` — a hand-built
  Application husk, the second-construct-OS-component family's next
  member after `MainActivity()`. The correct shape: the context
  **arrives as a parameter** of `onReceive`, is guard-unwrapped,
  and serves everything (manager fetch, envelope, builder). The
  husk was deleted in favor of the parameter (it never fired a code
  path this sitting — its crash was waiting for its first caller).
- **Behavior deltas, all per the locked F5 design, accepted
  consciously:** manual skip is silent from now on (a user act is
  not a deadline completion); deadline completions ring even with
  the app open (the receiver is blind to the UI); a catch-up burst
  rings exactly once, structurally (one call → one answer).
- **Battery:** user-verified green this sitting — the deadline bell
  rings with the app closed; expected-silent cases stay silent.
- **Review catch recorded:** a dead
  `import com.class1c.thyme.ThymeApplication` remains in the
  receiver (only the companion import is used) — delete on next
  touch.

### 2.6 F6 — `POST_NOTIFICATIONS` runtime permission (13+)

**Files:** `features/pomodoro/PomodoroScreen.kt` (first-Start request
seam) + `AndroidManifest.xml` (permission declaration) +
`features/pomodoro/settings/PomodoroSettingsScreen.kt` (the
grant-state row + app-info deep link for permanent denial).

| File | Why this file |
|---|---|
| `features/pomodoro/PomodoroScreen.kt` | The request happens at the **first Start tap** — the strongest UX link (the toggle now advertises a notification), and it's a user moment like the FGS start |
| `AndroidManifest.xml` | The `POST_NOTIFICATIONS` declaration (13+ runtime permission) |
| `PomodoroSettingsScreen.kt` | Permanent-denial surface: the settings row + app-info deep link are secondary paths when the dialog is exhausted |

- Request at the natural moment == first Start tap (strongest UX link:
  the toggle now advertises a notification). Handle permanent denial
  (row-off → 🚫, setting row offering the app-info deep link).
- All FGS/notification posting defends on the grant; denial degrades to
  zero notifications, timer still runs.

**Status (2026-10-25 — request seam landed & fresh-install prompt
verified; sitting in progress):**

- **Half pre-paid:** the manifest claim was already declared during
  the earlier notification work, and the test device's grant was
  already user-fixed from a prior sitting — so the fresh path was
  unreachable on that phone. The unlock: **an uninstall/reinstall
  resets the permission ledger**, and the dialog exists again.
  Recorded as the standard test-door for this whole block.
- **Seam shape as landed:** the permission launcher
  (`rememberLauncherForActivityResult` + the request-permission
  contract) lives at screen level; the **Start tap** asks the phone
  live (`checkSelfPermission`, behind the SDK ≥ 33 guard — minSdk 28
  needs it) and launches the dialog only when ungranted; the event
  flow (repo start + foreground-service start) runs regardless of
  the verdict — deny-then-degrade is structurally true before any
  code says so.
- **The first attempt deleted, and the design delta it left:** a
  plain `var firstTap` local was the original once-only guard — a
  per-recomposition ghost (the third species of "state lives where
  the ownership lives": a Composable's locals re-run from the top
  every frame, so it could not remember across taps). Deleted rather
  than replaced with remembered state: the landed design **re-asks
  on every Start tap while ungranted**, and bounds the nag with the
  *system's own* patience — after two denials the system stops
  showing the dialog and `launch()` returns "denied" silently. Every
  re-launch also coincides with a fresh user-intent tap, which is
  the legitimate re-ask moment. Recorded deviation from "ask exactly
  once," accepted consciously; the revoked-side battery is what
  certifies the bound holds.
- **Callback is an empty stub, deliberately:** nothing must react to
  verdicts yet — the courtesy "the bell stays silent until enabled"
  text is a taste row that would live exactly here.
- **Remaining for 2.6:** the settings-screen **grant-state mirror**
  (a live-checked row: off → visible + tap opens Thyme's system
  notification-settings location — a deep link to the user's own
  app, always legal; no screen, no storage) and the **revoked-side
  battery** (deny twice → no dialog, no nag, no crash; one full
  card-less cycle).

**Sitting 2 (2026-10-25, later — the grant-state mirror landed;
one battery left):**

- **The mirror landed:** a settings row rendered only under the live
  `checkSelfPermission` read (recomputed at composition — no
  storage, the phone owns the grant ledger); hidden when granted.
  No new screen, no DataStore key — the row is a reader with a map,
  never a toggle: the app cannot write permission state, and no API
  exists to let it.
- **The row's action is the deep link, and the dialog-replay draft
  died in review:** the first draft replayed the permission dialog
  — a dead end in *exactly the state the row exists for* (the
  system stops showing the dialog after two denials; replaying
  `launch()` there is a silent no-op). Landed design: the row opens
  **the system's notification-settings page for Thyme**
  (`ACTION_APP_NOTIFICATION_SETTINGS`) — the switch that never
  exhausts. Lesson recorded: the exhaust-recovery surface must act
  with tools that cannot exhaust.
- **Deep-link facts:** opening one's **own** app's settings page
  needs no permission (self-navigation is always legal);
  `FLAG_ACTIVITY_NEW_TASK` kept as belt-over-suspenders from an
  activity context; the old `SDK_INT >= 33` guard around the
  navigation is now unnecessary (the action is API-21-era, far
  under minSdk 28) — recorded as harmless leftover, not to be read
  as "the deep link is API-33-gated."
- **The sitting's lesson — the same toast, twice, by two different
  envelope mistakes.** The user's symptom ("the app wasn't found in
  the list of installed apps") was produced by **two distinct bugs
  in sequence**: (1) the `EXTRA_APP_PACKAGE` extra absent entirely —
  the intent left with no address (the page had no which-app
  answer); (2) after adding one, the **key and value traded
  places** — `putExtra(payload, label)` — so the page searched for
  a package literally named `android.provider.extra.APP_PACKAGE`.
  Bare the contract plainly: `putExtra` reads **label-then-payload**
  — the system-defined name in the key slot, `context.packageName`
  in the value slot. Sister entries in the compute-deliver family:
  the ringtone sound computed and never attached, the tap-envelope
  built but not bound. Improvisation during debugging (swapping the
  action to plain `ACTION_SETTINGS` — an extra-less action, so it
  "worked") is recorded as a diagnostic move, not a fix: it lands
  on the Settings app's *home*, leaving the user to find the app —
  a working-looking wrong address.
- **Batteries — closed green, user-verified on device (2026-10-25):**
  the permission-prompt paths were exercised this sitting ("i have
  tested the permission prompts") — the denial-patience shape
  (dialog stops returning after exhaustion, no nag, no crash) and
  the denied→recovery path (row → system page → grant → bell
  return). Recorded as the checking close-out; both boxes from the
  list below hold.

### 2.7 F7 — HyperOS onboarding

**Files:** `features/pomodoro/settings/PomodoroSettingsScreen.kt` (the
two rows + fallback copy) + `AndroidManifest.xml`
(`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) +
`res/values/strings.xml` (the guidance copy).

| File | Why this file |
|---|---|
| `PomodoroSettingsScreen.kt` | The two onboarding rows mirror **live** system state (exemption granted? autostart enabled?) — a settings surface, not a one-shot dialog |
| `AndroidManifest.xml` | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, the only permission needed for the exemption request |
| `res/values/strings.xml` | The guidance copy (HyperOS-specific instructions can't auto-translate) |

- Battery-optimization exemption request (package-claimed) with a
  settings row mirroring the state; fallback copy if the system dialog is
  unavailable on some ROM.
- MIUI autostart guidance row: attempt the known autostart intent,
  `ActivityNotFound`-caught → instructional copy fallback.

**Status (2026-10-25 — the battery-exemption row landed, battery
green):**

- **Manifest claim** — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` added,
  correct at the first attempt: permissions are declared facts, not
  behavior, so there was nothing to get wrong once the name matched.
- **The mirror is the exemption, not the request permission.** First
  draft read `checkSelfPermission(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)`
  — but that permission is **install-time ("normal")**: granted with
  no human, so it can't track what the user *does*, and it answers
  "may I ask?" rather than "am I exempt?" — a mirror one layer above
  the truth. Landed: the **PowerManager** console
  (`getSystemService(PowerManager)`) and
  `isIgnoringBatteryOptimizations(packageName)` — live, the same
  read-the-owner pattern as the notifications mirror and
  `canScheduleExactAlarms()`.
- **The action is the direct confirm dialog, after two wrong-doors.**
  Draft history, all real: (1) `ACTION_IGNORE_BATTERY_OPTIMIZATION_
  SETTINGS` (plural, the *global list*) with the per-app extra
  attached — the list door **reads no recipient shelf**, so the
  extra was ignored; symptom "opens nothing / no error". (2) After
  learning the **data shelf** (`setData` takes a parsed **Uri**, not
  a string — `("package:" + packageName).toUri()` is the idiom; the
  compiler's refusal was a guard, not an obstacle), the **plural
  constant stayed** — right letter, wrong door, still silent. (3)
  The one-constant fix: `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  + the package-scheme data URI → the system's own allow/reject
  dialog for exactly this app. Recorded as a general law: **the
  action decides which shelf the receiving screen reads** — a
  letter on an unread shelf is indistinguishable from no letter.
- **The intent's three shelves, tabulated for the house file:**
  *action* (the door — constructor/`setAction`), *data* (the
  addressed thing — `setData`/parsed Uri; read by the battery
  dialog's door), *extras* (`putExtra` labeled pockets; read by the
  notification-settings door). Not interchangeable; each door's
  contract names its shelf.
- **Play-policy lint warning: acknowledged and kept.** Lint flags
  the permission + intent as violating Google Play's acceptable-use
  rules for this special access. Reasoning recorded: the app is
  local/sideloaded (the policy governs *Play distribution*); a
  pomodoro timer sits squarely in the policy's own acceptable-use
  family (must survive Doze, like alarm clocks); the **direct
  request dialog was chosen over the list route deliberately** —
  it's the more honest ask even though it's the linted one. If Play
  ever happens: acceptable-use justification at review, not
  removal.
- **Battery: user-verified green** this sitting — dialog appears
  from the non-exempt state, exemption lands, row hides per the
  mirror. (Shell receipt available anytime: `dumpsys deviceidle
  whitelist` should list the package.)
- **Remaining in this task:** the **autostart guidance row**
  (attempt-the-intent, caught-fallback copy — HyperOS has no stable
  public entry), the **exact-alarm guiding row** (mirror
  `canScheduleExactAlarms()`, open `ACTION_REQUEST_SCHEDULE_EXACT_
  ALARM` — the self-openable toggle HyperOS buries), and the
  **fallback copy** (`strings.xml`, shared by the fallback paths).
- **Micro-sweep on the shelf:** with `toUri()` doing the parsing,
  `import android.net.Uri` became unused — retire on next touch.

---

## 3. Phase F device checklist

Foreground sanity first (Phase-E paths must not regress):

- [ ] Phase-E §3 checks still pass after 2.1's rewrite (the machine's
      outward behavior must be bit-identical in-app). *Full battery is
      only meaningful once the §2.3 receiver skeleton lands; the
      demolition interim battery lives in §2.1 item 1.*
- [ ] 00:00 never lingers on screen past the completion beat.

Background/scene battery tests (screen OFF, 10-min cycles):

- [ ] Timer running → screen off → notification countdown keeps
      descending; FGS survives several minutes before HyperOS's
      reclaim, then the alarm catches the boundary (a few seconds of
      drift is acceptable; the catch-up erases it).
- [ ] Auto-start ON + screen off for ~3×0:05 cycles → three-time boundary
      wake works, chain survives, single chime per completion.
- [ ] FGS hard-stopped (swipe from recents) mid-phase + alarm still
      scheduled → completion band still arrives (notification + chime).
- [ ] Overnight: deep-sleep w/ ~4 cycles → 2.4 catch-up lands the wake in
      a sane state; exactly ONE chime; auto-start OFF landing == paused.
- [ ] Pause during a live notification → notification disappears; alarm
      canceled; re-entry state sane.
- [ ] Canceling/uninstall of notification permission mid-run → service
      still guards (no crash); posts no-ops.
- [ ] Battery-optimization exempt request + MIUI autostart rows work and
      reflect actual system state.
- [x] `canScheduleExactAlarms()` false → fallback path still advances
      (slightly late is OK, no crash). *Verified early, during the
      skeleton sitting (2026-10-23): fallback arm carries a ~45 s
      delivery window, completions land, no crash — receipts in
      §2.1 item 2.*

---

## 4. Phase F locked decisions

| # | Decision | Choice |
|---|----------|--------|
| F1 | Sequencing | Phase E closed; 2.1 timestamp model before all F UI/service work; **demolition lands alone, the receiver skeleton (§2.3 first bolt) follows as its own sitting — interim `00:00`-hang accepted (2026-10-21)** |
| F2 | Timekeeping | **Locked 2026-10-21 — model variant B**: stored `isRunning` (constant `false` default only, never a computed default); pair discipline at every writer (`isRunning = true` ⇔ fresh future `endTime`, same atomic copy); `endTime` = "appointment as booked," kept as stale fiction across pause; freeze in `pause()`; sentinel `0` = no-appointment (pristine / auto-start-OFF landing); ticker + job/cancel machinery deleted. Variant A (derive the flag) stays a one-file future swap while the `session.isRunning` access-path rule holds. **Display cadence (locked 2026-10-21, landed 2026-10-22 — user-built frame-clock loop):** vsync-sampled display over the exposed `endTime` (sub-frame accurate; zero work unrendered; `_tick` heartbeat retired; paused = snapshot binding; no fixed-interval pulse — its error is bounded by its period; wall-second alignment and deadline-anchored pulses both rejected). Landed shape + lessons in §2.1 item 1 |
| F3 | Foreground service | specialUse FGS; notification countdown via OS chronometer anchored to `endTime`; starts only from user tap; survives/auto-renders across auto-start; stops on pause/end. **Landed 2026-10-23, battery green** (shape record in §2.2's status) |
| F4 | Alarm backup | `setExactAndAllowWhileIdle` scheduled per phase-start-family; cancel on pause/reset; receiver = the single completion actor. **Skeleton landed 2026-10-23 with green battery** (details in §2.1 item 2's status) |
| F5 | Completion feedback | chime + vibrate from receiver only, exactly once per catch-up burst. **Landed 2026-10-24 as a service-side experiment** (battery green — deviations + final-shape receiver handover recorded in §2.5's status; two open verifiers there). **Addendum 2026-10-25:** heads-up popup on the bell card (channel importance HIGH), sound = the notification chime (ringtone melody rejected by ear), and VIBRATE manifest-cleared — full record in the status's addendum. **Addendum 2 (same day):** tap-to-open envelopes on both cards (launch intent fetched via the package manager — the manifest stays the entry-screen's single source); the declaration-timing crash rule recorded in final form. **Addendum 3 (2026-10-25, final shape):** the bell moved into `TimerAlarmReceiver` gated on `completeExpiredPhase(): Boolean` — stale alarms silent, one-per-catch-up structural, manual skip silent by design; stateless-receiver and receiver-is-not-a-Context lessons verified on device |
| F6 | Runtime permission | `POST_NOTIFICATIONS` requested at first Start; deny = degrade-not-crash. **Landed & closed 2026-10-25:** seam + settings-row mirror, batteries green (fresh-install prompt, denial patience, grant round-trip) — full shape + the envelope-lessons in §2.6's status |
| F7 | HyperOS onboarding | battery-exemption dialog + MIUI autostart guidance rows |
| F8 | Overdue policy | **A — fast-forward.** Trust timestamps: consume every missed phase in order until a future `endTime` (or paused-full landing); single chime; hook points = alarm receiver + app re-entry. **Landed 2026-10-24** (private catch-up loop, single gated public entry, both hooks wired — battery green, full record in §2.4's status) |
| F9 | HyperIsland | Deferred — research task in its own later phase |

---

## 5. Carried / deferred

- **Persistence of session across hard kill** — the one honest gap F2-A
  does not cover: dead process == pristine cold start (no catch-up).
  Overlaps the stats-phase work naturally (one DataStore write of the
  session covers both).
- Work-goal **tally** (accumulated focus minutes to
  `targetWorkMinutes`, counting break time per `countBreaksInWorkTime`)
  + progress UI.
- Gradle carries: pinned `androidx-material3` duplicate deletion (2.0
  item 5) + pin-style consistency (item 6).
- §2.1 housekeeping remainder — "ignore for now" recorded; park until a
  natural visit during 2.1-rewrites in this phase.
- `PomodoroUiState` first-frame TODO (pre-collect defaults) — carried.
- HyperIsland research (F9).

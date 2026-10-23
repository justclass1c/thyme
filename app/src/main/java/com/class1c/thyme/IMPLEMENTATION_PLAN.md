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
| `ThymeApplication.kt` | `@HiltAndroidApp`; **F2.4/2.5's notification channels get created here** |
| `navigation/{AppNavHost,Route,NavigationBar}.kt` | type-safe nav wiring |
| `AndroidManifest.xml` | every OS-visible component declaration: F2.2's service + permissions, 2.3's receiver, F6/F7 permissions |
| `res/xml/*`, `res/values/*` | backup rules, strings/colors/themes |
| Gradle (`app/build.gradle.kts`, `gradle/libs.versions.toml`) | dependency truth; §5 carry items live here |

### 1.5 Phase F task → file map (one row per task; paths relative to `app/src/main/java/com/class1c/thyme/` unless stated)

| Task | Files | State |
|---|---|---|
| **2.1 — timestamp model** | `domain/model/TimerSession.kt` · `data/repository/InMemoryTimerRepository.kt` · `features/pomodoro/PomodoroViewModel.kt` · `features/pomodoro/PomodoroUiState.kt`; display tail: `features/pomodoro/PomodoroProgress.kt` | ✅ landed |
| **2.1 item 2 — receiver skeleton** | **new** `alarm/TimerAlarmReceiver.kt` · `app/src/main/AndroidManifest.xml` · `domain/repository/TimerRepository.kt` · `data/repository/InMemoryTimerRepository.kt` | ✅ landed 2026-10-23 (battery green) — status in §2.1 item 2 |
| **2.2 — F3 foreground service** | **new** `service/TimerForegroundService.kt` · `features/pomodoro/PomodoroScreen.kt` · `AndroidManifest.xml` · (optional) `ui/icons/*` | ⏳ |
| **2.3 — F4 alarms (full)** | `alarm/TimerAlarmReceiver.kt` (grows) · `AndroidManifest.xml` · `data/repository/InMemoryTimerRepository.kt` · `di/TimerModule.kt` (only if an alarm-scheduler abstraction is introduced) · `features/pomodoro/settings/PomodoroSettingsScreen.kt` | ⏳ |
| **2.4 — F8 catch-up** | `data/repository/InMemoryTimerRepository.kt` · `alarm/TimerAlarmReceiver.kt` · `features/pomodoro/PomodoroViewModel.kt` · (optional) `features/pomodoro/PomodoroProgress.kt` | ⏳ |
| **2.5 — F5 chime** | `alarm/TimerAlarmReceiver.kt` · `ThymeApplication.kt` · (optional) `app/src/main/res/values/strings.xml` | ⏳ |
| **2.6 — F6 POST_NOTIFICATIONS** | `features/pomodoro/PomodoroScreen.kt` · `AndroidManifest.xml` · `features/pomodoro/settings/PomodoroSettingsScreen.kt` | ⏳ |
| **2.7 — F7 HyperOS onboarding** | `features/pomodoro/settings/PomodoroSettingsScreen.kt` · `AndroidManifest.xml` · `app/src/main/res/values/strings.xml` | ⏳ |

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
3. ⏳ F8-A's real `catchUp()` (§2.4) — only on the finished model.
   Scope honesty: catch-up covers frozen-survivor wakes only; a
   hard-killed process cold-starts pristine (deferred persistence,
   §5).

---

### 2.2 F3 — foreground service + the countdown notification

**Files:** `features/pomodoro/PomodoroScreen.kt` (tap-start/stop seam) +
**new** `service/TimerForegroundService.kt` (next to `data/`, package
`com.class1c.thyme.service`) + `AndroidManifest.xml` (service
declaration + type + permissions) + `ui/icons` only if you draw a
notification icon variant. The repo stays untouched — the service is an
observer of the session flow, not an editor of it.

| File | Why this file |
|---|---|
| `features/pomodoro/PomodoroScreen.kt` | `startForegroundService` is only legal as a **user act** — the control-row click handlers are the single place "user intent" and "OS service start" meet. The repo must stay UI-free, so the seam sits in the screen |
| **new** `service/TimerForegroundService.kt` | One OS-visible role earns one component: process keeper + notification renderer. Own file/package (`service/`) because it is neither state (`data/`) nor a screen (`features/`) |
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
| F3 | Foreground service | specialUse FGS; notification countdown via OS chronometer anchored to `endTime`; starts only from user tap; survives/auto-renders across auto-start; stops on pause/end |
| F4 | Alarm backup | `setExactAndAllowWhileIdle` scheduled per phase-start-family; cancel on pause/reset; receiver = the single completion actor. **Skeleton landed 2026-10-23 with green battery** (details in §2.1 item 2's status) |
| F5 | Completion feedback | chime + vibrate from receiver only, exactly once per catch-up burst |
| F6 | Runtime permission | `POST_NOTIFICATIONS` requested at first Start; deny = degrade-not-crash |
| F7 | HyperOS onboarding | battery-exemption dialog + MIUI autostart guidance rows |
| F8 | Overdue policy | **A — fast-forward.** Trust timestamps: consume every missed phase in order until a future `endTime` (or paused-full landing); single chime; hook points = alarm receiver + app re-entry |
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

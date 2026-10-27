# Thyme — Glossary

*Words this project coined, and ordinary words this project gave a second
job to. One entry = one breath of definition (a tiny example when it
earns one). New entries append as sittings land.*

---

## The intent envelope (three shelves)

**Intent** — the message object one piece of code hands the system to
deliver. Carries four kinds of cargo: the action, the data, the extras,
and (sometimes) an explicit destination.

**Action (the door)** — a string naming *which screen/process to
deliver to*: built-in constants (`Settings.ACTION_...`), or app-owned
keys (`com.class1c.thyme.SOMETHING`). The action decides the door;
everything else is cargo.

**Data shelf / `setData`** — the intent's *address* slot: a parsed
`Uri` saying *what the message is about*
(`"package:com.class1c.thyme"`, built as `("package:" + packageName).toUri()`).
The battery-optimization dialog's door reads its "which app" from here.

**Extras / `putExtra`** — labeled side-pockets (`putExtra(label, payload)`),
read *by code inside the destination*, by name. The
notification-settings door takes its "which app" here
(`Settings.EXTRA_APP_PACKAGE` as the label, `context.packageName` as
the payload — **label first, payload second**; the label/payload swap
was a paid-for bug).

**The shelf law** — *the action decides which shelf the receiving
screen reads.* The same "which app?" answer rides the data line at one
door and a pocket at another. There is no derivable rule: **foreign
constant → Ctrl+click it and read its one-line contract; own intent →
extras by default, your key, both written ends.** An unclaimed shelf
(letter attached where the door reads nothing) is silently ignored —
indistinguishable from sending nothing.

**Explicit destination / `ComponentName`** — pointing at one screen
*by full name* (package + class) instead of by action
(I say "go to plot 17 on Miller Street" rather than "take me to a post
office"). Technically writing the address; *not* a check —
constructing it never verifies the door exists.

**`ActivityNotFoundException`** — the phone's answer at delivery time
to "no such door at this address." The **only** reliable trigger for a
fallback, because only the phone's answer counts — pre-click checks
re-read your own notes.

**Attempt → caught fallback** — the row pattern for foreign gates:
build, `startActivity` inside `try`, catch
`ActivityNotFoundException` + `SecurityException`, and *change the
row's shape* on catch rather than die or stay silent.

---

## Permissions & the gates

**The phone owns the ledger** — permission/exemption state lives in
system storage, not in the app. The app may only *read it live*; the
perpetual sin is storing a copy and having it rot. (Why rows are
live-recomputed, never persisted.)

**Mirror (a live-checked row)** — a settings row whose very
*visibility* is the answer to its own question, re-read at every
recomposition: "exempt?" → row hidden when true. The app can open
doors; it can never flip switches.

**Reader-with-a-map** — what a correct row is: a read-only display of
someone else's truth, wired to the legitimate door that reaches it.

**Permission species** — three, by grant timing: **install-time
("normal")** — granted with no human (VIBRATE; cannot track user
action, so never mirrors it); **runtime** — asked via dialog, the
system keeps a count (POST_NOTIFICATIONS); **special access** —
toggles in specific system pages (battery exemption, exact alarms,
vendor autostart).

**Two-denial patience** — the system's built-in exhaustion for
runtime-permission dialogs: after two denials it stops showing them.
Capstone lesson: *an exhaust-recovery surface must act with tools that
cannot exhaust* (a deep link, not a replayed dialog).

**Doze** — the phone's deep-sleep state that idles ordinary apps; the
timer's survival depends on the battery-optimization exemption
(`isIgnoringBatteryOptimizations` as its mirror,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` manifest claim to earn the
request dialog).

**Admissions guard / autostart** — the vendor's (MIUI/HyperOS) private
per-app switch: "may this app start running when nobody tapped it?"
No public read, no dialog, no write — only *attempt the folklore
address, else teach the path.* (Installs reset none of these; they
reset the *runtime* ledger.)

**Folklore (in the address sense)** — vendor screen names copied from
community knowledge: real on some builds, rotting silently on others.
Keep only with the catch as the safety net, and label the verified vs
the unverified.

**Package visibility** — from Android 11, an app cannot *see* other
apps' packages unless its manifest declares (`<queries>`) a reason.
Why pre-flight "does this component exist?" resolve-checks can lie,
and why `startActivity` + catch stays the honest probe.

**Wake-up** — unattended machine-initiated start (alarm delivery,
proc-revival, boot). The autostart guard's whole jurisdiction.

---

## State & its species

**State lives where the ownership lives** — the three-species census:
① the phone's system pages (permissions, switches) — read live, never
copied; ② the session in **RAM** (`InMemoryTimerRepository` — one
writer; lost on hard kill, hence no boot receiver yet); ③ **remembered
composition state** (`remember { mutableStateOf }`) — lives as long as
the screen does, resets on return.

**Remembered state** — a composition-local variable that survives
recomposition (re-rendering) but not screen teardown. Enough memory
for "the phone said no" fallbacks without a ledger.

**Shape-changing row** — a row whose content switches on remembered
state (button → instructional copy). Better than a popup: the answer
stays put and reads at the user's pace.

---

## The crash & silence families (paid tuition)

**Compute-and-never-attach family** — the object is *computed* and
then never *delivered/handed anywhere*: a Toast built without
`.show()`, a sound on a builder never attached, an envelope bound too
late. Alias rule: a factory whose result you don't hand to anything
else needs a finalizer or a receiver.

**The format law** — *copy format follows its job:* confirmations fit
transient bubbles (toasts); instruction sets need persisting surfaces
(row copy, strings.xml).

**Declaration-time crash family** — code that runs *at object
creation* (`MainActivity()` / `ThymeApplication()` bodies) executes
before the context attaches / injection completes; anything
context-dependent there stops the machine at launch.

**Promise vs check** — `@RequiresApi(API)` is a *promise to the
compiler's checker* (no runtime effect, wrong to use on broadly
reachable functions); `if (Build.VERSION.SDK_INT >= ...)` is a *check
that decides at runtime*. The real failure an unchecked new API
causes: **`NoSuchMethodError`** — the machine looking up a method the
phone's OS doesn't have, and stopping. Short-circuit `&&` order makes
the check *load-bearing*: unsafe call second, never evaluated when the
first condition fails.

**android.R trap** — autocomplete's wrong `R`: the *platform's own*
resources. The app's `R` is autogenerated into the **namespace
package** (`com.class1c.thyme`); files in subpackages import it by
full address.

---

## Notification apparatus

**Channel** — a named delivery lane (importance decides pop-up vs
silent, etc.). Settings are user-locked at first creation: *an
id-bump rule* ("Ringtone2"→3→4...) is the only migration. **Ghost
channels** — old-bump leftovers still on the device, awaiting a
`deleteNotificationChannel` sweep (polish shelf).

**FGS (foreground service)** — a process kept visibly alive while the
timer runs; only the user tap starts it; it observes and renders, and
only the repository writes.

---

## Verbs of the trade

**Receipt (a device receipt)** — hard evidence from the running
phone, not belief from code: `dumpsys` fingerprints, an appops read,
a logcat line captured before rotation.

**Battery (a test)** — the script of manual checks per sitting; "green"
= run and passed, on the device.

**Wrong door, right letter** — symptom of an intent resolving but
the wrong target or ignoring its cargo (the plural
`...OPTIMIZATION_SETTINGS` list door: legal, silent, useless).

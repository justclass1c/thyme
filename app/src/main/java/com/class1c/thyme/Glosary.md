# Glosary — Thyme's house glossary

Plain-language definitions of the words this project has coined and
reused across sittings. A word earns an entry by being used in
reviews, lessons, or plan records — it is not general Android
vocabulary unless a sitting leaned on it.

---

## The intent's anatomy (the envelope)

**Envelope** — anything sent through `startActivity` (or an alarm
delivery): one object that carries an action, optional data, optional
extras. Called an envelope because the same object holds several
kinds of paper, and the receiver only reads some of them.

**Door** — the screen (or receiver) that an intent's *action* names.
Choosing the action constant is choosing which door opens; the door's
own code decides which parts of the envelope it reads.

**The three shelves** — the parts of the envelope, each with a
setter:

| Shelf | Setter | Read by |
|---|---|---|
| *action* (the door) | constructor / `setAction` | the OS — picks the destination |
| *data* (the address line) | `setData(`/`.toUri()`, a parsed `Uri` | doors whose contract asks "about *what*?", e.g. the battery-exemption dialog, `package:` URIs |
| *extras* (labeled pockets) | `putExtra(label, payload)` | code *inside* the door, after landing, e.g. `EXTRA_APP_PACKAGE` on the notification-settings page |

The shelf is not a style choice: each system action's documentation
names the shelf it reads. Foreign constant → ctrl-click it and read
its one-line contract. Own intent (you wrote both ends) → extras by
default, and the label must match at both ends.

**Label-then-payload** — the `putExtra` reading order: the
system-defined key name first, the value second. Swapping them
produces a door that searches for a package literally named
`"android.provider.extra.APP_PACKAGE"`.

**Unclaimed letter** — envelope content attached to a door that
reads that shelf: compiles, runs, and is silently ignored. The
classic case was a per-app extra handed to a list screen that reads
no recipient at all; the mirror case was a `package:` data URI on the
exact-alarm door, which takes no input.

**Folklore address** — a `ComponentName` pointing at a vendor's
private screen (the security center's autostart manager), learned by
community hearsay rather than an official constant. Constructing it
never checks the phone — it's an address written on an envelope; only
the phone's answer at delivery (or its exception) tells whether the
door exists.

## Walking a user through a gate

**Gate** — a phone-side switch that blocks background behavior and
that the app cannot flip itself. Four family members so far:
notifications (Android), battery-optimization exemption (Android),
exact alarms (Android), autostart (vendor-private).

**The three verbs** — what an app may do about a gate, limited by
who owns it: **ask** (request a dialog or permission), **mirror**
(read and display the current state), **walk** (deep-link the user to
the switch). Autostart allows only attempt/walk — no read, no
request; write belongs to the vendor.

**Mirror** — a live-checked settings row that displays a fact owned
elsewhere (the grant ledger, the PowerManager, the AlarmManager),
recomputed at composition, stored nowhere. Storing a local copy of a
phone-owned fact is a mirror one layer above the truth.

**Reader with a map** — a mirror row: it reports the state it
cannot change, and its only action is the map (`deep link`) to the
owner's switch. Never build a toggle row for a state the app can't
write.

**Deep link** — an intent to a system settings page, usually the
user's own app's page (`ACTION_APP_NOTIFICATION_SETTINGS` +
`EXTRA_APP_PACKAGE`). Opening one's *own* settings page is always
legal and needs no permission.

**The exhaustion rule** — an exhaust-recovery surface must act with
tools that cannot exhaust. The system permission dialog stops
appearing after two denials, precisely in the state the mirror row
exists for — so the row deep-links to a switch, never re-plays the
dialog.

**Always-visible row** — a mirror's honest substitute where no read
API exists (autostart): a static row that states the gate and its
way through, rather than a fluent row that would lie.

**Attempt → caught fallback** — the autostart row's design: try
`startActivity` on the explicit component; when the phone throws
(no such door, or access denied), render instructional copy. The
fallback triggers on the *failure of the attempt* — never on a
pre-click null-check of a property you set yourself.

**Shape-changing row** — the fallback rendered *in place*: the row
swaps its button for instructional text when the attempt fails. Copy
format follows its job: confirmations fit transient bubbles
(toasts), instruction sets need persisting surfaces.

**Pre-paid permission** — a manifest `<uses-permission>` whose cost
was paid at declarations time (a "normal" install-time grant or a
future-proof declaration), so later code assumes nothing: whether it
answers "granted" live is the owner's affair — `checkSelfPermission`
on a "normal" permission can ride a fact that never changes, which is
why the battery row's first draft mirrored the wrong layer.

## Failure families (bug species, named to be recognized)

**Compute-and-never-attach** — building a value and never delivering
it: a ringtone computed and never set on the channel; a
`Toast.makeText(...)` without `.show()`; an envelope built but not
bound. Factory results with no consumer need their finalizer or
their attachment call.

**Wrong door** — right-looking intent, wrong action constant: a
letter the door never reads is indistinguishable from no letter.
Silence (nothing opens, or a useless page opens) is the symptom,
never a crash.

**Method-lookup stop** (`NoSuchMethodError`) — calling a method the
device's OS version doesn't have: the runtime looks the name up,
finds nothing, stops without mercy. Detonates only on the older
phone, exactly when nobody is watching.

**A promise is not a check** — `@RequiresApi(...)` speaks to the
compile-time checker and inserts no runtime decision; a
`Build.VERSION.SDK_INT >=` test decides at runtime. Silencing a
squiggle with a promise is not protecting an older phone — only the
guard is. Order matters inside `&&`: short-circuit evaluation lets
the SDK check fire first, so the dangerous call is never evaluated
there.

**Declaration-time crash** — code running when a class is declared
but before its context exists (`MainActivity()` /
`ThymeApplication()` husks; property initializers run before Android
attaches the app and injects collaborators). Logic that needs
context belongs in `onCreate` and friends, not initializers.

**State lives where the ownership lives** — one of three species:
phone-owned facts (permissions, switches → mirror live, never store);
app-owned session memory (rows, drafts → remembered state, survives
recomposition, not navigation); and persistent truth (DataStore —
deliberately unused so far). Migrating a fact between owners is a
design change, not a refactor.

## App-specific nouns

**Single writer** — only `InMemoryTimerRepository` mutates the
session. Receivers are one-line thunks into it; the foreground
service only observes and renders; screens only start it.

**Thunk receiver** — a `BroadcastReceiver` so small it does one
delegation call under the entry gate; also stateless (a fresh object
per delivery — device-proven), and never treated as a `Context` (the
context arrives as the `onReceive` parameter).

**Catch-up** — the overdue policy: trust stored timestamps, consume
every missed phase in order until a future phase-end, ring once per
burst, and run from *two* hook points: the alarm receiver and app
re-entry.

**Ghost channels** — notification channels from retired id strings,
still sitting in the phone's settings because channels are
never silently rewritten. House rules: **id-bump rule** (changing a
channel's settings means a new id, leaving the old one behind as a
ghost) and the promised sweep (`deleteNotificationChannel` for the
retirees).

## Device facts that shaped design

**HyperOS / the vendor gate family** — the phone (POCO, HyperOS 3)
adds private per-app switches under Android's own permissions.
Buried screens are findable by folklore addresses or by
`adb shell am start -a android.settings.REQUEST_SCHEDULE_EXACT_ALARM`.

**Receipt gallery** — shell commands that print ground truth:
alarms currently armed → `adb shell dumpsys alarm | grep -i thyme`
(exact arm = `window=0 exactAllowReason=permission`; fallback arm =
`window=+Ns`); exemption state → `dumpsys deviceidle whitelist`;
permission grant ledger → `cmd appops get <pkg> | grep -i exact`.
Logcat rotates: a proof may already be gone — rerun the failure.

# Virtual Racing Wheel — Project Status

Single consolidated map of the project. The project spans five subprojects and grew across many
separate READMEs with no unified view — this file is that view.

## Subprojects

| Subproject | Path | What it is |
|---|---|---|
| Android app | `android/` | Turns a phone into the wheel/pedal controller, plus Controller Builder, telemetry dashboard, settings, pairing, and community-feed import. |
| Windows companion | `windows-companion/` | Receives control input over UDP, drives a virtual Xbox controller (ViGEmBus) + dedicated clutch axis (vJoy), relays game telemetry back to the phone. WPF GUI + `--headless` console mode share one engine. |
| Game telemetry plugins | `GamePlugins/` | Per-game shared-memory readers that feed the Windows companion's telemetry relay. Currently: Assetto Corsa (unverified — see its README). |
| Community stopgap | `community/` | Minimal static-JSON-feed layout sharing. Explicitly not a real backend — see its README. |
| CI | `.github/workflows/` | Browser-buildable Android (Gradle, debug APK artifact) and Windows Companion (dotnet build+test) pipelines. |

## Phase-by-phase status

| Phase | Area | Status | Notes |
|---|---|---|---|
| 1 | Android core (steering, pedals, UI) | Solid | Full unit test coverage on the math (steering pipeline, pedal normalization). |
| 2 | Windows companion core + pairing | Solid | LAN discovery, PIN-confirmed whitelisting, persistent trust, console-based revocation. |
| 3 | Controller Builder | Solid | Drag/resize/add/delete, multiple saved layouts, overlap prevention (revert-on-release), cascading non-overlap placement for new elements, camera-control stick, export/import. |
| 4 | Telemetry pipeline | Partial | Schema, relay (45127→45128), Android dashboard, `--simulate-telemetry` mode all built. Real game data source: only Assetto Corsa, and that's unverified — see `GamePlugins/AssettoCorsa/README.md` for exactly which fields are trustworthy. |
| 5 | Windows GUI | Solid | Code-only WPF: PIN display, live stats, trusted-device list w/ revoke, activity log, tray icon, hand-rolled latency/packet-loss trend graphs. `CompanionEngine` shared identically between GUI and `--headless` mode. |
| 6 | Community sharing | Minimal | Read-only static JSON feed import only. No upload/accounts/ratings/moderation — a deliberate stopgap, not a finished feature. |
| 7 | Testing | Solid | Android: JVM unit tests for steering math, pedal processing, layout mirroring, overlap engine. Windows: xUnit tests for `CompanionEngine` pairing/trust/revocation and axis-conversion math. |
| 8 | Multi-device | Basic | Phones pick a role (controller vs. telemetry display) after pairing. Telemetry relay fans out to every trusted device's IP. The original single-target bottleneck was narrowly the telemetry relay, not pairing (which already supported multiple devices). |
| 9 | Settings polish | Solid | Units toggle (km/h ↔ mph), left-handed mode (mirrors any layout at render time without rebuilding it). Mirroring math unit tested. |

## Biggest remaining functional gap

**Real game telemetry data is thin.** Only one game (Assetto Corsa) has a plugin at all, and it's
unverified against a live session — max RPM is hardcoded, fuel is liters not a percentage, lap
time isn't read, and clutch position is unread. Adding/validating more game plugins (or hardening
the AC one) is the highest-value next step for making the telemetry feature actually useful.

## Other known open items

- Community sharing (Phase 6) needs real backend infrastructure decisions before it can move past
  the static-feed stopgap.
- No PIN/whitelist auth existed at the very start of the project — since fixed (Phase 2) — but
  worth remembering the pairing flow was hardened incrementally, not designed all at once.

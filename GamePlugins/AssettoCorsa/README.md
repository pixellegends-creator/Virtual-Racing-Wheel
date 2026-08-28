# Assetto Corsa Telemetry Plugin

Reads AC's `acpmf_physics` shared-memory block and converts it into the companion's generic
`TelemetryFrame` for the relay (port 45127 → 45128).

**Status: unverified against a real game session.** No AC install was available during
development, so this has only been checked against the publicly documented shared-memory layout,
not tested live. Treat it as a first draft, not a finished integration.

## Field reliability

| Field | Status | Notes |
|---|---|---|
| `SpeedKmh` | Solid | Direct read of `SpeedKmh` from the physics page. |
| `Rpm` | Solid | Direct read of `Rpms`. |
| `Gear` | Solid (probably) | AC encodes 0=reverse, 1=neutral, 2=first, etc. — converted to a signed gear number (`-1` = reverse, `0` = neutral). Worth double-checking against a live session. |
| `MaxRpm` | **Wrong until fixed** | Hardcoded to `8000f`. AC doesn't expose this in the physics page — it's in the static page (`acpmf_static`), which isn't wired up yet. Any car with a different redline will show an inaccurate RPM gauge scale. |
| `FuelLiters` | Approximate — units caveat | AC reports fuel already **in liters**, not a percentage. Don't treat this as 0–100%; a UI showing a "fuel %" bar needs the car's tank capacity (also from the static page, not yet read) to compute a real percentage. |
| `LapTimeSeconds` | Not implemented | Left at `0f`. Lap time lives in `acpmf_graphics`, which this plugin doesn't read yet. |
| Tire temps | Solid (probably) | Direct reads of the four `TyreCoreTemperature*` fields. |
| Clutch | **Unread** | `physics.Clutch` exists in the struct mapping but is never forwarded into the `TelemetryFrame`. Known gap — clutch position telemetry (as opposed to clutch pedal *input*, which is separate and already handled by the vJoy path) isn't surfaced yet. |

## Known gaps / next steps

- Wire up `acpmf_static` for real max RPM and fuel tank capacity.
- Wire up `acpmf_graphics` for lap time, sector times, position.
- Forward `Clutch` from the physics page.
- Validate struct field offsets against a live AC session — the layout here follows AC's public
  SDK documentation but has not been confirmed against actual running memory.

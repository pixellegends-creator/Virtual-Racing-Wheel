# Community Layouts (Phase 6) — Stopgap, Not a Real Backend

This is a deliberately minimal stand-in for Phase 6 (community layout sharing), built to give
every phase of the original spec at least a working starting point.

## What it actually does

The Android app lets a user enter a static JSON feed URL (e.g. a `layouts.json` file hosted free
on GitHub, GitHub Gist, or any static file host). It fetches that URL, parses it as an array of
exported layout objects (the same JSON format produced by the Controller Builder's own
export feature), and lets the user pick which ones to import into their local Controller Builder.

## What it explicitly does NOT do

- **No upload UI.** Users can't publish a layout from the app — someone has to manually add it to
  the JSON feed file themselves.
- **No accounts.** No login, no identity, no attribution beyond whatever the feed file itself
  contains.
- **No ratings, comments, or discovery.** It's a flat list, whatever order the feed file has them in.
- **No moderation.** Anyone who controls the feed URL controls what shows up. There's no
  reporting/flagging mechanism.

## Why

Real community sharing needs backend infrastructure decisions (hosting, auth, storage, abuse
handling) that were explicitly out of scope for this pass. This stopgap exists so the feature
*works end-to-end* — fetch a feed, import a layout — without committing to any of those decisions
yet. Treat this as the seam where a real backend would plug in later, not as the finished feature.

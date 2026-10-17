# Product evidence and claim control

## Choose the input mode

Use **source-aware mode** when source, tests, docs, or a build project are available. Use
**binary-only mode** when only an installed app or build exists; require a feature manifest or
repeatable navigation flow for any claim that cannot be confirmed visually.

Never modify the app merely to make a promo claim true. A video output directory may contain demo
fixtures, constructed explanatory HTML overlays, capture manifests, and sanitized copies of
screenshots. Constructed HTML may annotate native capture; it is never sufficient runtime evidence
for a product-proof claim.

## Build the evidence matrix

Give each claim a stable ID and record:

| Field | Meaning |
|---|---|
| `id` | Stable identifier such as `READ-03` |
| `claim` | Exact audience-facing promise |
| `user_value` | Why it matters to the target audience |
| `source_evidence` | File and line, test, model, route, or documentation reference |
| `runtime_evidence` | Simulator/emulator/physical capture path under `captures/` |
| `status` | `verified`, `demo`, or `unverified` |
| `risk` | Privacy, network, generated data, account, or licensing caveat |
| `shot_candidate` | Whether and how it can become a visual sequence |

A claim may become `verified` for the final film only when both source evidence and a native
runtime capture exist. Block `unverified` claims from the final script. Use `demo` only for clearly
labeled overlays that sit on top of native capture (for example a post-submit highlight), never as
the sole product UI. If capture access is missing, hard-pause and ask the user to authorize a
device or supply a simulator/emulator image.

## Understand before selecting

Inspect feature entry points, user flows, domain models, state transitions, tests, copy, failure
states, design tokens, and existing screenshots. Distinguish:

- implemented behavior from roadmap language;
- a deterministic local fallback from a successful remote AI request;
- a planned sync contract from an observed sync result;
- a simulator/emulator capture from physical-device footage;
- an explanatory reconstruction from a captured interactive state.

Select features by audience value, evidence strength, visual potential, distinctness, and narrative
connectivity. Do not reward a feature merely because it is easy to screenshot.

## Protect data

Before capture, replace or hide names, email addresses, account IDs, tokens, cookies, documents,
private learning records, internal URLs, live analytics, and customer data. Prefer stable fictional
fixtures. Do not publish a project path, device identifier, signing identity, API key, or private
repository metadata in the case study.

Write `capture-manifest.json` with each asset's origin, platform, device class, locale, orientation,
status-bar treatment, contains-native-chrome flags, evidence IDs, and sanitization notes.

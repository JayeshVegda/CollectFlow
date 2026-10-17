---
name: native-app-promo
description: Turn a native iOS or Android app into an evidence-backed cinematic product promo that requires real simulator/emulator or authorized physical-device capture, brand-specific motion direction, HTML/React motion layered on native screenshots, Remotion, sound design, and deterministic media QA. Use when Codex needs to understand an app's real capabilities, autonomously choose the product story, define an emotional motion identity, choreograph a commercial-quality app film, or audit a native-app promo for fabricated claims, missing native capture, generic or weak motion, device-frame errors, frozen scenes, visual jitter, or audio/render defects.
---

# Native App Promo

Create a product film from the app itself. Act as product strategist, director, motion designer,
capture operator, Remotion engineer, sound designer, and QA owner. Let the app determine the
story and visual language; do not force every product through one template.

## Establish the contract

Treat the supplied app project, binary, documentation, capture flows, and user constraints as the
source of truth. Interpret, group, prioritize, and name existing capabilities for marketing, but
never invent a capability, success state, account state, sync result, AI result, or physical-device
capture.

**Native capture is mandatory for delivery.** The finished film must prove the product with real
simulator, emulator, or authorized physical-device screenshots/recordings. HTML/React layers may
explain, enlarge, or transition around that native evidence; they must never replace it as the only
product UI.

**Hard pause when capture is unavailable.** If no booted simulator/emulator exists and no physical
device is authorized for capture, stop before Remotion scene construction and final render. Ask the
user in their language for one of:

1. authorize one connected physical device for promo capture; or
2. install/boot a simulator or emulator image the agent can use.

Do not ship a “completed” promo whose product UI is only a reconstructed explanatory layer. Do not
claim completion while that gate is open. Planning docs and discovery may continue; picture lock,
commercial render, and delivery may not.

Use autonomous direction when the user delegates creative control. Continue through discovery,
planning, capture, implementation, audio, render, and QA without requesting approval for each
intermediate artifact. Pause when truth, privacy, credentials, third-party rights, destructive
changes, missing native-capture access, or a materially different product direction requires a user
decision.

Keep the product source tree read-only unless the user explicitly asks to change the app. Put all
video work in a separate output directory. Preserve unrelated working-tree changes.

## Run the workflow

1. **Discover the product.** Read `references/evidence.md`. Inspect source, docs, tests, routes,
   models, design tokens, and runnable states. Attempt to launch the simulator or emulator. Produce
   `product-brief.md` and `evidence-matrix.md` before writing promotional claims.
2. **Direct the film.** Read `references/story-direction.md`. Select a single audience, promise,
   narrative spine, feature set, native-product surface, and content-driven runtime. Produce
   `decision-matrix.md`, `design-spec.md`, `storyboard.md`, and `shot-map.md`.
3. **Direct the motion.** Read `references/motion-direction.md`. Derive an emotional target and one
   brand motion identity from the app. Define frame-based duration, easing, path, choreography,
   layer roles, rest geometry, and settle ownership before implementing scenes. Produce
   `motion-direction.md` and complete the motion fields in `shot-map.md`.
4. **Capture evidence (blocking).** Read the platform section of `references/native-capture.md`.
   Freeze demo data, locale, orientation, status bar, and device configuration. Capture
   deterministic native states for every shot that proves a product claim. Record whether every
   asset came from a simulator, emulator, or authorized physical device. If capture cannot start,
   hard-pause and ask the user to authorize a device or supply a simulator/emulator image; do not
   proceed to picture lock with constructed UI only.
5. **Build the film.** Read `references/remotion-production.md`. Copy `assets/remotion-starter/`
   into the output project or integrate into an existing isolated Remotion project. Use captured
   native screenshots or recordings as the product-UI credibility anchors. Use frame-driven
   HTML/React motion for callouts, relationships, and transitions around those anchors. If
   `$video-shotcraft` is available and the user asks to use it, obey its mode contract: use
   autonomous free creation when creative control is delegated, guided co-creation when the user
   wants checkpoints, or its template route only when the template is explicitly selected. Use its
   validated shot recipes and exact referenced demo implementations as the motion vocabulary. Keep
   this skill's native evidence, capture, privacy, device-surface, settled-frame stability, and
   final delivery rules authoritative.
6. **Lock picture and design sound.** Read `references/audio.md`. Add BGM and SFX only after visual
   timing is coherent. Pin every cue to scene-relative frames. Keep an asset-level license ledger.
7. **Render and iterate.** Read `references/qa.md`. Render stills during implementation, then the
   complete film. Inspect decoded frames, scene boundaries, completion holds, native-device areas,
   audio, and media metadata. Fix failures and rerender until the gates pass.
8. **Deliver evidence.** Read `references/deliverables.md`. Deliver BGM and no-BGM/SFX-only MP4s,
   source, capture manifest, story artifacts, contact sheet, license ledger, and final QA report.

## Enforce product-film rules

- Keep one platform and one coherent product surface unless the user explicitly requests a
  cross-platform story. Never alternate between web and native UI by accident.
- Every product-proof shot must include real native capture. Do not turn the film into a long screen
  recording, and do not rebuild the whole app UI from memory as a substitute for capture. Rebuild
  explanatory callouts and transitions as refined HTML layers around the native evidence.
- Derive typography, colors, materials, spacing, corner language, and motion character from the
  app. Reuse shot structure, not another product's skin.
- Give every shot one emotional target, one hero, one meaning-bearing primary action, and exact
  frame ranges for setup, action, reaction, resolution, readable hold, and handoff. Secondary and
  ambient motion are optional support layers, not decoration quotas.
- Use the minimum properties needed. Motion direction, path, easing, and timing must explain
  hierarchy, causality, progress, or state; remove movement that has no semantic job.
- Give each feature an establish, explain, settle, and handoff phase. Reserve about 2–3 seconds of
  readable completion state for dense feature explanations while retaining only local micro-motion.
- Stop parent transforms when information settles. Never leave a full phone, editor, card group, or
  canvas on an endless sine-wave translation or scale loop.
- Keep meaningful movement active across the film; do not let any fully static composition exceed
  5 seconds. Do not use paper grain or ambient drift as a substitute for explanatory motion.
- Use shared elements for semantic handoffs. At a cut, preserve the outgoing element's geometry
  until the incoming scene owns it; remove non-shared remnants before the boundary.
- Drive all visible motion from Remotion frame time. Forbid `Date.now()`, unseeded `Math.random()`,
  free-running CSS animations, and render-order-dependent randomness.
- Detect whether a screenshot already contains status bars, notches, or a Dynamic Island before
  drawing a synthetic device surface. Never double-render device chrome.
- Label constructed overlays such as post-submit interaction demonstrations. Do not present them as
  a captured live product state, and do not use them as the only proof of a feature.
- Derive runtime from meaningful information. Remove filler instead of stretching to a target, and
  extend the film when a real feature needs time to become understandable.

## Completion gate

Do not claim completion until all of the following are true:

- Every product-proof shot has a corresponding simulator, emulator, or authorized physical-device
  asset recorded in `captures/capture-manifest.json` and present under
  `captures/sanitized-native-assets/`.
- No final product-UI surface is only a reconstructed HTML mock of the app.
- Every visible functional claim maps to verified source evidence plus runtime native capture, or is
  clearly labeled as a non-capture demonstration overlay that does not replace the native proof.
- Native captures use scrubbed data and a consistent device surface.
- Each scene has been reviewed at entry, peak action, settled state, and exit.
- The motion identity and shot-level direction cards are complete, brand-specific, and reflected in
  the rendered film rather than added as post-hoc documentation.
- Settled parent groups remain visually fixed; local micro-motion does not move the reading frame.
- No black or blank frame, accidental hard cut, duplicate device chrome, or more than 5 seconds of
  full stillness remains.
- Repeated still renders of the same frame are byte-identical.
- Final output has the requested resolution and complete CFR frame count; audio is 48 kHz stereo
  when present, has no clipping, and is the same duration as the video.
- The BGM and no-BGM deliverables share the same visual stream.
- `final-qa.md` records commands, measured results, capture origin, known limitations, and exact
  delivered files.

Use `references/case-lessons.md` when diagnosing motion stability, device-chrome problems, feature
timing, or native-capture credibility. It contains generalized lessons from the Moxi English film,
not a reusable visual template.

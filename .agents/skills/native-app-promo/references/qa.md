# Final QA

## Review the story

Compare the rendered film with `product-brief.md`, `evidence-matrix.md`, `design-spec.md`,
`motion-direction.md`, `storyboard.md`, `shot-map.md`, and `captures/capture-manifest.json`.
Confirm that every promised feature appears clearly, each claim matches source plus native capture,
demo overlays are labeled, no product-proof shot is HTML-only, and the film stays on the requested
product surface. Fail QA if native assets are missing.

Review motion at normal speed before examining individual frames. Confirm that each shot has one
clear hero, its action matches the declared emotional and semantic purpose, the selected direction
and path make sense, timing belongs to the product's motion identity, and secondary or ambient
layers never compete with the explanation. Reject motion that is only decorative, generic, or
readable only in slow motion.

## Review frames

Render stills at each scene's entry, peak action, settle, and exit. Decode boundary frames around
every cut. Check:

- no black, blank, duplicate, or one-frame flash;
- no accidental web/native or device-family switch;
- no duplicated status bar, notch, or Dynamic Island;
- no cropped source chrome or distorted screen aspect;
- no parent movement after the settle frame;
- no unfinished mask, clipped word, stale bridge remnant, or disappearing shared element;
- no fully static composition longer than 5 seconds;
- sufficient 2–3 second reading window after dense information completes.
- no linear spatial tween, unresolved spring, attention-stealing ambient loop, or unrelated motion
  from mixed screen origins;
- no parent-level breathing, floating, parallax, or scale pulse during a declared reading hold.

Use phase correlation or a stable ROI comparison for reported jitter. Target less than 0.1 px of
apparent parent movement during completion holds. Target no more than 2 px of shared-element center
delta at a cut.

## Review determinism and media

Render the same representative still twice and compare SHA-256 hashes. Search code for
`Date.now`, unseeded `Math.random`, CSS `animation`, and `@keyframes`.

Run:

```bash
python3 scripts/qa-video.py final.mp4 --json qa-report.json
```

When system FFmpeg is unavailable, set `NATIVE_APP_PROMO_FFMPEG`,
`NATIVE_APP_PROMO_FFPROBE`, and `NATIVE_APP_PROMO_FFMPEG_LIBDIR` to Remotion's bundled compositor
binary and library paths before running the script.

Verify width, height, CFR frame rate, exact frame count, duration, pixel format, range, color matrix,
audio sample rate, channels, peak, black frames, and longest near-still run. Review a contact sheet
and the full film at normal playback and 100% scale.

Confirm the BGM and no-BGM outputs have the same frame count and identical or intentionally
equivalent visual elementary streams. Record exact SHA-256 hashes and known metadata limitations.

Do not treat a successful TypeScript check, Remotion render, or ffprobe result as visual acceptance.
All three are necessary; none is sufficient alone.

# Output contract

Keep all production work outside the native app source tree. Use this structure:

```text
promo-output/
├── docs/
│   ├── product-brief.md
│   ├── evidence-matrix.md
│   ├── decision-matrix.md
│   ├── design-spec.md
│   ├── motion-direction.md
│   ├── storyboard.md
│   ├── shot-map.md
│   ├── audio-attribution.md
│   └── final-qa.md
├── captures/
│   ├── capture-manifest.json
│   └── sanitized-native-assets/
├── remotion/
├── out/
│   ├── preview/
│   ├── qa/
│   └── final/
│       ├── product-promo-bgm.mp4
│       └── product-promo-no-bgm.mp4
└── README.md
```

The final handoff must state separately:

- what product evidence was inspected;
- what emotional target, motion identity, duration palette, easing, and settle rule governed the
  film;
- whether capture came from a simulator, emulator, or authorized physical device, with asset paths;
- which overlays were reconstructed and how they were labeled (overlays only; never sole product UI);
- what was rendered and its exact path;
- which code, media, and visual checks passed;
- which limitations or licensing conditions remain.

Reject delivery if `captures/sanitized-native-assets/` is empty or if product-proof shots lack native
assets in `capture-manifest.json`.

Do not claim a physical-device test, public deployment, Git push, or audio right that was not
actually completed.

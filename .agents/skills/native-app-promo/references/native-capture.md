# Native capture

## Capture gate (blocking)

Native capture is required before picture lock and final delivery. Acceptable origins:

- iOS Simulator
- Android emulator
- physical device only when the user supplies footage or explicitly authorizes capture

If none of the above is available:

1. stop before building product-UI scenes that pretend to be the app;
2. tell the user that the promo cannot be completed without native capture;
3. ask them to authorize one connected device or install/boot a simulator/emulator image;
4. after access exists, capture real states and use those assets as the product-UI anchors.

Do not finish a promo whose only product UI is constructed HTML. Constructed layers may annotate or
bridge native captures; they may not replace them.

## General procedure

Build and launch the app without editing product behavior. Fix locale, orientation, device class,
appearance, clock, connectivity, and demo data before capture. Navigate to repeatable states. Capture
the smallest set of states that proves the story; avoid long, aimless recordings.

Use `scripts/capture-ios.sh` or `scripts/capture-android.sh` for deterministic screenshots. Import
physical-device recordings only when the user supplies or authorizes them.

Every shot that proves a product claim must list at least one native asset path in
`captures/capture-manifest.json`.

## iOS Simulator

Check booted devices with `xcrun simctl list devices booted`. Capture a PNG with:

```bash
scripts/capture-ios.sh output/state-name.png booted
```

Use `xcrun simctl status_bar <device> override` when a stable status bar is appropriate, and clear
the override after capture. Use `xcrun simctl io <device> recordVideo` for a short interaction that
cannot be represented by screenshots and frame-driven reconstruction.

State clearly that Simulator footage is simulator capture, not physical iPhone footage.

## Android emulator or device

Confirm exactly one intended target with `adb devices`. Capture with:

```bash
scripts/capture-android.sh output/state-name.png <serial>
```

Use `adb shell screenrecord` only for short interactions. Android screenrecord may omit product
audio; design and mix promo audio separately.

Physical Android devices: do not capture until the user authorizes that serial for promo work.
Prefer a dedicated emulator when privacy scrubbing is uncertain.

## Device-surface contract

Record these fields for every asset:

```json
{
  "origin": "simulator | emulator | physical-device",
  "screenshotContainsStatusBar": true,
  "screenshotContainsNotchOrIsland": true,
  "syntheticDeviceFrame": true,
  "syntheticStatusBar": false,
  "syntheticNotchOrIsland": false,
  "screenObjectFit": "cover",
  "screenObjectPosition": "50% 0%"
}
```

Never infer a generic screen aspect ratio when the source dimensions are known. Preserve the native
top safe area when the screenshot contains system chrome. If using `cover`, calculate and review the
crop; do not accidentally trim the status bar or Dynamic Island.

Use one device family and one platform throughout the film unless the story explicitly proves a
cross-device capability. Do not show browser chrome, desktop windows, emulator shells, or mouse
cursors as part of an otherwise native product surface.

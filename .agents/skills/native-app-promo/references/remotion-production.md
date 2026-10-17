# HTML and Remotion production

## Build a deterministic scene system

Drive all motion with `useCurrentFrame()` and `useVideoConfig()`. Use `interpolate()`, bounded
springs with `overshootClamping`, explicit easing, and seeded randomness. Clamp entrance and camera
motion at the settle frame.

Implement the tokens approved in `motion-direction.md`; do not rediscover easing and duration ad
hoc inside every component. Convert time-based references into integer frames at the composition
FPS, and keep named quick, standard, and slow durations consistent across scenes. Give each visible
transform one semantic owner so parent and child layers do not unknowingly animate the same axis.

Represent each scene with four phases:

1. `enter`: establish the visual subject;
2. `explain`: reveal evidence in an understandable sequence;
3. `settle`: stop parent transforms and preserve a readable completion state;
4. `handoff`: move one semantic anchor into the next scene.

Setup, action, reaction, resolution, readable hold, and handoff may overlap, but record their exact
frame ranges. The short motion resolution and the longer 2–3 second reading hold are separate
phases. A spring must reach its analytical rest before the reading hold begins.

Keep a timeline registry with stable scene names, `from`, `duration`, settle frame, and transition
ownership. Pin audio cues relative to scene starts.

## Use real UI and constructed HTML correctly

Every product-proof scene must embed a real native screenshot or recording from
`captures/sanitized-native-assets/`. Use HTML/React layers only to enlarge small information,
explain relationships, visualize invisible state, or execute transitions around that native anchor.
Match the source UI's typography, spacing, colors, corner language, and density. Label a constructed
overlay when it is not present in the captured screenshot.

Do not rebuild the app UI from memory and present it as a native screenshot. Do not ship a film whose
phone screen is only a hand-drawn or HTML reconstruction. Do not leave a single screenshot fixed
while decorative particles move around it. If native assets are missing, stop and request device or
simulator/emulator access instead of inventing UI.

## Prevent motion jitter and upward pulls

- Freeze the scene's parent transform at the latest information-completion frame.
- Replace endless `sin(frame / n)` translation or scale on major groups with a one-time pulse that
  returns to the exact rest transform.
- Keep post-settle motion inside local elements such as a waveform, cursor, progress glint, or tiny
  highlight; do not move the reading frame.
- Avoid simultaneous fractional transforms on a phone parent and its screen child. Compose camera
  motion in one `translate3d`/scale transform when possible.
- Keep background texture phase continuous across unmasked cuts, or cover a reset with a deliberate
  transition. A per-sequence paper drift reset can look like the whole frame jumping.
- Clamp extended shots. Never let an interpolation extrapolate beyond its originally designed end.
- Inspect decoded video at 100% scale before blaming the codec. Compare PNG or ProRes samples with
  H.264 output when a player appears to jump.

## Build shared-element handoffs

At the outgoing scene's final frame, keep only the shared element and intentional background. Match
its box, radius, border, text baseline, scale, and rotation in the next scene's first frame. Target
no more than 2 px of center displacement. Fade all non-shared remnants to zero before the cut.

When entering a bridge, inherit the outgoing completion component or reuse the same geometry
constants. Do not redraw an approximately similar element on a different background and call it a
shared transition.

## Render profiles

Use a fast 1x preview profile while iterating. For commercial output, prefer PNG intermediates,
2x supersampling when practical, high-quality downsampling, H.264 CRF 16 with a slow preset,
`yuv420p`, TV range, and BT.709 matrix metadata. Verify the actual encoded result rather than
assuming command flags were retained.

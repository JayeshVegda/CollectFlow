# Motion direction gate

Use this reference after the product story is selected and before scene implementation. It adapts
the philosophy-first approach of LottieFiles' open-source
[`motion-design-skill`](https://github.com/LottieFiles/motion-design-skill) to deterministic,
frame-driven product films. The upstream project is MIT-licensed; see the repository's
`THIRD_PARTY_NOTICES.md`.

The goal is not to add more movement. The goal is to make every movement communicate product
meaning, guide attention, and resolve into a stable reading frame.

## Define one brand motion identity

Derive the identity from the app and audience. Record it in `motion-direction.md` before building
scenes:

| Token | Decision |
|---|---|
| target feeling | What the audience should feel: trust, calm, confidence, delight, urgency, or another explicit emotion |
| personality | One dominant character, such as restrained editorial, playful, professional, premium, or energetic |
| signature easing | The curve or bounded spring used by most entrances and on-screen moves |
| duration palette | Quick, standard, and slow durations expressed in frames at the composition FPS |
| entrance grammar | The recurring spatial rule for new information |
| path language | Straight, arced, radial, converging, or another product-relevant geometry |
| overshoot budget | Maximum overshoot for normal, success, and exceptional moments |
| settle rule | Exact rule for when parent transforms stop and the reading frame becomes fixed |

Use the dominant personality for roughly 90% of the film. A special moment may borrow another
character, but the shift must have narrative meaning. Do not default every education product to
playful motion or every premium product to slow fades.

## Pass every shot through the direction card

Add these fields to every shot in `shot-map.md`:

| Field | Question |
|---|---|
| purpose | Is the motion revealing, transitioning, emphasizing, responding, explaining a relationship, or creating atmosphere? |
| emotion | What should the viewer feel at this beat? |
| hero | Which single element owns attention? |
| primary action | What one action verb describes the main motion? |
| primary property | Which property carries the meaning: position, scale, rotation, mask, color, or another explicit property? |
| secondary reaction | What reacts after or against the hero, and why? |
| ambient role | What low-energy motion preserves life without changing the reading frame? Use `none` when it adds no value. |
| path | What direction communicates the intended relationship or progress? |
| timing | Exact frame ranges for setup, action, reaction, resolution, readable hold, and handoff |
| easing | Entrance, on-screen, and exit curves or bounded spring parameters |
| rest geometry | Final parent transform and the frame at which it becomes immutable |

If a field has no semantic answer, remove the motion instead of decorating it.

## Choreograph attention, not simultaneous activity

- Give each beat one hero. Supporting motion must be smaller, quieter, later, or slower.
- Use a shared spatial origin. Elements caused by the same event should appear to come from that
  event rather than unrelated screen edges.
- Stagger in reading order or along the product's data flow. The cascade should clarify a sequence,
  not delay comprehension.
- Use counter-motion only when it strengthens depth or causality: a lifted card may soften its
  shadow; a rightward evidence flow may move a distant layer slightly left.
- Break long travel with a change of speed, path, scale, or ownership. An object should not cross
  more than roughly one third of the canvas as one uninterrupted tween.
- Do not animate every visible element. When many elements are present, keep most settled while the
  current relationship is explained.

Treat setup → action → reaction → resolution as a flexible micro-story. Simple transitions may
skip setup or reaction. Dense product explanations must never skip resolution and the readable
hold.

## Choose properties by meaning

- Position explains direction, transfer, order, and relationship.
- Scale explains importance, activation, proximity, or depth.
- Rotation explains reorientation or processing; avoid it when no state is changing.
- Masks and clips explain reveal, scanning, progress, or containment.
- Color explains state or identity only when the app already gives that color semantic meaning.
- Opacity supports another property for important changes; opacity alone is acceptable only for
  quiet backgrounding or a deliberately restrained dissolve.

Prefer one meaning-bearing property plus one supporting property. Three or more animated
properties require a clear reason. Prefer transforms and opacity over layout-changing properties.

## Translate timing into deterministic frames

Treat interaction timing tables as starting points, not film-wide laws. At 30 fps, useful initial
ranges are:

| Event | Starting range |
|---|---|
| tiny feedback or indicator | 3–6 frames |
| icon or label state change | 5–9 frames |
| card or evidence unit | 7–14 frames |
| panel or device-level transition | 12–24 frames |
| cinematic reveal | 18–36 frames |

Scale duration with distance, visual weight, information density, and target emotion. Entrances
normally decelerate; exits normally accelerate; on-screen travel normally eases at both ends.
Spatial movement must not use linear easing unless the product explicitly represents uniform
mechanical progress. A spring must be clamped or analytically settled before the reading hold.

The scene still follows the product-film timing contract: after dense information is complete,
reserve about 2–3 seconds for reading. This hold is separate from the short motion resolution.

## Separate three levels of movement

Use the upstream primary / secondary / ambient model as an attention hierarchy, not a quota:

- **Primary:** the meaning-bearing action the viewer follows;
- **Secondary:** a delayed or counter reaction that explains weight, cause, or relationship;
- **Ambient:** low-energy background life that never moves the reading frame.

Every shot needs a primary action. Secondary and ambient layers are optional when restraint is the
right direction. Ambient texture, grain, particles, waves, or glints cannot compensate for a weak
primary action.

At the settle frame:

- freeze the phone, editor, major card, camera, and canvas parent at exact rest geometry;
- end all parent-level breathing, floating, parallax, and scale pulses;
- allow only local semantic motion such as a waveform, cursor, progress glint, or tiny status pulse;
- ensure the local motion cannot be misread as the entire composition drifting or jumping.

## Direct transitions as semantic handoffs

The outgoing subject should become the incoming subject whenever the product relationship supports
it. Preserve its geometry across the boundary and transfer ownership only after the new scene can
continue the motion. Match position, size, radius, border, baseline, scale, and rotation.

Use entrance easing for the incoming owner and exit easing for non-shared remnants. Remove all
non-shared remnants before the cut. A full-screen veil is appropriate only when the story calls for
a chapter break; it is not a substitute for a missing relationship.

## Motion review gate

Before picture lock, review the film at normal speed and answer:

- Does each shot have one clear hero and one readable action?
- Does the motion personality belong to this product rather than a generic archetype?
- Do direction, path, timing, and easing communicate the intended relationship?
- Is the action legible without slow motion?
- Do secondary reactions support the hero rather than compete with it?
- Does every dense scene fully resolve before its reading hold?
- Are all settled parent groups pixel-stable in the decoded video?
- Are ambient layers subtle, deterministic, and continuous across exposed cuts?
- Does each transition begin in the outgoing scene and end in the incoming scene?
- Would removing any animation improve clarity? If yes, remove it.

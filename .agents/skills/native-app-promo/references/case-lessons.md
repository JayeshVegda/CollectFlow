# Generalized lessons from the Moxi English case

The Moxi English film used one iOS Simulator product surface, real native screenshots as proof, and
frame-driven HTML/React layers as the primary storytelling system. Its final iteration demonstrated
several reusable lessons:

1. A real screenshot is mandatory for product proof, but not automatically useful. A 5-second
   runtime fragment without a clear narrative relation can weaken the story more than a shorter,
   selected native state. Never ship an HTML-only phone UI because capture was inconvenient.
2. Feature identity colors must come from the product. A visually attractive dark or saturated
   treatment can still be wrong when it changes the module's meaning.
3. Feature comprehension needs a settle window. Adding 2–3 seconds after the final information
   appears is often better than adding another animation.
4. Real native detail works best as an anchor. Enlarge difficulty, evidence, waveform, vocabulary,
   or revision state in HTML instead of asking viewers to read a small phone screenshot.
5. A bridge must begin in the outgoing scene and end in the incoming scene. A new transition scene
   that only resembles both sides still feels like a cut.
6. Simulator screenshots may already contain system chrome. Adding a synthetic Dynamic Island can
   create a doubled or cropped island.
7. Endless breathing on a large editor, phone, or scene group reads as jitter after information has
   settled. Freeze the parent and leave motion only in a waveform, cursor, or highlight.
8. Per-scene texture phase resets and extrapolated camera transforms can look like a one-frame
   upward pull. Keep phase continuous or mask the boundary, and clamp motion at completion.
9. Protect accepted sections. When feedback targets a scene, compare scene-local frames and avoid
   changing shared primitives that affect the entire film.
10. Technical QA must decode the final MP4. Source inspection alone cannot prove that the encoded
    film is stable, complete, correctly colored, and free of black frames.

Use these as failure-prevention rules, not as a request to imitate Moxi's warm paper, ink line,
typography, scene count, feature order, or runtime.

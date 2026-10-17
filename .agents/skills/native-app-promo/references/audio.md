# Audio design and rights

Lock visual timing before final sound design. Choose a BGM whose energy curve supports the story,
then map beats, scene boundaries, transitions, and the closing cadence. Duck the music under dense
native proof or important UI texture instead of increasing every sound.

Use SFX for physical meaning: paper movement for material transfer, a restrained click for a user
decision, a soft whoosh for spatial travel, a pen texture for writing, and an impact only for a real
structural landing. Avoid game-like confirmation tones unless they match the product.

Pin each cue to `scene.from + localOffset`. Set explicit duration, fade, and volume. Deliver:

- one MP4 with BGM and SFX;
- one MP4 with identical visuals and SFX but without BGM.

Maintain `audio-attribution.md` with file name, title, creator, source URL, license URL, downloaded
hash, edit notes, and whether redistribution of the source asset is allowed. Do not commit an audio
file merely because it appears in another project. Generated sound must document its synthesis and
must not contain an undeclared sample.

Measure integrated loudness or at minimum peak and RMS. Reject clipping. Require 48 kHz stereo for
final delivery unless the platform specifies otherwise. Confirm the audio and video end together.

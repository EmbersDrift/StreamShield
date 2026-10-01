# OBS hitbox regression checklist

Scope: Minecraft 1.21.11 and 26.1.2, Fabric and NeoForge. No 26.3 port is included.

## Implementation boundary

Only gizmos emitted by the vanilla entity-hitbox debug renderer are tagged. When enabled and the overlay is available, tagged geometry uses its own buffer and render target. The normal entity/item buffers are never flushed into that target. Color/depth overrides are restored on success and failure; the private depth copy does not modify world depth.

This is an independent implementation of the feature, informed by Streamproof's approach. Streamproof is not a dependency. Its shared-buffer flush is a plausible source of unrelated held-item geometry entering the hidden target; the original bug has not been reproduced here.

## Manual checks before release

Use Windows and OBS Game Capture; compare the game window and an actual recording, not just the game screenshot. Repeat on all four version/loader combinations.

1. With the new option off, toggle F3+B: hitboxes should appear in both game and recording.
2. Enable OBS overlay and **Hide hitboxes from OBS**. Hitboxes should remain in-game and disappear from the recording.
3. Observe another player holding a sword, bow, shield, block and an offhand item. Their model, armor and items must remain visible in both views while only debug hitboxes disappear from the recording. Repeat for mobs holding items and dropped items.
4. Test first/third person, nearby entities, entities behind opaque blocks, water/glass, and the local first-person hand. Check visual depth/order against the option-off baseline.
5. Toggle F3+B and the option repeatedly; disconnect/rejoin, resize, change fullscreen, and reload resources. No stale hitboxes or missing items should remain.
6. Verify the existing HUD hiding options still work independently and overlay HUD stays above hitboxes.
7. Test with Sodium and then with the actual streaming modpack, including Replay if used. Do not enable another mod's hitbox concealment simultaneously.
8. Disable the overlay: hitboxes must return to normal rendering. If initialization fails, do not assume they are capture-hidden; turn off F3+B before streaming.

Automated scope/state tests and startup smoke tests do not prove OBS exclusion or visual correctness. These recording checks remain necessary.

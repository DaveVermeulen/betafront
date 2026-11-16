# Roadmap

What's planned next for Betafront. Nothing here has a date yet.

## 1. Smooth lighting for OBJ faces

**Now:** each OBJ face gets one flat light level: the light of the block it faces when it lies on the block's edge, otherwise the light of the model's own block. Day and night, torches and glowing materials work, but there are no soft gradients and no ambient occlusion (darker inner corners), so models look flat next to vanilla blocks when Smooth Lighting is on.

**Goal:** with Smooth Lighting on, every vertex of an OBJ face gets its own light and shade, sampled from the blocks around it, the way vanilla full faces do.

Rough steps:

1. **BTA first.** Read how BTA 8 computes per-vertex light for cube faces (`RenderBlocks` / `LightingCache`): which neighbours each corner averages, and how the ambient-occlusion shade is passed (`setShade1i`).
2. For each OBJ vertex, sample the light index of the blocks around its position on the side its normal points to. Blend the samples by where the vertex sits (trilinear), so vertices inside the block get in-between values. Cache samples per block, so a 45-face model doesn't query the world hundreds of times.
3. Ambient occlusion: darken a vertex by how many solid blocks are around it, using vanilla's per-corner rule for faces flat on the block edge and an approximation for faces inside the block.
4. Respect the player's Smooth Lighting setting, and the model JSON's `ambient_occlusion` option (already parsed, default `true`).
5. Check it in game: the example crystal in a dark corner, next to a torch, at sunset, and the ramp against walls.
6. **StationAPI.** Its baked models already get vanilla smooth lighting for faces flat on the block edge. Faces inside the block need the same sampling step, through StationAPI's renderer.

## 2. Publish to a Maven repository

**Now:** a mod that wants to use Betafront has to download the jar and add it by hand.

**Goal:** a modder adds a repository and one dependency line:

```kotlin
repositories { maven("https://jitpack.io") }
dependencies { implementation("com.github.DaveVermeulen:betafront:bta-v0.1.0-alpha.1") }
```

Options:

| Option | Good | Not so good |
| --- | --- | --- |
| **JitPack** (recommended) | Builds straight from a GitHub tag, nothing to host or upload, free for public repos | The first request for a version triggers a build on JitPack's servers, which can be slow or fail; it has to download BTA and the Babric toolchain itself. DragonFly moved away from it after build problems, so test it early |
| Signalum Maven (where HalpLibe and BTA's loader live) | Where BTA modders already look; a plain Maven with prebuilt jars | Needs publish access from its maintainers; BTA only |
| A Maven folder on GitHub Pages | Prebuilt jars, no third party, no auth for users | We publish each release ourselves (a Gradle task or a GitHub Actions workflow) |
| GitHub Packages | Free, tied to the repo | Everyone who depends on it needs a GitHub token, even for public packages |

Rough steps (JitPack):

1. Add `maven-publish` to `bta/` and `stationapi/` with clear artifact names (`betafront-bta`, `betafront-stationapi`), including the sources jars.
2. Add a root `publishToMavenLocal` task that runs both, since JitPack runs that in the repository root, and a `jitpack.yml` that picks JDK 21 to run Gradle.
3. Push a tag, then request it on jitpack.io and read the build log. Fix whatever its build machine trips over (BTA's download servers, memory).
4. Add a "Using Betafront as a dependency" section to the README with the snippet above, for both BTA and StationAPI.
5. If JitPack builds turn out unreliable, publish the same artifacts to a Maven folder on GitHub Pages instead. Later, ask the Signalum maintainers about publishing the BTA build there too.

## 3. Small performance wins (BTA)

The expensive work already happens once: OBJ files are parsed and turned into quads when models load, and each rotation is cached. Blocks are drawn only when their chunk is rebuilt, not every frame. The StationAPI build bakes everything into StationAPI's own quads, so there's nothing to gain there. What's left is cheap per quad but repeated for every quad, so it adds up for detailed models:

1. **Cache texture lookups.** Every quad looks its texture up in the atlas again, on every chunk rebuild and, for items, every frame. A missing texture is even worse: each lookup throws and catches an exception. Look each texture up once per model and refresh when the atlas is rebuilt.
2. **Look up light and neighbours once per side, not per quad.** A block has at most 7 light values that matter (its own and its 6 neighbours) and 6 neighbour visibility checks, but today a 45-face crystal does 45 light lookups and allocates a position object for each culled face. Compute those 13 values once per block and reuse them.
3. **Flatter quad data.** Store each rotation's quads as one `float[]` instead of nested arrays, so a rebuild walks memory in order. Small win; do it together with smooth lighting, which reworks this code anyway.
4. **Warn about very detailed models.** Log a warning when a model has more than a few hundred quads. There's no level-of-detail in Beta, so a 5,000-face model makes every chunk it's in slow to rebuild.

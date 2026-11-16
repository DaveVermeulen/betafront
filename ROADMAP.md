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
repositories { maven("https://api.modrinth.com/maven") }
dependencies { implementation("maven.modrinth:betafront:0.1.0-alpha.1+8.0.1") }
```

Options:

| Option | Good | Not so good |
| --- | --- | --- |
| **Modrinth Maven** (recommended first) | Every Modrinth upload is a Maven artifact automatically; the same upload is what players download; no server to run | Needs a Modrinth project and a short approval; artifact name is `maven.modrinth:betafront` |
| Signalum Maven (where HalpLibe and BTA's loader live) | Where BTA modders already look | Needs publish access from its maintainers |
| JitPack | No setup | It builds the repo itself and needs BTA's download servers; DragonFly moved away from it for build problems |
| GitHub Packages | Free, tied to the repo | Everyone who depends on it needs a GitHub token, even for public packages |

Rough steps (Modrinth):

1. Create the Modrinth project "Betafront" (icon, description, LGPL-2.1-only, links to this repo) with two versions per release: one for BTA and one for Babric/StationAPI.
2. Add the `com.modrinth.minotaur` Gradle plugin to `bta/` and `stationapi/` so `./gradlew -p bta modrinth` uploads the jar (and its sources jar) with the right game version and the HalpLibe or StationAPI dependency. The upload token stays in an environment variable, never in the repo.
3. Optionally a GitHub Actions workflow that builds and publishes when a `bta-v*` or `stationapi-v*` tag is pushed, so a release is one tag.
4. Add a "Using Betafront as a dependency" section to the README with the snippet above.
5. Later, ask the Signalum maintainers about publishing there too, since BTA modders already use it for HalpLibe.

## 3. Small performance wins (BTA)

The expensive work already happens once: OBJ files are parsed and turned into quads when models load, and each rotation is cached. Blocks are drawn only when their chunk is rebuilt, not every frame. The StationAPI build bakes everything into StationAPI's own quads, so there's nothing to gain there. What's left is cheap per quad but repeated for every quad, so it adds up for detailed models:

1. **Cache texture lookups.** Every quad looks its texture up in the atlas again, on every chunk rebuild and, for items, every frame. A missing texture is even worse: each lookup throws and catches an exception. Look each texture up once per model and refresh when the atlas is rebuilt.
2. **Look up light and neighbours once per side, not per quad.** A block has at most 7 light values that matter (its own and its 6 neighbours) and 6 neighbour visibility checks, but today a 45-face crystal does 45 light lookups and allocates a position object for each culled face. Compute those 13 values once per block and reuse them.
3. **Flatter quad data.** Store each rotation's quads as one `float[]` instead of nested arrays, so a rebuild walks memory in order. Small win; do it together with smooth lighting, which reworks this code anyway.
4. **Warn about very detailed models.** Log a warning when a model has more than a few hundred quads. There's no level-of-detail in Beta, so a 5,000-face model makes every chunk it's in slow to rebuild.

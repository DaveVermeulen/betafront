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

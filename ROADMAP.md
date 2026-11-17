# Roadmap

What's planned next for Betafront. Nothing here has a date yet.

## 1. Smooth lighting for OBJ faces: done in 1.0.0

BTA: every OBJ vertex gets its own light and ambient-occlusion shade from BTA's `LightingCache`, with vanilla's per-corner rule, when Smooth Lighting is on. StationAPI: its renderer already lights every vertex; models that sit low in the block can set `"ambient_occlusion": false`.

Possible follow-up: StationAPI blends the light of solid neighbours into faces inside a block, which darkens small models in pits. A mixin into StationAPI's lighting could skip solid neighbours like vanilla does, but it would change how every StationAPI model with inner faces looks.

## 2. Publish to a Maven repository

**Now:** a mod that wants to use Betafront has to download the jar and add it by hand.

**Goal:** a modder adds a repository and one dependency line:

```kotlin
repositories { maven("https://davevermeulen.github.io/betafront/maven") }
dependencies { implementation("io.github.davevermeulen:betafront-bta:1.0.0+8.0.1") }
```

**Plan: our own Maven on GitHub Pages, published by GitHub Actions.** A Maven repository is just a folder of jars and small metadata files served over HTTPS, so GitHub Pages can host it for free. Users need no login and no third-party build, and every version is the exact jar we built and tested.

Rough steps:

1. Add `maven-publish` to `bta/` and `stationapi/` with clear coordinates: `io.github.davevermeulen:betafront-bta` and `io.github.davevermeulen:betafront-stationapi`, with sources jars and the dependencies (HalpLibe or StationAPI) in the POM.
2. Add a GitHub Actions workflow that runs when a release tag is pushed (`bta-v*` or `stationapi-v*`): build with JDK 21, run the core tests, publish that build into a local folder, then commit the folder to a `gh-pages` branch under `maven/`. Existing versions are never overwritten.
3. The same workflow attaches the jars to the GitHub release, so making a release becomes: bump the version, push a tag.
4. Turn on GitHub Pages for the `gh-pages` branch (repository settings, one time).
5. Add a "Using Betafront as a dependency" section to the README with the snippet above, for both BTA and StationAPI.

Other options, if the plan runs into trouble:

| Option | Good | Not so good |
| --- | --- | --- |
| JitPack | Builds straight from a GitHub tag, nothing to set up | The first request for a version builds on JitPack's servers, which can be slow or fail; DragonFly moved away from it after build problems |
| Signalum Maven (where HalpLibe and BTA's loader live) | Where BTA modders already look | Needs publish access from its maintainers; BTA only |
| GitHub Packages | Free, tied to the repo | Everyone who depends on it needs a GitHub token, even for public packages |

## 3. Small performance wins (BTA)

The expensive work already happens once: OBJ files are parsed and turned into quads when models load, and each rotation is cached. Blocks are drawn only when their chunk is rebuilt, not every frame. The StationAPI build bakes everything into StationAPI's own quads, so there's nothing to gain there. What's left is cheap per quad but repeated for every quad, so it adds up for detailed models:

1. **Done in 1.0.0: cache texture lookups.** Every quad looks its texture up in the atlas again, on every chunk rebuild and, for items, every frame. A missing texture is even worse: each lookup throws and catches an exception. Look each texture up once per model and refresh when the atlas is rebuilt.
2. **Done in 1.0.0: look up light and neighbours once per side, not per quad.** A block has at most 7 light values that matter (its own and its 6 neighbours) and 6 neighbour visibility checks, but today a 45-face crystal does 45 light lookups and allocates a position object for each culled face. Compute those 13 values once per block and reuse them.
3. **Flatter quad data.** Store each rotation's quads as one `float[]` instead of nested arrays, so a rebuild walks memory in order. Small win; do it together with smooth lighting, which reworks this code anyway.
4. **Warn about very detailed models.** Log a warning when a model has more than a few hundred quads. There's no level-of-detail in Beta, so a 5,000-face model makes every chunk it's in slow to rebuild.

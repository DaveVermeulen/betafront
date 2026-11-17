# Betafront: knowledge for contributors and AI agents

Everything someone needs to pick up work on Betafront without the history of how it got here. Read this before changing code. Players and modders want [docs/](docs/README.md) instead.

## What the project is

Betafront lets Minecraft Beta mods use Wavefront `.obj` files as block models. The model JSON format is copied from NeoForge 1.21.1's `neoforge:obj` loader, so NeoForge OBJ models carry over with few edits.

There are two mod builds that share one parser:

| Build | Game | Library it hooks into | Status |
| --- | --- | --- | --- |
| `bta/` | Better Than Adventure 8.0.1 | HalpLibe 6.2.0+8.0.1 | **Main focus.** Released as 0.1.0-alpha.1 |
| `stationapi/` | Regular Babric, Beta 1.7.3 | StationAPI 2.0.0-alpha.6.5 | Kept working, not released yet |

DragonFly (a BTA model library) may become a second BTA backend later. It has no BTA 8 version yet.

## Repository layout

```
core/         Pure Java OBJ/MTL parser + JUnit tests. No game code. Compiled into both mod jars.
bta/          BTA mod (Kotlin DSL build copied from Turnip-Labs/bta-example-mod)
stationapi/   StationAPI mod (Groovy build, fabric-loom + babric-loom-extension)
docs/         Modder documentation
.run/         Shared IntelliJ run configurations
settings.gradle, build.gradle, gradle.properties   Root composite build (includeBuild of all three)
README.md, ROADMAP.md, CHANGELOG.md, KNOWLEDGE.md (this file), CLAUDE.md
```

Each folder is its **own Gradle build**, because BTA and StationAPI need different Loom versions. The root build only joins them for IntelliJ and adds shortcut tasks. Both mod builds add `../core/src/main/java` as a source folder.

### Packages

- `io.github.davevermeulen.betafront.obj`: core parser (`ObjTokenizer`, `ObjMaterialLibrary`, `ObjGeometry`, `ObjSettings`)
- `io.github.davevermeulen.betafront.bta`, `.bta.client`, `.bta.example`
- `io.github.davevermeulen.betafront.stationapi`, `.stationapi.client`, `.stationapi.example`

Mod id is `betafront` in both builds. The model loader id is `betafront:obj`; `neoforge:obj` and `forge:obj` are accepted too.

## Building and testing

Gradle needs **Java 21 or newer** to run; the mods compile for Java 17 (toolchain).

| What | Command (repo root) | IntelliJ run config |
| --- | --- | --- |
| Parser tests | `./gradlew testCore` or `./gradlew -p core test` | Core Tests |
| Everything | `./gradlew buildAll` | Build All |
| Clean | `./gradlew cleanAll` | |
| BTA dev client | `./gradlew runClientBta` or `-p bta runClient` | Run Client (BTA) |
| Babric dev client | `./gradlew runClientBabric` or `-p stationapi runClient` | Run Client (Babric) |

Jars land in `bta/build/libs/betafront-bta-<version>+8.0.1.jar` and `stationapi/build/libs/betafront-stationapi-<version>.jar`.

Game logs from dev clients: `bta/run/logs/latest.log` and `stationapi/run/logs/latest.log`. Read these after a crash; IntelliJ's run window isn't visible to agents.

### Where builds can run

- **Claude cloud sessions cannot build the mods.** Their network blocks the BTA, Fabric, Signalum, Mojang and Glass/Babric hosts. They *can* compile and run the core tests with plain `javac` and a JUnit console jar from Maven Central (`repo1.maven.org` works), and edit code and docs.
- **A developer machine** with JDK 21+ builds everything.
- **Agents can't see the game.** An agent can start a client and read its log for crashes, but anything visual (lighting, model orientation, item size) must be checked in game by a person. Say so instead of claiming it works.

## Conventions

- **Licence:** the whole mod is LGPL-2.1-only. Files adapted from NeoForge keep their original header and list what changed. Keep that when editing them.
- **README keeps the AI disclaimer and the dependency tables.** Update the tables when a version changes.
- **Example blocks are dev-only** (`FabricLoader.isDevelopmentEnvironment()`), never in a normal game.
- Keep `core/` free of game and library code (no Gson, no Minecraft classes). Anything platform-specific goes in `bta/` or `stationapi/`.
- Update `CHANGELOG.md` for user-visible changes, and `ROADMAP.md` when an item is done or changes.

## How the code works

### Core (`core/`)

1. `ObjTokenizer` reads lines, joins `\` continuations, splits on whitespace. Only a `#` at the start of a line is a comment, so `map_Kd #texture0` survives.
2. `ObjMaterialLibrary` reads `.mtl`: `newmtl`, `Ka`, `Kd`, `Ks`, `map_Kd`, `d`, `Tr`, `Ns`, and `neoforge_TintIndex` / `forge_TintIndex` / `betafront_TintIndex`.
3. `ObjGeometry.parse(tokenizer, settings, materialLoader)` builds objects, groups and meshes. A missing `mtllib` file gives an empty library (Blender exports often name one that isn't shipped). `usemtl #name` with no such material makes a synthetic material whose texture is `#name`.
4. `ObjGeometry.buildQuads(mesh)` turns faces into quads like NeoForge's `makeQuad`: triangles repeat their last vertex, polygons with more than 4 vertices are fan-split. It fills in a face normal and default UVs when they're missing, and applies `flip_v`.
5. `cullSide(positions, normal)` returns the block side a quad lies flat on (or null). It's used for automatic culling and lighting.

Tests: `core/src/test/.../ObjGeometryTest.java` (15 tests) with fixtures in `core/src/test/resources/objtest/`, including copies of the shipped crystal and ramp.

### BTA build (`bta/`)

- `Betafront` (main entrypoint): `MOD_ID = HalpLibe.registerMod("betafront", true)` (the `true` makes HalpLibe stitch `assets/betafront/textures/block/*` into the atlas). Registers example blocks in dev.
- `BetafrontClient` (Fabric `client` entrypoint, which BTA's loader supports): listens to HalpLibe `ClientEvents.BLOCK_MODEL_RELOAD` and adds our models to the dispatcher.
- `ObjModels`: the public API. `ObjModels.block(block, "modid:block/name")` or with an `IntUnaryOperator` from metadata to Y rotation in degrees.
- `ObjModelDefinition`: loads `assets/<ns>/models/<path>.json` from the classpath (no texture-pack support yet), parses the OBJ through core, resolves `#texture` references, and keeps the quads with one cached copy per rotation.
- `ObjBlockModel extends BlockModelStandard`: `render(TessellatorGeneral, WorldSource, TilePosc)` for chunks, `renderStandalone(TessellatorGeneral, meta, byte lightIndex)` for items.
- `ExampleContent`: crystal (id 9000, light 7) and ramp (id 9001, faces the player through metadata 0 to 3), both in the creative inventory under Miscellaneous.

### StationAPI build (`stationapi/`)

- `ObjModelsClient` listens to `PreLoadUnbakedModelEvent` and swaps in `ObjModelLoader` for JSONs whose `loader` is an OBJ loader id.
- `ObjUnbakedModel.bake()` turns the core quads into StationAPI `BakedQuad`s (int[32] vertex data; `Sprite.getFrameU` takes 0 to 16). Blockstate rotations, culling, tint and emission go through StationAPI.
- Asset paths have an extra `stationapi/` folder: `assets/<ns>/stationapi/models/...` and `.../textures/...`.
- Example blocks are craftable in dev (1 dirt makes 4 crystals, 1 cobblestone makes 4 ramps), since Beta 1.7.3 has no creative mode.

## BTA 8.0.1 API facts (checked against the real jar)

Many older BTA guides and DragonFly (BTA 7.3) use different names. Verified for 8.0.1:

| Thing | 8.0.1 name |
| --- | --- |
| Block model render | `render(TessellatorGeneral, WorldSource, TilePosc)` |
| Item render | `renderStandalone(TessellatorGeneral, int meta, byte lightIndex)`, offset with `tessellator.offsetTranslation(±0.5, ...)` |
| Light | `block.getLightIndex(world, TilePosc)` returns a byte with sky and block light; `LightIndexHelper.blockLightFromIndex` / `setBlockLight` |
| Tessellator | `setLightmapCoord1i`, `setShade1i` (ambient-occlusion factor only), `setColorOpaque3f`, `setColor1i`, `setNormal`, `addVertexWithUV` |
| Face culling | `BlockModelStandard#shouldSideBeRendered(WorldSource, AABBdc, TilePos neighbour, Side, meta)` |
| Sides | `Side.BOTTOM`, `TOP`, `NORTH`, ...; `offsetX()`; `TilePosc#add(Side, TilePos)` |
| Textures | `TextureRegistry.getTexture("ns:block/name")` returns an `IconCoordinate`; **it throws** for an id without a known atlas folder; `getSubIconU/V` take 0 to 1 |
| Materials | `Materials.GLASS`, `Materials.STONE` |
| Placement | `BlockLogic#onPlacedByMob(World, TilePosc, Side, Mob, double, double)`, `world.setBlockDataNotify(TilePosc, int)` |
| Block ids | `BlockBuilder.build(name, numericId, logicSupplier)`, ids 0 to 16383 |
| Creative menu | Only blocks with `BlockBuilder.setCreativeInventoryPlacement(new CreativeInventoryPlacement.Category(...))` show up |
| BlockBuilder | Every `set...` returns a **copy**: always use the return value. `build("example_crystal", ...)` gives the lang key `tile.<modid>.example.crystal` (underscores become dots) |
| Lang file | `assets/<ns>/lang/en_US/en_US.lang`, e.g. `tile.betafront.example.crystal.name=Example Crystal` |
| Light index | A byte: `sky << 4 \| block` (`LightIndexHelper.lightIndex2i(sky, block)`, `skyLightFromIndex`, `avg`) |
| Smooth lighting | `RenderBlocks.setupLighting` with the public `renderBlocks.cache` (`LightingCache`: `setupCache(block, world, pos)`, then `getLightIndex(dx,dy,dz)`, `getBrightness(dx,dy,dz)` = AO factor, `getOpacity(dx,dy,dz)` for offsets -1..1). Each face corner averages the 4 blocks touching it with `LightIndexHelper.avg`, skipping ones hidden behind two solid blocks. On only when `GameSettings.AMBIENT_OCCLUSION.value` is true **and** `block.emission == 0` |
| Atlas reloads | `TextureRegistry.reloadGeneration` changes whenever the atlas is rebuilt; use it to invalidate cached `IconCoordinate`s |

Lighting rules learned the hard way:

- **Never bake world brightness into vertex colour or shade.** Pass the light index with `setLightmapCoord1i`; BTA's lightmap applies day/night when drawing. Baking it froze models at the time of day when their chunk was built.
- Directional face shading comes from the normal. For a quad with `shade_quads` off, send an upward normal.
- Faces flat on the block edge take light from the neighbour they face; faces inside the block from the block itself.

To read BTA's code: Loom's `genSources` fails for BTA ("Failed to get class entries"). Decompile the merged jar from the Gradle cache with Vineflower instead, just the classes you need (on a machine that has built the BTA mod once):

```bash
J=~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/8.0.1/minecraft-merged-deobf-8.0.1.jar
V=$(find ~/.gradle/caches -name "vineflower-*.jar" | head -1)
mkdir -p /tmp/bta-classes /tmp/bta-src && cd /tmp/bta-classes
unzip -qo "$J" 'net/minecraft/client/render/block/model/RenderBlocks*'
java -jar "$V" /tmp/bta-classes /tmp/bta-src
```

For signatures only, `javap -cp "$J" net.minecraft.core.util.helper.Side` is quicker. HalpLibe's jar: `find ~/.gradle/caches/modules-2 -name "halplibe-6.2.0+8.0.1.jar"`. Check names against the jars; don't guess them.

## Known pitfalls

| Symptom | Cause and fix |
| --- | --- |
| BTA client crashes on macOS: "GLFW may only be used on the main thread" | Needs `-XstartOnFirstThread`. Already set for Macs in `bta/build.gradle.kts`. |
| "Java heap space" while configuring `:bta` | The root composite build needs the 4 GB heap in the root `gradle.properties`. Run `gradlew --stop` after a failed run (the Loom cache lock stays held). |
| `Can't connect to Mojang API ... lookup/name/Player123` in the log | Harmless. The dev player name isn't a real account. |
| Example blocks missing in BTA | They must have a creative inventory placement (see above). |
| `babric-loom-extension` 1.17.4 throws `UnsupportedOperationException` | Use 1.17.5. |
| Gradle fails on Java 17 | Gradle itself needs Java 21+. |
| `gh pr create` or GraphQL fails in the cloud | GraphQL is blocked there; use REST (`gh api`). We don't use PRs anyway. |

## Releases

- BTA tags look like `bta-v0.1.0-alpha.1`, published as a GitHub **pre-release** with the jar and sources jar attached.
- Version lives in `bta/gradle.properties` (`mod_version`), and the jar name adds `+8.0.1`.
- Steps: bump the version, add a CHANGELOG entry, `./gradlew cleanAll buildAll`, commit, push, tag, `gh release create <tag> --prerelease` with the jars. Publishing needs the maintainer's OK.
- The planned Maven setup (GitHub Pages, filled by a GitHub Action on each tag) is ROADMAP item 2.

## Working with several agents

The project is split so agents can work in parallel without stepping on each other:

| Area | Files | Can a cloud agent finish it alone? |
| --- | --- | --- |
| Parser and tests | `core/` | Yes (javac + JUnit jar) |
| BTA rendering | `bta/src/main/java/.../bta/client/` | No: needs a full build and someone looking at the game |
| BTA example content | `bta/.../bta/example/`, `bta/src/main/resources/` | No: needs a build |
| StationAPI build | `stationapi/` | No: needs a build |
| Docs, roadmap, changelog | `README.md`, `docs/`, `ROADMAP.md`, `CHANGELOG.md`, this file | Yes |
| Build and release tooling | Gradle files, `.run/`, future `.github/workflows/` | Partly |

Rules for parallel work:

1. Take one roadmap item or one area. Don't refactor files outside your area in the same change.
2. Pull before you start and again before you commit. Keep commits small.
3. Changes to `core/` affect both builds: run the core tests, and say that both mods need a rebuild.
4. When you learn something another agent would need (an API name, a pitfall, a maintainer's decision), add it here in the same commit.
5. Say plainly what you could and couldn't verify: built, tested, or seen in game.
6. Only one agent runs a dev client at a time (they share `bta/run/` and `stationapi/run/`, and there's one screen). Don't launch `runClient*` unless asked, since it opens a game window; close it afterwards. `--dry-run` checks the wiring without launching.
7. When the maintainer says "pause" or "stop": stop editing at once, leave files as they are (don't revert or commit), and report `git status` and what the uncommitted diff does.

## Current state (keep this up to date)

- Released: BTA 0.1.0-alpha.1. `main` has more since then: mod icon, dev recipes for StationAPI, root heap fix, roadmap, docs.
- **In progress:** ROADMAP item 1 (smooth lighting for OBJ faces, BTA). A draft (per-vertex light and AO from BTA's `LightingCache` when Smooth Lighting is on, glowing blocks stay flat like vanilla, plus cached texture and light lookups) exists outside `main` and is paused. Don't start it again elsewhere without checking.
- Next: ROADMAP item 2 (Maven on GitHub Pages), item 3 (small performance wins; partly in the draft above).

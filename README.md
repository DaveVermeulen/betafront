# ObjModelsBabric

Wavefront `.obj` block and item models for [StationAPI](https://github.com/ModificationStation/StationAPI) on Babric (Minecraft Beta 1.7.3).

ObjModelsBabric is an add-on: StationAPI stays unmodified. Any StationAPI model JSON can point at an `.obj` file, using the same JSON shape as NeoForge's `neoforge:obj` loader, so models made for modern NeoForge carry over with few edits.

> **AI disclaimer:** most of this mod's code and documentation was written with the help of Claude, an AI assistant by Anthropic, and reviewed by the author. The OBJ parsing code is adapted from NeoForge (see [Licence](#licence)). Please report anything that looks wrong as an issue.

## Dependencies

To play with it:

| Dependency | Version | Notes |
| --- | --- | --- |
| Minecraft | Beta 1.7.3 | Client side; the mod only changes rendering |
| [Babric](https://github.com/babric/prism-instance) (Fabric Loader for beta) | 0.19.3 or newer | The Babric Prism instance is the easiest way to set it up |
| [StationAPI](https://modrinth.com/mod/stationapi) | 2.0.0-alpha.6.5 or newer | Required; ObjModelsBabric hooks into its model loader |
| Java | 17 or newer | |

To build it:

| Dependency | Version |
| --- | --- |
| JDK | 17 or newer |
| Gradle | 9.5.1 (wrapper included) |
| fabric-loom | 1.17.20 |
| babric-loom-extension | 1.17.4 |
| biny mappings | b1.7.3+e1fe071 |
| JUnit Jupiter (tests only) | 5.10.2 |

## Using it in a mod

Add ObjModelsBabric as a dependency, then write the model JSON as usual under `assets/<modid>/stationapi/models/`:

```json
{
  "loader": "objmodelsbabric:obj",
  "model": "examplemod:stationapi/models/block/lamp.obj",
  "flip_v": true,
  "textures": {
    "particle": "examplemod:block/lamp",
    "texture0": "examplemod:block/lamp"
  }
}
```

Blockstate files don't change; they point at this JSON like any other model, including `x`/`y` rotations. Item models work the same way: put the same kind of JSON in `models/item/`.

Blocks with an OBJ model are usually not full cubes, so override `isOpaque()` and `isFullCube()` to return `false`. Otherwise neighbouring faces get hidden behind it.

### Options

| Key | Default | What it does |
| --- | --- | --- |
| `loader` | required | `objmodelsbabric:obj` (`neoforge:obj` and `forge:obj` are accepted too) |
| `model` | required | The `.obj` file, relative to the namespace root |
| `mtl_override` | none | Use this `.mtl` instead of the file's `mtllib` line |
| `textures` | none | Texture map. `usemtl #texture0` or MTL `map_Kd #texture0` refers to `texture0` here |
| `flip_v` | `false` | Use `1 - v`. Blender and Blockbench exports usually need `true` |
| `automatic_culling` | `true` | Faces lying flat on a block side are hidden by a solid neighbour on that side |
| `shade_quads` | `true` | Apply the game's per-direction face shading |
| `emissive_ambient` | `true` | An MTL `Ka` above 0 makes that material glow (average of Ka, 0 to full bright) |
| `ambient_occlusion` | `true` | Use smooth lighting when the player has it on. Not a NeoForge option |

### Textures

- The simplest setup needs no `.mtl` at all: name the material `#texture0` in your modelling tool (`usemtl #texture0`) and map `texture0` in the JSON. A missing `.mtl` file named by `mtllib` is ignored.
- With an `.mtl`, `map_Kd` can be a texture id (`examplemod:block/lamp`) or a reference to the JSON texture map (`#texture0`).
- Faces with no material, or a material without `map_Kd`, use `#default` and then `#particle`.
- Texture ids work like StationAPI JSON models: `examplemod:block/lamp` is `assets/examplemod/stationapi/textures/block/lamp.png`.

### Supported OBJ features

- `v`, `vt`, `vn`, `f` (including negative indices and `v//vn`), `o`, `g`, `s`, `usemtl`, `mtllib`.
- Triangles, quads and larger polygons. Larger polygons are fan-split, so they should be convex.
- MTL: `newmtl`, `Kd`, `Ka`, `map_Kd`, `d`, plus `neoforge_TintIndex`/`objmodelsbabric_TintIndex` for block colour tinting.
- Coordinates are block units: `0` to `1` fills one block.

Not supported yet: entity and block-entity rendering, per-vertex colours (`vc`), MTL colour tints (`Kd` is read but not applied), group visibility toggles.

## Example content

In the development client (`./gradlew runClient`) the mod registers two test blocks, each with an OBJ item model too:

- `objmodelsbabric:tryrolite_crystal`: a crystal cluster exported from Blender, with no `.mtl` (uses `usemtl #texture0`).
- `objmodelsbabric:obj_ramp`: a ramp that turns to face the player, to check blockstate rotations and culling.

They are not registered in a normal game.

## Building

```
./gradlew build     # mod jar in build/libs, also runs the tests
./gradlew test      # parser unit tests only
./gradlew runClient # dev client with the example blocks
```

## Licence

LGPL-2.1-only, see [LICENSE](LICENSE). The OBJ tokenizer, parser, material library and quad baking are adapted from [NeoForge](https://github.com/neoforged/NeoForge) 1.21.1 (Forge Development LLC and contributors, LGPL-2.1-only); those files keep their original headers and list the changes made.

# OBJ Models

Wavefront `.obj` block and item models for [StationAPI](https://github.com/ModificationStation/StationAPI) on Babric (Minecraft Beta 1.7.3).

OBJ Models is an add-on: StationAPI stays unmodified. Any StationAPI model JSON can point at an `.obj` file, using the same JSON shape as NeoForge's `neoforge:obj` loader.

## Using it in a mod

Add OBJ Models as a dependency, then write the model JSON as usual under `assets/<modid>/stationapi/models/`:

```json
{
  "loader": "objmodels:obj",
  "model": "examplemod:stationapi/models/block/lamp.obj",
  "flip_v": true,
  "textures": {
    "particle": "examplemod:block/lamp",
    "body": "examplemod:block/lamp"
  }
}
```

Blockstate files don't change; they point at this JSON like any other model, including `x`/`y` rotations. Item models work the same way: put the same kind of JSON in `models/item/`.

### Options

| Key | Default | What it does |
| --- | --- | --- |
| `loader` | required | `objmodels:obj` (`neoforge:obj` and `forge:obj` are accepted too) |
| `model` | required | The `.obj` file, relative to the namespace root |
| `mtl_override` | none | Use this `.mtl` instead of the file's `mtllib` line |
| `textures` | none | Texture map. An MTL `map_Kd #body` refers to `body` here |
| `flip_v` | `false` | Use `1 - v`. Blender and Blockbench exports usually need `true` |
| `automatic_culling` | `true` | Faces lying flat on a block side are hidden by a solid neighbour on that side |
| `shade_quads` | `true` | Apply the game's per-direction face shading |
| `emissive_ambient` | `true` | An MTL `Ka` above 0 makes that material glow (average of Ka, 0 to full bright) |
| `ambient_occlusion` | `true` | Use smooth lighting when the player has it on. Not a NeoForge option |

### Textures

- `map_Kd` in the `.mtl` can be a texture id (`examplemod:block/lamp`) or a reference to the JSON texture map (`#body`).
- Faces with no material, or a material without `map_Kd`, use `#default` and then `#particle`.
- Texture ids work like StationAPI JSON models: `examplemod:block/lamp` is `assets/examplemod/stationapi/textures/block/lamp.png`.

### Supported OBJ features

- `v`, `vt`, `vn`, `f` (including negative indices and `v//vn`), `o`, `g`, `s`, `usemtl`, `mtllib`.
- Triangles, quads and larger polygons. Larger polygons are fan-split, so they should be convex.
- MTL: `newmtl`, `Kd`, `Ka`, `map_Kd`, `d`, plus `neoforge_TintIndex`/`objmodels_TintIndex` for block colour tinting.
- Coordinates are block units: `0` to `1` fills one block.

Not supported yet: entity and block-entity rendering, per-vertex colours (`vc`), MTL colour tints (`Kd` is read but not applied), group visibility toggles.

## Building

Requires Java 17 or newer.

```
./gradlew build     # mod jar in build/libs
./gradlew test      # parser unit tests
./gradlew runClient # dev client with the test ramp block (objmodels:obj_ramp)
```

## Licence

LGPL-2.1-only. The OBJ tokenizer, parser, material library and quad baking are adapted from [NeoForge](https://github.com/neoforged/NeoForge) 1.21.1 (Forge Development LLC and contributors, LGPL-2.1-only); those files keep their original headers and list the changes made.

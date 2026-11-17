# Adding an OBJ block model

Every OBJ model has three parts:

1. The `.obj` file (and optionally an `.mtl` file next to it).
2. A **model JSON** with `"loader": "betafront:obj"` that points at the `.obj` file and maps its textures.
3. A way to tell the game which block uses the model. This is where the two builds differ.

## 1. Make the .obj file

Any tool that exports Wavefront OBJ works. Blender and Blockbench are tested.

- **Units are blocks.** `0` to `1` on each axis fills one block. Blockbench works in pixels (16 per block), so scale its export to 1/16, or model in a 1 by 1 by 1 space in Blender.
- **Faces** can be triangles, quads or larger polygons. Larger polygons are split into a fan, so keep them convex.
- **Textures:** the simplest setup has no `.mtl` at all. Name the material `#texture0` in your modelling tool, so the file says `usemtl #texture0`, and map `texture0` in the JSON. A missing `.mtl` named by `mtllib` is ignored.

### Supported OBJ lines

| Line | Notes |
| --- | --- |
| `v`, `vt`, `vn` | Positions, texture coordinates, normals |
| `f` | Faces, including `v/vt/vn`, `v//vn` and negative indices |
| `o`, `g`, `s` | Read, but every group is always drawn |
| `usemtl` | A material from the `.mtl`, or `#name` for a JSON texture |
| `mtllib` | Relative to the `.obj` file's folder, or a full `namespace:path` |

Other lines are ignored.

### Supported MTL lines

| Line | Notes |
| --- | --- |
| `newmtl` | Starts a material |
| `map_Kd` | A texture id (`examplemod:block/lamp`) or a JSON texture (`#texture0`) |
| `Ka` | Above 0 makes the material glow (see `emissive_ambient`) |
| `betafront_TintIndex` | Block colour tint, like grass and leaves. Also accepts `neoforge_TintIndex`. StationAPI only |
| `Kd`, `d` | Read but not applied yet |

Faces with no material, or a material without `map_Kd`, use the `#default` texture, then `#particle`.

## 2. Write the model JSON

```json
{
  "loader": "betafront:obj",
  "model": "examplemod:models/block/lamp.obj",
  "flip_v": true,
  "textures": {
    "particle": "examplemod:block/lamp",
    "texture0": "examplemod:block/lamp"
  }
}
```

| Key | Default | What it does |
| --- | --- | --- |
| `loader` | required | `betafront:obj`. `neoforge:obj` and `forge:obj` work too |
| `model` | required | The `.obj` file, as `namespace:path` inside `assets/<namespace>/` |
| `textures` | none | Names to texture ids. `#texture0` in the OBJ or MTL refers to `texture0` here. Set `particle` for break particles |
| `flip_v` | `false` | Flips textures vertically. Blender and Blockbench exports usually need `true` |
| `automatic_culling` | `true` | Faces lying flat on a block side are hidden when a solid block is next to that side |
| `shade_quads` | `true` | Darkens faces by direction like vanilla blocks |
| `emissive_ambient` | `true` | Lets an MTL `Ka` above 0 make a material glow |
| `mtl_override` | none | Use this `.mtl` instead of the file's `mtllib` line |
| `ambient_occlusion` | `true` | Smooth lighting when the player has it on. On BTA, blocks that glow themselves stay flat-lit, like vanilla. On StationAPI, set it to `false` for glowing blocks and small models that sit low in the block: StationAPI blends the light of solid blocks around each vertex into faces inside the block, so they turn dark in pits and corners |
| `display` | block defaults | StationAPI only: how the item sits in the inventory, hand, on the ground and in item frames, written like a modern `display` block (`gui`, `ground`, `fixed`, `thirdperson_righthand`, `firstperson_righthand`, ...). Leave it out to match normal block items |
| `gui_light` | `side` | StationAPI only: `side` lights the inventory icon like a block, `front` like a flat item |

Texture ids work like vanilla: `examplemod:block/lamp` is the `lamp.png` texture in the block textures folder (paths per build below).

If your texture looks upside down or scrambled, try toggling `flip_v` first.

## 3a. Hook it up in a BTA mod

Files go under `assets/<modid>/`:

```
assets/examplemod/models/block/lamp.json
assets/examplemod/models/block/lamp.obj
assets/examplemod/textures/block/lamp.png
```

In the JSON, `"model"` is `"examplemod:models/block/lamp.obj"`.

Texture packs can replace any of these files by putting their own copy at the same path, for example `assets/examplemod/models/block/lamp.obj` in the pack's zip. The JSON, `.obj`, `.mtl` and textures are each looked up separately: whatever the selected packs don't have comes from the mod jar. New texture names only work if the mod jar ships a texture with that name too, because the block atlas is built from the textures in the jars. Switching packs reloads the models.

Register the model from your **client** entrypoint:

```java
import io.github.davevermeulen.betafront.bta.client.ObjModels;

public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ObjModels.block(ExampleBlocks.LAMP, "examplemod:block/lamp");
    }
}
```

`"examplemod:block/lamp"` means `assets/examplemod/models/block/lamp.json`. The block's item is drawn with the same model.

To turn the model by block metadata, pass a function from metadata to degrees (0, 90, 180 or 270, clockwise seen from above, like a blockstate `y`):

```java
ObjModels.block(ExampleBlocks.RAMP, "examplemod:block/ramp", meta -> new int[] {180, 270, 0, 90}[meta & 3]);
```

Your block's `BlockLogic` should return `false` from `isSolidRender()` and `isCubeShaped()`, otherwise the faces of neighbouring blocks disappear behind it.

## 3b. Hook it up in a StationAPI mod

Files go under `assets/<modid>/stationapi/`:

```
assets/examplemod/stationapi/blockstates/lamp.json
assets/examplemod/stationapi/models/block/lamp.json
assets/examplemod/stationapi/models/block/lamp.obj
assets/examplemod/stationapi/models/item/lamp.json
assets/examplemod/stationapi/textures/block/lamp.png
```

In the JSON, `"model"` includes the `stationapi/` folder: `"examplemod:stationapi/models/block/lamp.obj"`.

Texture packs can replace any of these files by shipping their own copy at the same path, `stationapi/` folder included. Whatever the pack doesn't have comes from the mod jar.

No code is needed. The blockstate file points at the model JSON like any other StationAPI model, and `x`/`y` rotations work as usual:

```json
{
  "variants": {
    "": { "model": "examplemod:block/lamp" }
  }
}
```

For the item, put a model JSON with the same contents in `models/item/` (named after the block). It is shown like a normal block item unless you add a `display` block; `"parent": "examplemod:block/lamp"` doesn't work, because StationAPI only allows its own JSON models as parents.

Your block should override `isOpaque()` and `isFullCube()` to return `false`.

## Troubleshooting

- **Block shows as a missing or plain model:** check the log for `Failed to load OBJ model`. The message names the file and, for parse errors, the line.
- **`Missing OBJ resource`:** the `model` or `mtllib` path is wrong. Remember the `stationapi/` folder in StationAPI paths.
- **Texture is missing or wrong:** check that every `#name` in the OBJ or MTL has an entry in the JSON `textures` map, then try toggling `flip_v`.
- **Neighbouring blocks vanish:** the block still reports itself as a solid full cube (see the overrides above).

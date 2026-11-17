# Changelog

## 1.1.0 (BTA only)

### BTA 8.0.1 (`betafront-bta-1.1.0+8.0.1.jar`)

- Texture packs can override OBJ models: the model JSON, `.obj`, `.mtl` and textures are read from the highest selected pack that has them, falling back to the mod jar. Changing packs reloads the models.

### Babric b1.7.3 with StationAPI

No new release. Texture pack overrides already work there (checked in game), since everything is read through StationAPI's resource manager; files go under `assets/<modid>/stationapi/`.

## 1.0.0

First stable release, for both builds. Both were tested in game.

### BTA 8.0.1 (`betafront-bta-1.0.0+8.0.1.jar`)

- Smooth lighting and ambient occlusion for OBJ faces when Smooth Lighting is on. Faces flat on a block side light exactly like vanilla; faces inside the block get blended values. Blocks that glow themselves stay flat-lit, like vanilla. Turn it off per model with `"ambient_occlusion": false`.
- Texture lookups are cached per model, and light and face visibility are worked out once per block side instead of once per face.
- Everything from 0.1.0-alpha.1: OBJ block models registered with `ObjModels.block(...)`, NeoForge-style model JSON, the item drawn with the block model, rotation by metadata, day/night lighting and glowing materials.
- Requires HalpLibe 6.2.0+8.0.1 or newer.

### Babric b1.7.3 with StationAPI (`betafront-stationapi-1.0.0.jar`)

First release of the StationAPI build.

- OBJ block and item models through normal StationAPI model JSONs: point a blockstate at a model JSON with `"loader": "betafront:obj"`. Blockstate `x`/`y` rotations, face culling, tint indexes (`betafront_TintIndex`) and glowing materials work.
- Item models sit in the inventory, hand and on the ground like normal block items. The model JSON's `display` and `gui_light` are read, defaulting to StationAPI's `block/block` values.
- Smooth lighting through StationAPI's renderer when Smooth Lighting is on. For glowing blocks and small models that sit low in the block, set `"ambient_occlusion": false` (see docs/adding-models.md).
- Requires StationAPI 2.0.0-alpha.6.5 or newer and Babric (Fabric Loader) 0.19.3 or newer.

### Both builds

- The example crystal and ramp blocks are only registered in the development client, not in a normal game.

Known limits:

- Models and textures load from mod jars only on BTA, not from texture packs.
- No entity or tile-entity models.
- Material colours (`Kd`) and vertex colours are not applied as tints.

## 0.1.0-alpha.1 (BTA 8.0.1)

First alpha of Betafront for Better than Adventure. Expect rough edges.

- Wavefront OBJ block models for BTA, registered from a client entrypoint with `ObjModels.block(block, "modid:block/name")`.
- Model JSON follows NeoForge's OBJ loader: `"loader": "betafront:obj"` (`neoforge:obj` and `forge:obj` also accepted), `model`, `textures`, `flip_v`, `automatic_culling`, `shade_quads`, `emissive_ambient`, `mtl_override`, and `.mtl` material libraries.
- The block's item is drawn with the same OBJ model.
- Models can be turned around the vertical axis by block metadata (0, 90, 180 or 270 degrees).
- Lighting follows sky and block light, including day/night; emissive materials light themselves.
- Requires HalpLibe 6.2.0+8.0.1 or newer.
- The example crystal and ramp blocks are only registered in the development client, not in a normal game.

Known limits:

- No per-vertex smooth lighting or ambient occlusion on OBJ faces.
- Models and textures load from mod jars only, not from texture packs.
- No entity or tile-entity models.
- Material colours (`Kd`) and vertex colours are not applied as tints.

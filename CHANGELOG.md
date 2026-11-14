# Changelog

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

# Known limitations

These don't work yet:

- **Blocks only.** No entity, tile entity or held-item-only models (block items do use the block's model).
- **No colour from materials.** `Kd` material colours and per-vertex colours (`vc`) are not applied as tints.
- **No transparency from materials.** MTL `d` and `Tr` are read but ignored; transparency comes only from the texture.
- **All groups are drawn.** `o` and `g` groups can't be shown or hidden per model, as NeoForge's `visibility` option does.
- **Convex polygons only.** Faces with more than four corners are fan-split, so concave ones come out wrong. Triangulate them in your modelling tool.

### BTA build only

- Models and textures load from mod jars only; texture packs can't replace them.
- Rotation is around the vertical axis only, in steps of 90 degrees, chosen by block metadata.
- Tint indexes (`betafront_TintIndex`) are not applied yet.

### StationAPI build only

- Rotation follows the blockstate's `x` and `y`, so it is limited to steps of 90 degrees.
- Smooth lighting comes from StationAPI's renderer, which blends the light of solid blocks around each vertex into faces inside a block. Small models that sit low in the block, and glowing ones, can turn dark in pits and corners; set `"ambient_occlusion": false` for those.

Found something else? Please open an issue at https://github.com/DaveVermeulen/betafront/issues.

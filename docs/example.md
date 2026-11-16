# Worked example

Betafront ships two test blocks, registered only in the development client:

- `betafront:example_crystal`: a crystal cluster exported from Blender, with no `.mtl` file.
- `betafront:obj_ramp`: a ramp that turns to face the player, to check rotations and culling.

Both builds use the same `.obj` files. The StationAPI paths are shown here; the BTA files are the same without the `stationapi/` folder.

## The crystal: no .mtl needed

`models/block/example_crystal.obj` was exported from Blender with the material named `#texture0`, so its faces say `usemtl #texture0`. Its `mtllib` line names a file that doesn't exist, which is fine.

`models/block/example_crystal.json`:

```json
{
  "loader": "betafront:obj",
  "model": "betafront:stationapi/models/block/example_crystal.obj",
  "flip_v": true,
  "textures": {
    "particle": "betafront:block/example_crystal",
    "texture0": "betafront:block/example_crystal"
  }
}
```

`texture0` points at `textures/block/example_crystal.png`. `flip_v` is on because Blender puts `v = 0` at the bottom of the image.

`blockstates/example_crystal.json` has a single variant:

```json
{
  "variants": {
    "": { "model": "betafront:block/example_crystal" }
  }
}
```

## The ramp: an .mtl and rotations

`models/block/obj_ramp.obj` is small enough to read in full. It is full height at the north edge and floor height at the south edge:

```
mtllib obj_ramp.mtl
o Ramp
v 0 0 0
v 1 0 0
v 1 0 1
v 0 0 1
v 0 1 0
v 1 1 0
vt 0 0
vt 1 0
vt 1 1
vt 0 1
vn 0 -1 0
vn 0 0 -1
vn 0 0.7071 0.7071
vn -1 0 0
vn 1 0 0
usemtl ramp
s off
f 1/4/1 2/3/1 3/2/1 4/1/1   # bottom
f 2/1/2 1/2/2 5/3/2 6/4/2   # back (north)
f 4/1/3 3/2/3 6/3/3 5/4/3   # slope
f 1/1/4 4/2/4 5/4/4         # west side
f 3/1/5 2/2/5 6/3/5         # east side
```

The bottom and back lie flat on the block's sides, so `automatic_culling` hides them when a solid block touches those sides.

`models/block/obj_ramp.mtl` sends the material's texture back to the JSON:

```
newmtl ramp
Kd 1 1 1
map_Kd #ramp
```

and `models/block/obj_ramp.json` maps `ramp` to a texture:

```json
{
  "loader": "betafront:obj",
  "model": "betafront:stationapi/models/block/obj_ramp.obj",
  "flip_v": true,
  "textures": {
    "particle": "betafront:block/obj_ramp",
    "ramp": "betafront:block/obj_ramp"
  }
}
```

### Rotation on StationAPI

The ramp block has a `facing` property, and the blockstate turns the model with `y`:

```json
{
  "variants": {
    "facing=north": { "model": "betafront:block/obj_ramp" },
    "facing=east":  { "model": "betafront:block/obj_ramp", "y": 90 },
    "facing=south": { "model": "betafront:block/obj_ramp", "y": 180 },
    "facing=west":  { "model": "betafront:block/obj_ramp", "y": 270 }
  }
}
```

### Rotation on BTA

The ramp stores its facing in block metadata (0 south, 1 west, 2 north, 3 east), and the client entrypoint maps it to degrees:

```java
ObjModels.block(ExampleContent.exampleCrystal, "betafront:block/example_crystal");
ObjModels.block(ExampleContent.objRamp, "betafront:block/obj_ramp", meta -> new int[] {180, 270, 0, 90}[meta & 3]);
```

The block code is in `bta/.../example/ExampleContent.java` and `stationapi/.../example/ExampleContent.java`.

## Trying it in the dev client

From the repository root:

```
./gradlew runClientBta       # Better Than Adventure
./gradlew runClientBabric    # Beta 1.7.3 with StationAPI
```

In IntelliJ, pick **Run Client (BTA)** or **Run Client (Babric)** from the run configurations.

Then get the blocks:

- **BTA:** open the creative inventory; both blocks are under Miscellaneous.
- **StationAPI:** Beta 1.7.3 has no creative mode or commands, so the dev client adds two crafting recipes. One **dirt** gives 4 example crystals, and one **cobblestone** gives 4 ramps.

Things worth checking:

- Place ramps facing each way: the high edge should always be away from you.
- Put a ramp's back against a stone block: the back face should disappear, and the stone's face behind it should stay visible.
- Watch the crystal from day into night: it should stay lit, since the block gives off light.
- Hold each block: the item should use the same model.

package io.github.davevermeulen.objmodelsbabric.obj;

/**
 * Options read from an {@code "loader": "objmodelsbabric:obj"} model JSON. Names and defaults follow
 * NeoForge's {@code neoforge:obj} loader so its model JSONs work with few edits.
 *
 * @param modelLocation     "namespace:path" of the .obj file, relative to the namespace root
 * @param automaticCulling  quads lying on a block side are hidden by a solid neighbour (default true)
 * @param shadeQuads        apply directional face shading (default true)
 * @param flipV             use 1 - v, for tools that put v = 0 at the bottom (default false)
 * @param emissiveAmbient   an MTL Ka above zero makes the mesh glow, 0 to full bright (default true)
 * @param mtlOverride       material library to use instead of the file's mtllib, or null
 * @param ambientOcclusion  use smooth lighting when the game has it on (default true; not in NeoForge)
 */
public record ObjSettings(
        String modelLocation,
        boolean automaticCulling,
        boolean shadeQuads,
        boolean flipV,
        boolean emissiveAmbient,
        String mtlOverride,
        boolean ambientOcclusion
) {
    public static ObjSettings defaults(String modelLocation) {
        return new ObjSettings(modelLocation, true, true, false, true, null, true);
    }
}

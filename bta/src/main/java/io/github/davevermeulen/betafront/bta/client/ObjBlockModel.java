package io.github.davevermeulen.betafront.bta.client;

import io.github.davevermeulen.betafront.obj.ObjGeometry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.GameSettings;
import net.minecraft.client.render.LightingCache;
import net.minecraft.client.render.block.model.BlockModelStandard;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.util.helper.LightIndexHelper;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.WorldSource;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Draws a block with an OBJ model, in the world and as an item.
 */
@Environment(EnvType.CLIENT)
public class ObjBlockModel<T extends BlockLogic> extends BlockModelStandard<T> {
    protected final ObjModelDefinition definition;
    protected final IntUnaryOperator yRotation;

    public ObjBlockModel(Block<T> block, ObjModelDefinition definition, IntUnaryOperator yRotation) {
        super(block);
        this.definition = definition;
        this.yRotation = yRotation;
    }

    @Override
    public boolean render(TessellatorGeneral tessellator, WorldSource world, TilePosc tilePos) {
        int x = tilePos.x(), y = tilePos.y(), z = tilePos.z();
        int meta = world.getBlockData(tilePos);
        // Same rule as vanilla blocks: smooth lighting only with the option on, and not for blocks
        // that give off light themselves.
        boolean smooth = definition.settings.ambientOcclusion()
                && Boolean.TRUE.equals(GameSettings.AMBIENT_OCCLUSION.value)
                && block.emission == 0;
        BlockLight light = new BlockLight(world, tilePos, meta, smooth);
        boolean drewAnything = false;
        for (ObjModelDefinition.Quad quad : definition.quads(yRotation.applyAsInt(meta))) {
            if (quad.cullSide() != null && !light.visible(quad.cullSide()))
                continue;
            IconCoordinate icon = icon(quad.texture());
            if (icon == null)
                continue;

            // BTA shades faces by direction from the vertex normal; a quad with shading turned off
            // gets an upward normal, which BTA leaves at full brightness.
            if (quad.shade())
                tessellator.setNormal(quad.normal()[0], quad.normal()[1], quad.normal()[2]);
            else
                tessellator.setNormal(0f, 1f, 0f);
            tessellator.setColorOpaque3f(1f, 1f, 1f);

            int emitted = Math.round(quad.emission() * 15f);
            float[][] pos = quad.positions();
            float[][] uv = quad.uvs();
            if (!smooth) {
                // Faces flat against the block edge take their light from the neighbour they face,
                // like vanilla full faces; everything else uses the light at the block itself.
                tessellator.setLightmapCoord1i(withEmission(light.flat(quad.cullSide()), emitted));
                // In BTA the shade is only the ambient-occlusion factor (1 = unoccluded), not world
                // brightness; putting world brightness here would bake the time of day into the chunk.
                tessellator.setShade1i(255);
            }
            for (int i = 0; i < 4; i++) {
                if (smooth) {
                    light.sample(quad.lightSide().ordinal(), pos[i]);
                    tessellator.setLightmapCoord1i(withEmission(light.lightIndex, emitted));
                    tessellator.setShade1i(emitted > 0 ? 255 : (int) (light.shade * 255f));
                }
                tessellator.addVertexWithUV(x + pos[i][0], y + pos[i][1], z + pos[i][2],
                        icon.getSubIconU(uv[i][0]), icon.getSubIconV(uv[i][1]));
            }
            drewAnything = true;
        }
        return drewAnything;
    }

    /** Emissive materials raise the block-light part only, so sky light still follows the time of day. */
    private static byte withEmission(byte lightIndex, int emitted) {
        return emitted > LightIndexHelper.blockLightFromIndex(lightIndex)
                ? LightIndexHelper.setBlockLight(lightIndex, emitted) : lightIndex;
    }

    /** Draws the model as an item (inventory, hand, dropped), the same way BlockModelStandard draws a cube. */
    @Override
    public void renderStandalone(TessellatorGeneral tessellator, int meta, byte lightIndex) {
        tessellator.offsetTranslation(-0.5, -0.5, -0.5);
        tessellator.startDrawingQuads();
        tessellator.setColor1i(getStandaloneTintColor(meta));
        for (ObjModelDefinition.Quad quad : definition.quads(yRotation.applyAsInt(meta))) {
            IconCoordinate icon = icon(quad.texture());
            if (icon == null)
                continue;
            byte quadLight = lightIndex;
            int emitted = Math.round(quad.emission() * 15f);
            if (emitted > LightIndexHelper.blockLightFromIndex(quadLight))
                quadLight = LightIndexHelper.setBlockLight(quadLight, emitted);
            tessellator.setLightmapCoord1i(quadLight);
            if (quad.shade())
                tessellator.setNormal(quad.normal()[0], quad.normal()[1], quad.normal()[2]);
            else
                tessellator.setNormal(0f, 1f, 0f);
            float[][] pos = quad.positions();
            float[][] uv = quad.uvs();
            for (int i = 0; i < 4; i++)
                tessellator.addVertexWithUV(pos[i][0], pos[i][1], pos[i][2],
                        icon.getSubIconU(uv[i][0]), icon.getSubIconV(uv[i][1]));
        }
        tessellator.draw();
        tessellator.offsetTranslation(0.5, 0.5, 0.5);
    }

    @Override
    public boolean shouldItemRender3d() {
        return true;
    }

    @Override
    public IconCoordinate getParticleTexture(Side side, int meta) {
        IconCoordinate icon = icon(definition.particleTexture);
        return icon != null ? icon : super.getParticleTexture(side, meta);
    }

    private static final AABBdc FULL_BLOCK = new AABBd(0, 0, 0, 1, 1, 1);

    /** Offset of the neighbour on each side, indexed by {@link ObjGeometry.Side}: down, up, north, south, west, east. */
    private static final int[][] DIRECTION = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    /** The axis (0 x, 1 y, 2 z) each side faces along. */
    private static final int[] NORMAL_AXIS = {1, 1, 2, 2, 0, 0};
    /** The two axes lying in each side's plane. */
    private static final int[][] PLANE_AXES = {{0, 2}, {0, 2}, {0, 1}, {0, 1}, {1, 2}, {1, 2}};
    private static final Side[] SIDES = {Side.BOTTOM, Side.TOP, Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST};

    /**
     * Light and visibility around one block, worked out lazily and at most once per side while the
     * block is drawn, instead of once per quad.
     * <p>
     * Smooth lighting copies vanilla's {@code RenderBlocks.setupLighting}: each corner of a side
     * averages the four blocks touching it (skipping ones hidden behind solid blocks) for the light
     * index, and their ambient-occlusion factors for the shade. That is done for two layers: the
     * neighbours outside the side, and the block's own layer. A vertex then blends the four corners
     * by where it sits on the side, and the two layers by how far it is from the side. So a face
     * flat on the block edge gets exactly vanilla's lighting and faces inside the block get
     * in-between values.
     */
    private final class BlockLight {
        private final WorldSource world;
        private final TilePosc pos;
        private final int meta;
        private final LightingCache cache;
        private final Boolean[] visible = new Boolean[6];
        private final byte[] flatLight = new byte[7];
        private final boolean[] flatDone = new boolean[7];
        /** [side * 2 + layer][corner * 3 + (sky, block, shade)], corner = u * 2 + v. */
        private final float[][] corners = new float[12][];

        /** Results of the last {@link #sample}. */
        byte lightIndex;
        float shade;

        BlockLight(WorldSource world, TilePosc pos, int meta, boolean smooth) {
            this.world = world;
            this.pos = pos;
            this.meta = meta;
            this.cache = smooth ? renderBlocks.cache : null;
            if (smooth)
                cache.setupCache(block, world, pos);
        }

        /** True when the neighbour on this side doesn't hide a face lying flat against it. */
        boolean visible(ObjGeometry.Side side) {
            int i = side.ordinal();
            if (visible[i] == null)
                visible[i] = shouldSideBeRendered(world, FULL_BLOCK, pos.add(SIDES[i], new TilePos()), SIDES[i], meta);
            return visible[i];
        }

        /** Flat lighting: the neighbour's light for a face on that side, else the block's own. */
        byte flat(ObjGeometry.Side side) {
            int i = side != null ? side.ordinal() : 6;
            if (!flatDone[i]) {
                flatLight[i] = block.getLightIndex(world, side != null ? pos.add(SIDES[i], new TilePos()) : pos);
                flatDone[i] = true;
            }
            return flatLight[i];
        }

        /** Smooth light index and shade for a vertex at {@code p} (block units) on a face looking towards {@code side}. */
        void sample(int side, float[] p) {
            int normalAxis = NORMAL_AXIS[side];
            int au = PLANE_AXES[side][0], av = PLANE_AXES[side][1];
            float along = clamp(p[normalAxis]);
            float depth = DIRECTION[side][normalAxis] > 0 ? 1f - along : along;
            float u = clamp(p[au]), v = clamp(p[av]);
            float w00 = (1 - u) * (1 - v), w01 = (1 - u) * v, w10 = u * (1 - v), w11 = u * v;

            float sky = 0, blockLight = 0, ao = 0;
            for (int layer = 0; layer < 2; layer++) {
                float weight = layer == 0 ? 1f - depth : depth;
                if (weight <= 0f)
                    continue;
                float[] c = corners(side, layer);
                for (int k = 0; k < 3; k++) {
                    float value = c[k] * w00 + c[3 + k] * w01 + c[6 + k] * w10 + c[9 + k] * w11;
                    if (k == 0) sky += value * weight;
                    else if (k == 1) blockLight += value * weight;
                    else ao += value * weight;
                }
            }
            lightIndex = LightIndexHelper.lightIndex2i(Math.round(sky), Math.round(blockLight));
            shade = ao;
        }

        /** Corner values of one side, for the outside layer (0) or the block's own layer (1). */
        private float[] corners(int side, int layer) {
            int slot = side * 2 + layer;
            if (corners[slot] != null)
                return corners[slot];
            int[] d = DIRECTION[side];
            int ox = layer == 0 ? d[0] : 0, oy = layer == 0 ? d[1] : 0, oz = layer == 0 ? d[2] : 0;
            int au = PLANE_AXES[side][0], av = PLANE_AXES[side][1];
            float[] c = new float[12];
            for (int u = 0; u < 2; u++) {
                for (int v = 0; v < 2; v++) {
                    int[] a = {ox, oy, oz}, b = {ox, oy, oz}, ab = {ox, oy, oz};
                    a[au] += u == 0 ? -1 : 1;
                    b[av] += v == 0 ? -1 : 1;
                    ab[au] = a[au];
                    ab[av] = b[av];
                    boolean aSolid = cache.getOpacity(a[0], a[1], a[2]);
                    boolean bSolid = cache.getOpacity(b[0], b[1], b[2]);
                    boolean abSolid = cache.getOpacity(ab[0], ab[1], ab[2]);

                    byte lc = cache.getLightIndex(ox, oy, oz);
                    byte la = aSolid ? lc : cache.getLightIndex(a[0], a[1], a[2]);
                    byte lb = bSolid ? lc : cache.getLightIndex(b[0], b[1], b[2]);
                    byte lab = aSolid && bSolid ? la : abSolid ? lc : cache.getLightIndex(ab[0], ab[1], ab[2]);
                    byte corner = LightIndexHelper.avg(lc, la, lb, lab);

                    float sc = cache.getBrightness(ox, oy, oz);
                    float sa = cache.getBrightness(a[0], a[1], a[2]);
                    float sb = cache.getBrightness(b[0], b[1], b[2]);
                    float sab = aSolid && bSolid ? sa : cache.getBrightness(ab[0], ab[1], ab[2]);

                    int i = (u * 2 + v) * 3;
                    c[i] = LightIndexHelper.skyLightFromIndex(corner);
                    c[i + 1] = LightIndexHelper.blockLightFromIndex(corner);
                    c[i + 2] = (sc + sa + sb + sab) / 4f;
                }
            }
            corners[slot] = c;
            return c;
        }

        private static float clamp(float f) {
            return f < 0f ? 0f : Math.min(f, 1f);
        }
    }

    /** Atlas entries by texture id, valid until the atlas is rebuilt (TextureRegistry.reloadGeneration). */
    private final Map<String, IconCoordinate> icons = new HashMap<>();
    private int iconsGeneration = -1;

    /** The atlas entry for a texture id such as {@code mymod:block/lamp}, falling back to the particle texture. */
    protected IconCoordinate icon(String texture) {
        if (iconsGeneration != TextureRegistry.reloadGeneration) {
            icons.clear();
            iconsGeneration = TextureRegistry.reloadGeneration;
        }
        String key = texture != null ? texture : "";
        if (icons.containsKey(key))
            return icons.get(key);
        IconCoordinate icon = lookUp(texture);
        if (icon == null && definition.particleTexture != null && !definition.particleTexture.equals(texture))
            icon = lookUp(definition.particleTexture);
        icons.put(key, icon);
        return icon;
    }

    /** TextureRegistry throws for an id that names no atlas (e.g. one without a "block/" folder). */
    private static IconCoordinate lookUp(String texture) {
        if (texture == null)
            return null;
        try {
            return TextureRegistry.getTexture(texture);
        } catch (RuntimeException e) {
            return null;
        }
    }

    protected static Side toSide(ObjGeometry.Side side) {
        if (side == null)
            return null;
        return switch (side) {
            case DOWN -> Side.BOTTOM;
            case UP -> Side.TOP;
            case NORTH -> Side.NORTH;
            case SOUTH -> Side.SOUTH;
            case WEST -> Side.WEST;
            case EAST -> Side.EAST;
        };
    }
}

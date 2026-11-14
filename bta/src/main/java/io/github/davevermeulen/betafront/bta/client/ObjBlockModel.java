package io.github.davevermeulen.betafront.bta.client;

import io.github.davevermeulen.betafront.obj.ObjGeometry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapHelper;
import net.minecraft.client.render.block.model.BlockModelStandard;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.WorldSource;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Draws a block with an OBJ model, in the world and as an item.
 */
@Environment(EnvType.CLIENT)
public class ObjBlockModel<T extends BlockLogic> extends BlockModelStandard<T> {
    /** Vanilla face shading by direction, indexed by {@link ObjGeometry.Side}: down, up, north, south, west, east. */
    private static final float[] SHADE = {0.5f, 1.0f, 0.8f, 0.8f, 0.6f, 0.6f};

    protected final ObjModelDefinition definition;
    protected final IntUnaryOperator yRotation;

    public ObjBlockModel(Block<T> block, ObjModelDefinition definition, IntUnaryOperator yRotation) {
        super(block);
        this.definition = definition;
        this.yRotation = yRotation;
    }

    @Override
    public boolean render(Tessellator tessellator, WorldSource world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        boolean drewAnything = false;
        for (ObjModelDefinition.Quad quad : definition.quads(yRotation.applyAsInt(meta))) {
            Side cull = toSide(quad.cullSide());
            if (cull != null && !isFaceVisible(world, x, y, z, cull, meta))
                continue;
            IconCoordinate icon = icon(quad.texture());
            if (icon == null)
                continue;

            // light the quad from the block it faces, like vanilla does for full faces
            Side facing = toSide(quad.lightSide());
            int lx = x, ly = y, lz = z;
            if (cull != null) {
                lx += facing.getOffsetX();
                ly += facing.getOffsetY();
                lz += facing.getOffsetZ();
            }
            float shade = quad.shade() ? SHADE[quad.lightSide().ordinal()] : 1f;
            if (LightmapHelper.isLightmapEnabled()) {
                int lightmap = block.getLightmapCoord(world, lx, ly, lz);
                if (quad.emission() > 0f)
                    lightmap = LightmapHelper.setBlocklightValue(lightmap, Math.round(quad.emission() * 15f));
                tessellator.setLightmapCoord(lightmap);
                tessellator.setColorOpaque_F(shade, shade, shade);
            } else {
                float brightness = Math.max(block.getBlockBrightness(world, lx, ly, lz), quad.emission());
                tessellator.setColorOpaque_F(brightness * shade, brightness * shade, brightness * shade);
            }

            float[][] pos = quad.positions();
            float[][] uv = quad.uvs();
            for (int i = 0; i < 4; i++)
                tessellator.addVertexWithUV(x + pos[i][0], y + pos[i][1], z + pos[i][2],
                        icon.getSubIconU(uv[i][0]), icon.getSubIconV(uv[i][1]));
            drewAnything = true;
        }
        return drewAnything;
    }

    @Override
    public void renderStandalone(Tessellator tessellator, int meta, float brightness, float alpha, Integer lightmapCoordinate) {
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f);
        tessellator.startDrawingQuads();
        if (lightmapCoordinate != null)
            tessellator.setLightmapCoord(lightmapCoordinate);
        for (ObjModelDefinition.Quad quad : definition.quads(yRotation.applyAsInt(meta))) {
            IconCoordinate icon = icon(quad.texture());
            if (icon == null)
                continue;
            float shade = (quad.shade() ? SHADE[quad.lightSide().ordinal()] : 1f) * Math.max(brightness, quad.emission());
            tessellator.setColorRGBA_F(shade, shade, shade, alpha);
            tessellator.setNormal(quad.normal()[0], quad.normal()[1], quad.normal()[2]);
            float[][] pos = quad.positions();
            float[][] uv = quad.uvs();
            for (int i = 0; i < 4; i++)
                tessellator.addVertexWithUV(pos[i][0], pos[i][1], pos[i][2],
                        icon.getSubIconU(uv[i][0]), icon.getSubIconV(uv[i][1]));
        }
        tessellator.draw();
        GL11.glTranslatef(0.5f, 0.5f, 0.5f);
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

    /** True when the neighbour on this side doesn't hide a face lying flat against it. */
    protected boolean isFaceVisible(WorldSource world, int x, int y, int z, Side side, int meta) {
        return block.getLogic().shouldSideBeRendered(world,
                x + side.getOffsetX(), y + side.getOffsetY(), z + side.getOffsetZ(), side, meta);
    }

    /** The atlas entry for a texture id such as {@code mymod:block/lamp}, falling back to the particle texture. */
    protected IconCoordinate icon(String texture) {
        IconCoordinate icon = texture != null ? TextureRegistry.getTexture(texture) : null;
        if (icon == null && definition.particleTexture != null && !definition.particleTexture.equals(texture))
            icon = TextureRegistry.getTexture(definition.particleTexture);
        return icon;
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

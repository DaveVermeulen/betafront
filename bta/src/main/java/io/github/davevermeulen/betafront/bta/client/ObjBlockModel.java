package io.github.davevermeulen.betafront.bta.client;

import io.github.davevermeulen.betafront.obj.ObjGeometry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

import java.util.List;
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
        TilePos lightPos = new TilePos();
        boolean drewAnything = false;
        for (ObjModelDefinition.Quad quad : definition.quads(yRotation.applyAsInt(meta))) {
            Side cull = toSide(quad.cullSide());
            if (cull != null && !isFaceVisible(world, tilePos, cull, meta))
                continue;
            IconCoordinate icon = icon(quad.texture());
            if (icon == null)
                continue;

            // Faces flat against the block edge take their light from the neighbour they face, like
            // vanilla full faces; everything else uses the light at the block itself.
            if (cull != null)
                tilePos.add(cull, lightPos);
            else
                lightPos.set(x, y, z);
            // The light index holds sky and block light; BTA's lightmap turns it into a brightness
            // when the chunk is drawn, so day/night applies without rebuilding the chunk.
            byte lightIndex = block.getLightIndex(world, lightPos);
            int emitted = Math.round(quad.emission() * 15f);
            if (emitted > LightIndexHelper.blockLightFromIndex(lightIndex))
                lightIndex = LightIndexHelper.setBlockLight(lightIndex, emitted);

            // BTA shades faces by direction from the vertex normal; a quad with shading turned off
            // gets an upward normal, which BTA leaves at full brightness.
            if (quad.shade())
                tessellator.setNormal(quad.normal()[0], quad.normal()[1], quad.normal()[2]);
            else
                tessellator.setNormal(0f, 1f, 0f);
            tessellator.setLightmapCoord1i(lightIndex);
            // In BTA the shade is only the ambient-occlusion factor (1 = unoccluded), not world
            // brightness; putting world brightness here would bake the time of day into the chunk.
            tessellator.setShade1i(255);
            tessellator.setColorOpaque3f(1f, 1f, 1f);

            float[][] pos = quad.positions();
            float[][] uv = quad.uvs();
            for (int i = 0; i < 4; i++)
                tessellator.addVertexWithUV(x + pos[i][0], y + pos[i][1], z + pos[i][2],
                        icon.getSubIconU(uv[i][0]), icon.getSubIconV(uv[i][1]));
            drewAnything = true;
        }
        return drewAnything;
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

    /** True when the neighbour on this side doesn't hide a face lying flat against it. */
    protected boolean isFaceVisible(WorldSource world, TilePosc tilePos, Side side, int meta) {
        return shouldSideBeRendered(world, FULL_BLOCK, tilePos.add(side, new TilePos()), side, meta);
    }

    /** The atlas entry for a texture id such as {@code mymod:block/lamp}, falling back to the particle texture. */
    protected IconCoordinate icon(String texture) {
        IconCoordinate icon = lookUp(texture);
        if (icon == null && definition.particleTexture != null && !definition.particleTexture.equals(texture))
            icon = lookUp(definition.particleTexture);
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

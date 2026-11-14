package io.github.davevermeulen.betafront.bta.client;

import io.github.davevermeulen.betafront.bta.Betafront;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Registers OBJ models for blocks. Call these from your mod's client entrypoint.
 * <pre>{@code
 * ObjModels.block(MyBlocks.LAMP, "mymod:block/lamp");
 * }</pre>
 * {@code "mymod:block/lamp"} is the model JSON at {@code assets/mymod/models/block/lamp.json}.
 * The block's item is drawn with the same model.
 */
@Environment(EnvType.CLIENT)
public final class ObjModels {
    private static final List<Entry> BLOCKS = new ArrayList<>();

    private record Entry(Block<?> block, String modelId, IntUnaryOperator yRotation) {}

    private ObjModels() {}

    /** Uses the model for the block, unrotated. */
    public static void block(Block<?> block, String modelId) {
        block(block, modelId, meta -> 0);
    }

    /**
     * Uses the model for the block, turned around the vertical axis by {@code yRotation.applyAsInt(metadata)}
     * degrees (0, 90, 180 or 270), clockwise seen from above like the {@code y} of a blockstate variant.
     */
    public static void block(Block<?> block, String modelId, IntUnaryOperator yRotation) {
        BLOCKS.add(new Entry(block, modelId, yRotation));
    }

    static void addTo(BlockModelDispatcher dispatcher) {
        for (Entry entry : BLOCKS) {
            try {
                ObjModelDefinition definition = ObjModelDefinition.load(entry.modelId());
                dispatcher.addDispatch(entry.block(), create(entry.block(), definition, entry.yRotation()));
            } catch (Exception e) {
                Betafront.LOGGER.error("Failed to load OBJ model {} for block {}", entry.modelId(), entry.block(), e);
            }
        }
    }

    private static <T extends BlockLogic> ObjBlockModel<T> create(Block<T> block, ObjModelDefinition definition, IntUnaryOperator yRotation) {
        return new ObjBlockModel<>(block, definition, yRotation);
    }
}

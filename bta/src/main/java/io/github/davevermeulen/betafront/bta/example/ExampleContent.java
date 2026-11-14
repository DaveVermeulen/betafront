package io.github.davevermeulen.betafront.bta.example;

import io.github.davevermeulen.betafront.bta.Betafront;
import io.github.davevermeulen.betafront.bta.client.ObjModels;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.material.Materials;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;
import turniplabs.halplibe.helper.BlockBuilder;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryCategory;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryPlacement;

/**
 * Test blocks for the development client (./gradlew -p bta runClient). Not registered in a normal game.
 */
public final class ExampleContent {
    // Fixed ids are fine for dev-only blocks; a real mod should use HalpLibe's id config.
    private static final int FIRST_ID = 9000;

    /** Ramp metadata is the facing index from the player's yaw: 0 south, 1 west, 2 north, 3 east. */
    private static final int[] RAMP_Y_ROTATION = {180, 270, 0, 90};

    public static Block<CrystalLogic> exampleCrystal;
    public static Block<RampLogic> objRamp;

    private ExampleContent() {}

    public static void registerBlocks() {
        BlockBuilder builder = new BlockBuilder(Betafront.MOD_ID)
                .setHardness(1.0f)
                // BTA's creative menu only lists blocks that are given a placement
                .setCreativeInventoryPlacement(new CreativeInventoryPlacement.Category(CreativeInventoryCategory.MISCELLANEOUS));
        exampleCrystal = builder.setLuminance(7).build("example_crystal", FIRST_ID, CrystalLogic::new);
        objRamp = builder.build("obj_ramp", FIRST_ID + 1, RampLogic::new);
    }

    @Environment(EnvType.CLIENT)
    public static void registerModels() {
        ObjModels.block(exampleCrystal, "betafront:block/example_crystal");
        ObjModels.block(objRamp, "betafront:block/obj_ramp", meta -> RAMP_Y_ROTATION[meta & 3]);
    }

    /** OBJ blocks aren't full cubes, so they must not hide their neighbours' faces. */
    public static class CrystalLogic extends BlockLogic {
        public CrystalLogic(Block<?> block) {
            super(block, Materials.GLASS);
        }

        @Override
        public boolean isSolidRender() {
            return false;
        }

        @Override
        public boolean isCubeShaped() {
            return false;
        }
    }

    public static class RampLogic extends BlockLogic {
        public RampLogic(Block<?> block) {
            super(block, Materials.STONE);
        }

        @Override
        public boolean isSolidRender() {
            return false;
        }

        @Override
        public boolean isCubeShaped() {
            return false;
        }

        @Override
        public void onPlacedByMob(World world, TilePosc tilePos, Side side, Mob mob, double xHit, double yHit) {
            int facing = MathHelper.floor(mob.yRot * 4.0f / 360.0f + 0.5) & 3;
            world.setBlockDataNotify(tilePos, facing);
        }
    }
}

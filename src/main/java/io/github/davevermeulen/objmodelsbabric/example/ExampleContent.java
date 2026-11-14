package io.github.davevermeulen.objmodelsbabric.example;

import io.github.davevermeulen.objmodelsbabric.ObjModelsBabric;
import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.block.BlockState;
import net.modificationstation.stationapi.api.event.registry.BlockRegistryEvent;
import net.modificationstation.stationapi.api.state.StateManager;
import net.modificationstation.stationapi.api.template.block.TemplateBlock;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.util.Namespace;
import net.modificationstation.stationapi.api.util.math.Direction;

import static net.modificationstation.stationapi.api.state.property.Properties.HORIZONTAL_FACING;

/**
 * Test blocks with OBJ models, registered only in the development environment (the runClient
 * task): a Blender-made crystal, and a ramp whose four rotations are easy to tell apart.
 * Their block items use OBJ item models too.
 */
public class ExampleContent {
    public static Block tryroliteCrystal;
    public static Block objRamp;

    @EventListener
    public void registerBlocks(BlockRegistryEvent event) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment())
            return;
        Namespace namespace = Namespace.of(ObjModelsBabric.NAMESPACE);
        tryroliteCrystal = new CrystalBlock(namespace.id("tryrolite_crystal"));
        objRamp = new RampBlock(namespace.id("obj_ramp"));
        ObjModelsBabric.LOGGER.info("Registered the ObjModelsBabric test blocks tryrolite_crystal and obj_ramp");
    }

    /** A non-cube block: neighbours stay visible through it. */
    public static class CrystalBlock extends TemplateBlock {
        public CrystalBlock(Identifier id) {
            super(id, Material.STONE);
            setHardness(0.5F);
            setLuminance(state -> 7);
            setSoundGroup(GLASS_SOUND_GROUP);
            setTranslationKey(id);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }

        @Override
        public boolean isFullCube() {
            return false;
        }
    }

    public static class RampBlock extends TemplateBlock {
        private static final Direction[] DIRECTIONS = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};

        public RampBlock(Identifier id) {
            super(id, Material.STONE);
            setHardness(1.0F);
            setTranslationKey(id);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }

        @Override
        public boolean isFullCube() {
            return false;
        }

        @Override
        public void onPlaced(World world, int x, int y, int z, LivingEntity placer) {
            Direction facing = DIRECTIONS[MathHelper.floor((double) (placer.yaw * 4.0F / 360.0F) + 0.5D) & 3];
            world.setBlockStateWithoutNotifyingNeighbors(x, y, z, getDefaultState().with(HORIZONTAL_FACING, facing));
        }

        @Override
        public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
            builder.add(HORIZONTAL_FACING);
        }
    }
}

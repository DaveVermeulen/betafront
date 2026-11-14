package io.github.davevermeulen.betafront.bta;

import io.github.davevermeulen.betafront.bta.example.ExampleContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import turniplabs.halplibe.HalpLibe;

/**
 * Betafront for Better Than Adventure: Wavefront OBJ block models.
 * <p>
 * Mods register a model for a block from their client entrypoint with
 * {@link io.github.davevermeulen.betafront.bta.client.ObjModels#block}.
 */
public final class Betafront implements ModInitializer {
    public static final String MOD_ID = HalpLibe.registerMod("betafront", true);
    public static final Logger LOGGER = LoggerFactory.getLogger("Betafront");

    /** The example blocks are only registered in the development client. */
    public static boolean exampleContentEnabled() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public void onInitialize() {
        if (exampleContentEnabled())
            ExampleContent.registerBlocks();
    }
}

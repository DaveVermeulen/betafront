package io.github.davevermeulen.betafront.bta.client;

import io.github.davevermeulen.betafront.bta.Betafront;
import io.github.davevermeulen.betafront.bta.example.ExampleContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import turniplabs.halplibe.event.defs.ClientEvents;
import turniplabs.halplibe.util.dependency.Key;

@Environment(EnvType.CLIENT)
public final class BetafrontClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // HalpLibe fires this whenever the block model dispatcher is (re)built
        ClientEvents.BLOCK_MODEL_RELOAD.listen(Key.of(Betafront.MOD_ID), ObjModels::addTo);

        if (Betafront.exampleContentEnabled())
            ExampleContent.registerModels();
    }
}

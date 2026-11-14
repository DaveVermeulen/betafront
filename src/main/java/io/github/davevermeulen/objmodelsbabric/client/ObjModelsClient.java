package io.github.davevermeulen.objmodelsbabric.client;

import com.google.gson.JsonObject;
import io.github.davevermeulen.objmodelsbabric.ObjModelsBabric;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.client.event.render.model.PreLoadUnbakedModelEvent;
import net.modificationstation.stationapi.api.util.Identifier;

import java.io.IOException;

/**
 * Hooks the OBJ loader into StationAPI: for every model StationAPI is about to load, a model JSON
 * with {@code "loader": "objmodelsbabric:obj"} is loaded by us instead of by the JSON loader.
 */
@Environment(EnvType.CLIENT)
public class ObjModelsClient {
    @EventListener
    public void routeObjModels(PreLoadUnbakedModelEvent event) {
        Identifier modelId = event.identifier;
        JsonObject json;
        try {
            json = ObjModelLoader.findObjModelJson(modelId);
        } catch (IOException e) {
            ObjModelsBabric.LOGGER.warn("Could not read model JSON {}", modelId, e);
            return;
        }
        if (json == null)
            return;

        event.loader = id -> {
            try {
                return ObjModelLoader.load(id, json);
            } catch (IOException | RuntimeException e) {
                ObjModelsBabric.LOGGER.error("Failed to load OBJ model {}: {}", id, e.getMessage());
                throw e instanceof IOException io ? io : new IOException(e);
            }
        };
    }
}

package io.github.davevermeulen.betafront.stationapi.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.davevermeulen.betafront.stationapi.Betafront;
import io.github.davevermeulen.betafront.obj.ObjGeometry;
import io.github.davevermeulen.betafront.obj.ObjMaterialLibrary;
import io.github.davevermeulen.betafront.obj.ObjSettings;
import io.github.davevermeulen.betafront.obj.ObjTokenizer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.modificationstation.stationapi.api.client.render.model.ModelIdentifier;
import net.modificationstation.stationapi.api.client.render.model.ModelLoader;
import net.modificationstation.stationapi.api.client.resource.ReloadableAssetsManager;
import net.modificationstation.stationapi.api.resource.Resource;
import net.modificationstation.stationapi.api.resource.ResourceManager;
import net.modificationstation.stationapi.api.util.Identifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads model JSONs that ask for the OBJ loader and builds {@link ObjUnbakedModel}s from them.
 */
@Environment(EnvType.CLIENT)
public final class ObjModelLoader {
    /** Loader ids accepted in the model JSON's "loader" key. */
    public static final Set<String> LOADER_IDS = Set.of(Betafront.NAMESPACE + ":obj", "neoforge:obj", "forge:obj");

    private ObjModelLoader() {}

    private static ResourceManager resources() {
        return ReloadableAssetsManager.INSTANCE;
    }

    /**
     * @return the model JSON for this id when its "loader" is an OBJ loader id, otherwise null
     */
    public static JsonObject findObjModelJson(Identifier modelId) throws IOException {
        Identifier id = ModelIdentifier.of(modelId.toString()).id;
        if (id.path.startsWith("builtin/"))
            return null;

        Optional<Resource> resource = resources().getResource(ModelLoader.MODELS_FINDER.toResourcePath(id));
        if (resource.isEmpty())
            return null;

        JsonObject json;
        try (Reader reader = resource.get().getReader()) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject())
                return null;
            json = element.getAsJsonObject();
        } catch (JsonParseException e) {
            return null; // not ours to report; StationAPI logs broken JSON itself
        }

        if (!json.has("loader"))
            return null;
        JsonElement loader = json.get("loader");
        String loaderId = loader.isJsonObject() ? loader.getAsJsonObject().get("id").getAsString() : loader.getAsString();
        return LOADER_IDS.contains(loaderId) ? json : null;
    }

    public static ObjUnbakedModel load(Identifier modelId, JsonObject json) throws IOException {
        if (!json.has("model"))
            throw new JsonParseException("OBJ model " + modelId + " needs a \"model\" key that points to a .obj file");

        ObjSettings settings = new ObjSettings(
                json.get("model").getAsString(),
                getBoolean(json, "automatic_culling", true),
                getBoolean(json, "shade_quads", true),
                getBoolean(json, "flip_v", false),
                getBoolean(json, "emissive_ambient", true),
                json.has("mtl_override") ? json.get("mtl_override").getAsString() : null,
                getBoolean(json, "ambient_occlusion", true)
        );

        Map<String, String> textures = new HashMap<>();
        if (json.has("textures"))
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet())
                textures.put(entry.getKey(), entry.getValue().getAsString());

        ObjGeometry geometry;
        try (ObjTokenizer tokenizer = new ObjTokenizer(open(settings.modelLocation()).getInputStream())) {
            geometry = ObjGeometry.parse(tokenizer, settings, ObjModelLoader::loadMaterialLibrary);
        }
        return new ObjUnbakedModel(modelId, geometry, textures);
    }

    private static ObjMaterialLibrary loadMaterialLibrary(String location) throws IOException {
        try (ObjTokenizer tokenizer = new ObjTokenizer(open(location).getInputStream())) {
            return new ObjMaterialLibrary(tokenizer);
        }
    }

    private static Resource open(String location) throws IOException {
        Identifier id = Identifier.tryParse(location);
        if (id == null)
            throw new FileNotFoundException("Invalid resource location: " + location);
        return resources().getResource(id).orElseThrow(() -> new FileNotFoundException("Missing OBJ resource: " + location));
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }
}

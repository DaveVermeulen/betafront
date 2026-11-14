package io.github.davevermeulen.betafront.bta.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.davevermeulen.betafront.obj.ObjGeometry;
import io.github.davevermeulen.betafront.obj.ObjMaterialLibrary;
import io.github.davevermeulen.betafront.obj.ObjSettings;
import io.github.davevermeulen.betafront.obj.ObjTokenizer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A model JSON with {@code "loader": "betafront:obj"}, read into quads ready to draw.
 * The JSON is the same as for the StationAPI build and NeoForge's OBJ loader.
 */
@Environment(EnvType.CLIENT)
public final class ObjModelDefinition {
    public static final Set<String> LOADER_IDS = Set.of("betafront:obj", "neoforge:obj", "forge:obj");
    private static final int MAX_REFERENCE_DEPTH = 8;

    /**
     * One quad of the model. Positions are block units (0 to 1), uvs run 0 to 1 across the texture.
     *
     * @param cullSide the block side this quad lies flat on, or null
     * @param texture  texture id such as {@code mymod:block/lamp}, or null when it could not be resolved
     */
    public record Quad(float[][] positions, float[][] uvs, float[] normal, ObjGeometry.Side cullSide,
                       ObjGeometry.Side lightSide, String texture, float emission, boolean shade) {}

    public final String id;
    public final ObjSettings settings;
    public final String particleTexture;
    private final List<Quad> quads;
    private final List<Quad>[] rotated;

    @SuppressWarnings("unchecked")
    private ObjModelDefinition(String id, ObjSettings settings, String particleTexture, List<Quad> quads) {
        this.id = id;
        this.settings = settings;
        this.particleTexture = particleTexture;
        this.quads = quads;
        this.rotated = new List[4];
        this.rotated[0] = quads;
    }

    /** The quads turned by 0, 90, 180 or 270 degrees around the block's vertical centre line. */
    public List<Quad> quads(int yRotation) {
        int steps = Math.floorMod(Math.round(yRotation / 90f), 4);
        List<Quad> result = rotated[steps];
        if (result == null) {
            result = new ArrayList<>(quads.size());
            for (Quad quad : quads)
                result.add(rotate(quad, steps));
            rotated[steps] = result;
        }
        return result;
    }

    /**
     * Loads {@code assets/<namespace>/models/<path>.json} for a model id such as {@code mymod:block/lamp}.
     */
    public static ObjModelDefinition load(String modelId) throws IOException {
        JsonObject json;
        try (Reader reader = new InputStreamReader(open(modelPath(modelId)), StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject())
                throw new JsonParseException("Model " + modelId + " is not a JSON object");
            json = element.getAsJsonObject();
        }

        if (json.has("loader")) {
            JsonElement loader = json.get("loader");
            String loaderId = loader.isJsonObject() ? loader.getAsJsonObject().get("id").getAsString() : loader.getAsString();
            if (!LOADER_IDS.contains(loaderId))
                throw new JsonParseException("Model " + modelId + " uses loader " + loaderId + ", expected betafront:obj");
        }
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
        try (ObjTokenizer tokenizer = new ObjTokenizer(open(settings.modelLocation()))) {
            geometry = ObjGeometry.parse(tokenizer, settings, ObjModelDefinition::loadMaterialLibrary);
        }

        List<Quad> quads = new ArrayList<>();
        for (ObjGeometry.ModelMesh mesh : geometry.allMeshes()) {
            ObjMaterialLibrary.Material mat = mesh.mat;
            String texture = resolve(textures, mat != null && mat.diffuseColorMap != null ? mat.diffuseColorMap : "#default");
            float emission = settings.emissiveAmbient() && mat != null ? mat.ambientLevel() : 0f;
            boolean shade = settings.shadeQuads() && emission == 0f;
            for (ObjGeometry.ObjQuad quad : geometry.buildQuads(mesh)) {
                float[] normal = quad.normals()[0];
                ObjGeometry.Side cull = settings.automaticCulling() ? ObjGeometry.cullSide(quad.positions(), normal) : null;
                quads.add(new Quad(quad.positions(), quad.uvs(), normal, cull, ObjGeometry.nearestSide(normal), texture, emission, shade));
            }
        }
        return new ObjModelDefinition(modelId, settings, resolve(textures, "#particle"), quads);
    }

    private static ObjMaterialLibrary loadMaterialLibrary(String location) throws IOException {
        try (ObjTokenizer tokenizer = new ObjTokenizer(open(location))) {
            return new ObjMaterialLibrary(tokenizer);
        }
    }

    /** {@code mymod:block/lamp} to {@code mymod:models/block/lamp.json}. */
    private static String modelPath(String modelId) {
        int colon = modelId.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : modelId.substring(0, colon);
        String path = modelId.substring(colon + 1);
        if (!path.endsWith(".json"))
            path = "models/" + path + ".json";
        return namespace + ":" + path;
    }

    /** Opens {@code namespace:path} as {@code assets/namespace/path} on the classpath. */
    private static InputStream open(String location) throws IOException {
        int colon = location.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : location.substring(0, colon);
        String path = location.substring(colon + 1);
        InputStream in = ObjModelDefinition.class.getResourceAsStream("/assets/" + namespace + "/" + path);
        if (in == null)
            throw new FileNotFoundException("Missing OBJ resource: " + location);
        return in;
    }

    /**
     * "#name" follows the JSON textures map (a missing "#default" falls back to "#particle");
     * anything else is a texture id. Returns null when the reference can't be resolved.
     */
    private static String resolve(Map<String, String> textures, String texture) {
        String name = texture;
        for (int depth = 0; depth < MAX_REFERENCE_DEPTH && name.startsWith("#"); depth++) {
            String key = name.substring(1);
            String next = textures.get(key);
            if (next == null && key.equals("default"))
                next = "#particle";
            if (next == null)
                return null;
            name = next;
        }
        return name.startsWith("#") ? null : name;
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    /** Turns a quad by {@code steps} quarter turns clockwise seen from above: north goes to east. */
    private static Quad rotate(Quad quad, int steps) {
        float[][] pos = new float[4][];
        for (int i = 0; i < 4; i++) {
            float x = quad.positions()[i][0], y = quad.positions()[i][1], z = quad.positions()[i][2];
            for (int s = 0; s < steps; s++) {
                float nx = 1f - z;
                z = x;
                x = nx;
            }
            pos[i] = new float[] {x, y, z};
        }
        float nx = quad.normal()[0], ny = quad.normal()[1], nz = quad.normal()[2];
        for (int s = 0; s < steps; s++) {
            float t = -nz;
            nz = nx;
            nx = t;
        }
        float[] normal = {nx, ny, nz};
        ObjGeometry.Side cull = quad.cullSide() == null ? null : ObjGeometry.cullSide(pos, normal);
        return new Quad(pos, quad.uvs(), normal, cull, ObjGeometry.nearestSide(normal), quad.texture(), quad.emission(), quad.shade());
    }
}

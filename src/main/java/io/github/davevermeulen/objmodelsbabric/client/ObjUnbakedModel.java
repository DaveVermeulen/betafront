/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 *
 * Baking adapted from NeoForge 1.21.1 (net.neoforged.neoforge.client.model.obj.ObjModel#makeQuad
 * and ModelMesh#addQuads) for StationAPI's BakedQuad format on Babric b1.7.3. Changes: vertex data
 * is StationAPI's 8-int layout (position + sprite UV), the rotation comes from StationAPI's
 * ModelBakeSettings, emissive_ambient maps to BakedQuad emission, and meshes without a material
 * use the model's "#default" or "#particle" texture.
 */

package io.github.davevermeulen.objmodelsbabric.client;

import com.mojang.datafixers.util.Pair;
import io.github.davevermeulen.objmodelsbabric.obj.ObjGeometry;
import io.github.davevermeulen.objmodelsbabric.obj.ObjMaterialLibrary;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.modificationstation.stationapi.api.client.render.model.BakedModel;
import net.modificationstation.stationapi.api.client.render.model.BakedQuad;
import net.modificationstation.stationapi.api.client.render.model.BakedQuadFactory;
import net.modificationstation.stationapi.api.client.render.model.Baker;
import net.modificationstation.stationapi.api.client.render.model.BasicBakedModel;
import net.modificationstation.stationapi.api.client.render.model.ModelBakeSettings;
import net.modificationstation.stationapi.api.client.render.model.UnbakedModel;
import net.modificationstation.stationapi.api.client.render.model.json.ModelOverrideList;
import net.modificationstation.stationapi.api.client.render.model.json.ModelTransformation;
import net.modificationstation.stationapi.api.client.texture.MissingSprite;
import net.modificationstation.stationapi.api.client.texture.Sprite;
import net.modificationstation.stationapi.api.client.texture.SpriteIdentifier;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlases;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.util.math.AffineTransformation;
import net.modificationstation.stationapi.api.util.math.Direction;
import net.modificationstation.stationapi.api.util.math.Matrix4f;
import net.modificationstation.stationapi.api.util.math.Vec3f;
import net.modificationstation.stationapi.api.util.math.Vec4f;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * An OBJ model waiting for its textures; {@link #bake} turns it into StationAPI baked quads.
 */
@Environment(EnvType.CLIENT)
public class ObjUnbakedModel implements UnbakedModel {
    private static final BakedQuadFactory QUAD_FACTORY = new BakedQuadFactory();
    private static final int MAX_REFERENCE_DEPTH = 8;

    private final Identifier id;
    private final ObjGeometry geometry;
    /** The JSON "textures" map: name to texture id or "#other". */
    private final Map<String, String> textures;

    public ObjUnbakedModel(Identifier id, ObjGeometry geometry, Map<String, String> textures) {
        this.id = id;
        this.geometry = geometry;
        this.textures = Map.copyOf(textures);
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        return List.of();
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> parents) {
    }

    @Override
    public Collection<SpriteIdentifier> getTextures(Function<Identifier, UnbakedModel> modelLoader, Set<Pair<String, String>> unresolvedTextureReferences) {
        Set<SpriteIdentifier> sprites = new HashSet<>();
        sprites.add(resolve("#particle", unresolvedTextureReferences));
        for (ObjGeometry.ModelMesh mesh : geometry.allMeshes())
            sprites.add(resolve(textureOf(mesh), unresolvedTextureReferences));
        return sprites;
    }

    @Override
    public BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings settings, Identifier modelId) {
        var options = geometry.settings;
        var builder = new BasicBakedModel.Builder(options.ambientOcclusion(), true, false, ModelTransformation.NONE, ModelOverrideList.EMPTY)
                .setParticle(textureGetter.apply(resolve("#particle", null)));

        AffineTransformation rotation = settings.getRotation();
        boolean rotated = !rotation.equals(AffineTransformation.identity());
        Matrix4f matrix = rotated ? rotation.getMatrix() : null;

        for (ObjGeometry.ModelMesh mesh : geometry.allMeshes()) {
            Sprite sprite = textureGetter.apply(resolve(textureOf(mesh), null));
            ObjMaterialLibrary.Material mat = mesh.mat;
            int tintIndex = mat != null ? mat.diffuseTintIndex : -1;
            float emission = options.emissiveAmbient() && mat != null ? mat.ambientLevel() : 0f;
            boolean shade = options.shadeQuads() && emission == 0f;

            for (ObjGeometry.ObjQuad quad : geometry.buildQuads(mesh)) {
                float[][] pos = quad.positions();
                float[][] norm = quad.normals();
                if (rotated) {
                    for (int i = 0; i < 4; i++) {
                        Vec3f p = new Vec3f(pos[i][0], pos[i][1], pos[i][2]);
                        QUAD_FACTORY.transformVertex(p, rotation);
                        pos[i] = new float[] {p.getX(), p.getY(), p.getZ()};

                        Vec4f n = new Vec4f(norm[i][0], norm[i][1], norm[i][2], 0f);
                        n.transform(matrix);
                        norm[i] = new float[] {n.getX(), n.getY(), n.getZ()};
                    }
                }

                int[] vertexData = new int[32];
                for (int i = 0; i < 4; i++) {
                    int o = i * 8;
                    vertexData[o] = Float.floatToRawIntBits(pos[i][0]);
                    vertexData[o + 1] = Float.floatToRawIntBits(pos[i][1]);
                    vertexData[o + 2] = Float.floatToRawIntBits(pos[i][2]);
                    // StationAPI sprite coordinates run 0 to 16 across the sprite
                    vertexData[o + 3] = Float.floatToRawIntBits(sprite.getFrameU(quad.uvs()[i][0] * 16.0));
                    vertexData[o + 4] = Float.floatToRawIntBits(sprite.getFrameV(quad.uvs()[i][1] * 16.0));
                }

                Direction face = toDirection(ObjGeometry.nearestSide(norm[0]));
                BakedQuad bakedQuad = new BakedQuad(vertexData, tintIndex, face, sprite, shade, emission);

                ObjGeometry.Side cull = options.automaticCulling() ? ObjGeometry.cullSide(pos, norm[0]) : null;
                if (cull == null)
                    builder.addQuad(bakedQuad);
                else
                    builder.addQuad(toDirection(cull), bakedQuad);
            }
        }
        return builder.build();
    }

    private static Direction toDirection(ObjGeometry.Side side) {
        return Direction.valueOf(side.name());
    }

    private static String textureOf(ObjGeometry.ModelMesh mesh) {
        if (mesh.mat != null && mesh.mat.diffuseColorMap != null)
            return mesh.mat.diffuseColorMap;
        return "#default";
    }

    /**
     * "#name" follows the JSON textures map (a missing "#default" falls back to "#particle");
     * anything else is a texture id in the game atlas, as in StationAPI JSON models.
     */
    private SpriteIdentifier resolve(String texture, Set<Pair<String, String>> unresolved) {
        String name = texture;
        for (int depth = 0; depth < MAX_REFERENCE_DEPTH && name.startsWith("#"); depth++) {
            String key = name.substring(1);
            String next = textures.get(key);
            if (next == null && key.equals("default"))
                next = "#particle";
            if (next == null) {
                if (unresolved != null)
                    unresolved.add(Pair.of(texture, id.toString()));
                return missing();
            }
            name = next;
        }
        if (name.startsWith("#"))
            return missing();

        Identifier textureId = Identifier.tryParse(name);
        if (textureId == null) {
            if (unresolved != null)
                unresolved.add(Pair.of(texture, id.toString()));
            return missing();
        }
        return SpriteIdentifier.of(Atlases.GAME_ATLAS_TEXTURE, textureId);
    }

    private static SpriteIdentifier missing() {
        return SpriteIdentifier.of(Atlases.GAME_ATLAS_TEXTURE, MissingSprite.getMissingSpriteId());
    }
}

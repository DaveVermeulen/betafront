/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 *
 * Ported from NeoForge 1.21.1 (net.neoforged.neoforge.client.model.obj.ObjMaterialLibrary)
 * for ObjModelsBabric on Babric b1.7.3. Changes: JOML vectors replaced by float[4], Guava removed,
 * "objmodelsbabric_TintIndex" accepted next to the Forge/NeoForge spellings.
 */

package io.github.davevermeulen.objmodelsbabric.obj;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An OBJ material library (MTL), composed of named {@link Material materials}.
 */
public class ObjMaterialLibrary {
    public static final ObjMaterialLibrary EMPTY = new ObjMaterialLibrary();
    final Map<String, Material> materials = new HashMap<>();

    private ObjMaterialLibrary() {}

    public ObjMaterialLibrary(ObjTokenizer reader) throws IOException {
        Material currentMaterial = null;

        String[] line;
        while ((line = reader.readAndSplitLine(true)) != null) {
            if (!line[0].equals("newmtl") && currentMaterial == null)
                continue; // statements before the first newmtl have nothing to apply to

            switch (line[0]) {
                case "newmtl": {
                    String name = String.join(" ", Arrays.copyOfRange(line, 1, line.length));
                    currentMaterial = new Material(name);
                    materials.put(name, currentMaterial);
                    break;
                }

                case "Ka":
                    currentMaterial.ambientColor = ObjGeometry.parseVector4(line);
                    break;

                case "map_Ka":
                    // Ignores all options params
                    currentMaterial.ambientColorMap = line[line.length - 1];
                    break;

                case "Kd":
                    currentMaterial.diffuseColor = ObjGeometry.parseVector4(line);
                    break;

                case "forge_TintIndex":
                case "neoforge_TintIndex":
                case "objmodelsbabric_TintIndex":
                    currentMaterial.diffuseTintIndex = Integer.parseInt(line[1]);
                    break;

                case "map_Kd":
                    // Ignores all options params
                    currentMaterial.diffuseColorMap = line[line.length - 1];
                    break;

                case "Ks":
                    currentMaterial.specularColor = ObjGeometry.parseVector4(line);
                    break;

                case "Ns":
                    currentMaterial.specularHighlight = Float.parseFloat(line[1]);
                    break;

                case "map_Ks":
                    // Ignores all options params
                    currentMaterial.specularColorMap = line[line.length - 1];
                    break;

                case "d":
                    // Ignores all options params
                    currentMaterial.dissolve = Float.parseFloat(line[line.length - 1]);
                    break;

                case "Tr":
                    // Ignores all options params
                    currentMaterial.transparency = Float.parseFloat(line[line.length - 1]);
                    break;
            }
        }
    }

    public boolean hasMaterial(String mat) {
        return materials.containsKey(mat);
    }

    public Material getMaterial(String mat) {
        Material material = materials.get(mat);
        if (material == null)
            throw new NoSuchElementException("The material was not found in the library: " + mat);
        return material;
    }

    public static class Material {
        public final String name;
        /** RGBA, as in the file. */
        public float[] ambientColor = {0, 0, 0, 1};
        public String ambientColorMap;
        public float[] diffuseColor = {1, 1, 1, 1};
        public String diffuseColorMap;
        public float[] specularColor = {0, 0, 0, 1};
        public float specularHighlight = 0;
        public String specularColorMap;

        public float dissolve = 1.0f;
        public float transparency = 0.0f;

        // non-standard
        public int diffuseTintIndex = -1;

        public Material(String name) {
            this.name = name;
        }

        /** Average of the ambient RGB, 0 to 1; used for {@code emissive_ambient}. */
        public float ambientLevel() {
            float level = (ambientColor[0] + ambientColor[1] + ambientColor[2]) / 3f;
            return Math.max(0f, Math.min(1f, level));
        }
    }
}

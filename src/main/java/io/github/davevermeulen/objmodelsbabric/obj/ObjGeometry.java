/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 *
 * Ported from NeoForge 1.21.1 (net.neoforged.neoforge.client.model.obj.ObjModel), parse half
 * and the vertex half of makeQuad, for ObjModelsBabric on Babric b1.7.3. Changes: no Minecraft or
 * JOML types so it can be unit tested; faces with more than four vertices are fan-split into
 * several quads instead of being cut to the first four; meshes without a material are kept
 * (they use the model's default texture) instead of being dropped; usemtl with an unknown
 * material falls back to no material instead of throwing, "usemtl #name" without a matching
 * material uses the JSON texture "name", and a missing mtllib file is treated as an empty library.
 */

package io.github.davevermeulen.objmodelsbabric.obj;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Geometry parsed from an OBJ file: vertex pools plus a tree of named groups and objects,
 * each holding meshes (faces sharing one material and smoothing group).
 */
public class ObjGeometry {
    private static final float[][] DEFAULT_COORDS = {
            {0, 0},
            {0, 1},
            {1, 1},
            {1, 0},
    };
    private static final float EPSILON = 1.0E-5F;

    /** Loads a material library from a resolved "namespace:path" location. */
    @FunctionalInterface
    public interface MaterialLibraryLoader {
        ObjMaterialLibrary load(String location) throws IOException;
    }

    /** One of the six block sides, named as in StationAPI's {@code Direction}. */
    public enum Side { DOWN, UP, NORTH, SOUTH, WEST, EAST }

    public final ObjSettings settings;
    final Map<String, ModelGroup> parts = new LinkedHashMap<>();

    final List<float[]> positions = new ArrayList<>();
    final List<float[]> texCoords = new ArrayList<>();
    final List<float[]> normals = new ArrayList<>();
    final List<float[]> colors = new ArrayList<>();
    private final Map<String, ObjMaterialLibrary.Material> textureMaterials = new LinkedHashMap<>();

    private ObjGeometry(ObjSettings settings) {
        this.settings = settings;
    }

    public static ObjGeometry parse(ObjTokenizer tokenizer, ObjSettings settings, MaterialLibraryLoader materials) throws IOException {
        var model = new ObjGeometry(settings);

        // for relative references to material libraries
        String modelLocation = settings.modelLocation();
        int colon = modelLocation.indexOf(':');
        String modelDomain = colon >= 0 ? modelLocation.substring(0, colon) : "minecraft";
        String modelPath = colon >= 0 ? modelLocation.substring(colon + 1) : modelLocation;
        int lastSlash = modelPath.lastIndexOf('/');
        modelPath = lastSlash >= 0 ? modelPath.substring(0, lastSlash + 1) : ""; // include the '/'

        ObjMaterialLibrary mtllib = ObjMaterialLibrary.EMPTY;
        ObjMaterialLibrary.Material currentMat = null;
        String currentSmoothingGroup = null;
        ModelGroup currentGroup = null;
        ModelObject currentObject = null;
        ModelMesh currentMesh = null;

        boolean objAboveGroup = false;

        if (settings.mtlOverride() != null)
            mtllib = materials.load(resolve(settings.mtlOverride(), modelDomain, modelPath));

        String[] line;
        while ((line = tokenizer.readAndSplitLine(true)) != null) {
            try {
                switch (line[0]) {
                    case "mtllib": // Loads material library
                    {
                        if (settings.mtlOverride() != null)
                            break;

                        String lib = String.join(" ", Arrays.copyOfRange(line, 1, line.length));
                        try {
                            mtllib = materials.load(resolve(lib, modelDomain, modelPath));
                        } catch (FileNotFoundException e) {
                            // Exporters often write an mtllib line even when no .mtl is shipped;
                            // "usemtl #texture" lines still work without one.
                            mtllib = ObjMaterialLibrary.EMPTY;
                        }
                        break;
                    }

                    case "usemtl": // Sets the current material (starts new mesh)
                    {
                        String mat = String.join(" ", Arrays.copyOfRange(line, 1, line.length));
                        ObjMaterialLibrary.Material newMat = mtllib.hasMaterial(mat) ? mtllib.getMaterial(mat) : textureMaterial(model, mat);
                        if (!Objects.equals(newMat, currentMat)) {
                            currentMat = newMat;
                            if (currentMesh != null && currentMesh.mat == null && currentMesh.faces.isEmpty()) {
                                currentMesh.mat = currentMat;
                            } else {
                                // Start new mesh
                                currentMesh = null;
                            }
                        }
                        break;
                    }

                    case "v": // Vertex
                        model.positions.add(parseVector4To3(line));
                        break;
                    case "vt": // Vertex texcoord
                        model.texCoords.add(parseVector2(line));
                        break;
                    case "vn": // Vertex normal
                        model.normals.add(parseVector3(line));
                        break;
                    case "vc": // Vertex color (non-standard)
                        model.colors.add(parseVector4(line));
                        break;

                    case "f": // Face
                    {
                        if (line.length < 4)
                            throw new IOException("a face needs at least 3 vertices");

                        if (currentMesh == null) {
                            currentMesh = new ModelMesh(currentMat, currentSmoothingGroup);
                            if (currentObject != null) {
                                currentObject.meshes.add(currentMesh);
                            } else {
                                if (currentGroup == null) {
                                    currentGroup = new ModelGroup("");
                                    model.parts.put("", currentGroup);
                                }
                                currentGroup.meshes.add(currentMesh);
                            }
                        }

                        int[][] vertices = new int[line.length - 1][];
                        for (int i = 0; i < vertices.length; i++) {
                            String[] vertexParts = line[i + 1].split("/", -1);
                            int[] vertex = new int[vertexParts.length];
                            for (int k = 0; k < vertexParts.length; k++)
                                vertex[k] = vertexParts[k].isEmpty() ? 0 : Integer.parseInt(vertexParts[k]);

                            vertex[0] = toIndex(vertex[0], model.positions.size());
                            if (vertex.length > 1)
                                vertex[1] = vertexParts[1].isEmpty() ? -1 : toIndex(vertex[1], model.texCoords.size());
                            if (vertex.length > 2)
                                vertex[2] = vertexParts[2].isEmpty() ? -1 : toIndex(vertex[2], model.normals.size());
                            if (vertex.length > 3)
                                vertex[3] = vertexParts[3].isEmpty() ? -1 : toIndex(vertex[3], model.colors.size());

                            vertices[i] = vertex;
                        }

                        currentMesh.faces.add(vertices);
                        break;
                    }

                    case "s": // Smoothing group (starts new mesh)
                    {
                        String smoothingGroup = line.length < 2 || "off".equals(line[1]) || "0".equals(line[1]) ? null : line[1];
                        if (!Objects.equals(currentSmoothingGroup, smoothingGroup)) {
                            currentSmoothingGroup = smoothingGroup;
                            if (currentMesh != null && currentMesh.smoothingGroup == null && currentMesh.faces.isEmpty()) {
                                currentMesh.smoothingGroup = currentSmoothingGroup;
                            } else {
                                // Start new mesh
                                currentMesh = null;
                            }
                        }
                        break;
                    }

                    case "g": {
                        String name = line.length > 1 ? line[1] : "";
                        if (objAboveGroup && currentGroup != null) {
                            currentObject = new ModelObject(currentGroup.name() + "/" + name);
                            currentGroup.parts.put(name, currentObject);
                        } else {
                            currentGroup = new ModelGroup(name);
                            model.parts.put(name, currentGroup);
                            currentObject = null;
                        }
                        // Start new mesh
                        currentMesh = null;
                        break;
                    }

                    case "o": {
                        String name = line.length > 1 ? line[1] : "";
                        if (objAboveGroup || currentGroup == null) {
                            objAboveGroup = true;

                            currentGroup = new ModelGroup(name);
                            model.parts.put(name, currentGroup);
                            currentObject = null;
                        } else {
                            currentObject = new ModelObject(currentGroup.name() + "/" + name);
                            currentGroup.parts.put(name, currentObject);
                        }
                        // Start new mesh
                        currentMesh = null;
                        break;
                    }
                }
            } catch (IOException | RuntimeException e) {
                throw new IOException(settings.modelLocation() + " line " + tokenizer.getLineNumber() + ": " + e.getMessage(), e);
            }
        }
        return model;
    }

    /**
     * "usemtl #texture0" with no such material in the library means "use the JSON texture texture0",
     * as Blockbench and NeoForge models commonly do. One shared material per name.
     */
    private static ObjMaterialLibrary.Material textureMaterial(ObjGeometry model, String name) {
        if (!name.startsWith("#"))
            return null;
        return model.textureMaterials.computeIfAbsent(name, n -> {
            var material = new ObjMaterialLibrary.Material(n);
            material.diffuseColorMap = n;
            return material;
        });
    }

    private static int toIndex(int objIndex, int poolSize) {
        int index = objIndex < 0 ? poolSize + objIndex : objIndex - 1;
        if (index < 0 || index >= poolSize)
            throw new IndexOutOfBoundsException("index " + objIndex + " is outside the " + poolSize + " entries defined so far");
        return index;
    }

    /** "lib.mtl" becomes "ns:dir/lib.mtl"; "other:path.mtl" is kept as is. */
    static String resolve(String location, String modelDomain, String modelPath) {
        return location.contains(":") ? location : modelDomain + ":" + modelPath + location;
    }

    private static float[] parseVector4To3(String[] line) {
        float[] vec4 = parseVector4(line);
        return new float[] {vec4[0] / vec4[3], vec4[1] / vec4[3], vec4[2] / vec4[3]};
    }

    private static float[] parseVector2(String[] line) {
        return switch (line.length) {
            case 1 -> new float[] {0, 0};
            case 2 -> new float[] {Float.parseFloat(line[1]), 0};
            default -> new float[] {Float.parseFloat(line[1]), Float.parseFloat(line[2])};
        };
    }

    private static float[] parseVector3(String[] line) {
        return switch (line.length) {
            case 1 -> new float[] {0, 0, 0};
            case 2 -> new float[] {Float.parseFloat(line[1]), 0, 0};
            case 3 -> new float[] {Float.parseFloat(line[1]), Float.parseFloat(line[2]), 0};
            default -> new float[] {Float.parseFloat(line[1]), Float.parseFloat(line[2]), Float.parseFloat(line[3])};
        };
    }

    static float[] parseVector4(String[] line) {
        return switch (line.length) {
            case 1 -> new float[] {0, 0, 0, 1};
            case 2 -> new float[] {Float.parseFloat(line[1]), 0, 0, 1};
            case 3 -> new float[] {Float.parseFloat(line[1]), Float.parseFloat(line[2]), 0, 1};
            case 4 -> new float[] {Float.parseFloat(line[1]), Float.parseFloat(line[2]), Float.parseFloat(line[3]), 1};
            default -> new float[] {Float.parseFloat(line[1]), Float.parseFloat(line[2]), Float.parseFloat(line[3]), Float.parseFloat(line[4])};
        };
    }

    public Map<String, ModelGroup> getParts() {
        return Collections.unmodifiableMap(parts);
    }

    /** Every mesh in the model, depth first, in file order. */
    public List<ModelMesh> allMeshes() {
        List<ModelMesh> out = new ArrayList<>();
        for (ModelGroup group : parts.values())
            group.collectMeshes(out);
        return out;
    }

    /**
     * Turns every face of a mesh into quads with positions, UVs (0 to 1, {@code flip_v} applied)
     * and one normal per vertex. Triangles become a quad whose last vertex repeats; faces with more
     * than four vertices are fan-split around their first vertex.
     */
    public List<ObjQuad> buildQuads(ModelMesh mesh) {
        List<ObjQuad> quads = new ArrayList<>();
        for (int[][] face : mesh.faces) {
            if (face.length <= 4) {
                quads.add(makeQuad(face));
                continue;
            }
            for (int i = 1; i + 1 < face.length; i += 2) {
                int[][] part = i + 2 < face.length
                        ? new int[][] {face[0], face[i], face[i + 1], face[i + 2]}
                        : new int[][] {face[0], face[i], face[i + 1]};
                quads.add(makeQuad(part));
            }
        }
        return quads;
    }

    private ObjQuad makeQuad(int[][] indices) {
        boolean needsNormalRecalculation = false;
        for (int[] ints : indices)
            needsNormalRecalculation |= ints.length < 3 || ints[2] < 0 || normals.isEmpty();

        float[] faceNormal = {0, 0, 0};
        if (needsNormalRecalculation)
            faceNormal = faceNormal(indices);

        float[][] pos = new float[4][];
        float[][] uv = new float[4][];
        float[][] norm = new float[4][];
        float[][] color = new float[4][];
        for (int i = 0; i < 4; i++) {
            int[] index = indices[Math.min(i, indices.length - 1)];
            pos[i] = positions.get(index[0]).clone();

            float[] texCoord = index.length >= 2 && index[1] >= 0 && !texCoords.isEmpty()
                    ? texCoords.get(index[1])
                    : DEFAULT_COORDS[i];
            uv[i] = new float[] {texCoord[0], settings.flipV() ? 1 - texCoord[1] : texCoord[1]};

            norm[i] = (!needsNormalRecalculation ? normals.get(index[2]) : faceNormal).clone();
            color[i] = index.length >= 4 && index[3] >= 0 && !colors.isEmpty() ? colors.get(index[3]).clone() : new float[] {1, 1, 1, 1};
        }
        return new ObjQuad(pos, uv, norm, color);
    }

    private float[] faceNormal(int[][] indices) {
        float[] a = positions.get(indices[0][0]);
        float[] b = positions.get(indices[1][0]);
        float[] c = positions.get(indices[2][0]);
        float[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        float[] ac = {c[0] - a[0], c[1] - a[1], c[2] - a[2]};
        float[] n = {
                ab[1] * ac[2] - ab[2] * ac[1],
                ab[2] * ac[0] - ab[0] * ac[2],
                ab[0] * ac[1] - ab[1] * ac[0]
        };
        float len = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        if (len > 0) {
            n[0] /= len;
            n[1] /= len;
            n[2] /= len;
        }
        return n;
    }

    /**
     * NeoForge's automatic culling rule: a quad whose four vertices all lie on one face of the
     * unit block, with a normal pointing out of it, is culled against that side's neighbour.
     *
     * @param pos the quad's four positions after any rotation, in block space (0 to 1)
     * @return the side, or null when the quad is not on the block boundary
     */
    public static Side cullSide(float[][] pos, float[] normal) {
        if (allEqual(pos, 0, 0) && normal[0] < 0) return Side.WEST;
        if (allEqual(pos, 0, 1) && normal[0] > 0) return Side.EAST;
        if (allEqual(pos, 2, 0) && normal[2] < 0) return Side.NORTH;
        if (allEqual(pos, 2, 1) && normal[2] > 0) return Side.SOUTH;
        if (allEqual(pos, 1, 0) && normal[1] < 0) return Side.DOWN;
        if (allEqual(pos, 1, 1) && normal[1] > 0) return Side.UP;
        return null;
    }

    private static boolean allEqual(float[][] pos, int axis, float value) {
        for (float[] p : pos)
            if (Math.abs(p[axis] - value) > EPSILON)
                return false;
        return true;
    }

    /** The side a normal points to most, used as the quad's lighting face. */
    public static Side nearestSide(float[] n) {
        float ax = Math.abs(n[0]), ay = Math.abs(n[1]), az = Math.abs(n[2]);
        if (ay >= ax && ay >= az) return n[1] >= 0 ? Side.UP : Side.DOWN;
        if (ax >= az) return n[0] >= 0 ? Side.EAST : Side.WEST;
        return n[2] >= 0 ? Side.SOUTH : Side.NORTH;
    }

    /** A quad ready for baking. Arrays are owned by the quad and may be modified. */
    public record ObjQuad(float[][] positions, float[][] uvs, float[][] normals, float[][] colors) {}

    public static class ModelObject {
        public final String name;
        final List<ModelMesh> meshes = new ArrayList<>();

        ModelObject(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public List<ModelMesh> meshes() {
            return Collections.unmodifiableList(meshes);
        }

        void collectMeshes(List<ModelMesh> out) {
            out.addAll(meshes);
        }
    }

    public static class ModelGroup extends ModelObject {
        final Map<String, ModelObject> parts = new LinkedHashMap<>();

        ModelGroup(String name) {
            super(name);
        }

        public Map<String, ModelObject> parts() {
            return Collections.unmodifiableMap(parts);
        }

        @Override
        void collectMeshes(List<ModelMesh> out) {
            super.collectMeshes(out);
            for (ModelObject object : parts.values())
                object.collectMeshes(out);
        }
    }

    public static class ModelMesh {
        public ObjMaterialLibrary.Material mat;
        public String smoothingGroup;
        public final List<int[][]> faces = new ArrayList<>();

        ModelMesh(ObjMaterialLibrary.Material currentMat, String currentSmoothingGroup) {
            this.mat = currentMat;
            this.smoothingGroup = currentSmoothingGroup;
        }
    }
}

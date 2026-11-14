package io.github.davevermeulen.betafront.obj;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjGeometryTest {
    private final List<String> loadedLibraries = new ArrayList<>();

    private static InputStream resource(String location) throws IOException {
        String path = "/" + location.substring(location.indexOf(':') + 1);
        InputStream in = ObjGeometryTest.class.getResourceAsStream(path);
        if (in == null)
            throw new FileNotFoundException(location);
        return in;
    }

    private ObjMaterialLibrary loadLibrary(String location) throws IOException {
        loadedLibraries.add(location);
        try (var tokenizer = new ObjTokenizer(resource(location))) {
            return new ObjMaterialLibrary(tokenizer);
        }
    }

    private ObjGeometry parse(ObjSettings settings) throws IOException {
        try (var tokenizer = new ObjTokenizer(resource(settings.modelLocation()))) {
            return ObjGeometry.parse(tokenizer, settings, this::loadLibrary);
        }
    }

    private ObjGeometry parseText(String text) throws IOException {
        var tokenizer = new ObjTokenizer(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        return ObjGeometry.parse(tokenizer, ObjSettings.defaults("test:inline.obj"), this::loadLibrary);
    }

    @Test
    void cubeLoadsWithRelativeMaterialLibrary() throws IOException {
        ObjGeometry cube = parse(ObjSettings.defaults("test:objtest/cube.obj"));

        assertEquals(List.of("test:objtest/cube.mtl"), loadedLibraries);
        assertEquals(8, cube.positions.size());
        assertEquals(6, cube.normals.size());

        var meshes = cube.allMeshes();
        assertEquals(1, meshes.size());
        assertEquals("stone", meshes.get(0).mat.name);
        assertEquals("#texture0", meshes.get(0).mat.diffuseColorMap);
        assertEquals(6, meshes.get(0).faces.size());
        assertEquals(6, cube.buildQuads(meshes.get(0)).size());
    }

    @Test
    void everyCubeFaceIsCulledToItsOwnSide() throws IOException {
        ObjGeometry cube = parse(ObjSettings.defaults("test:objtest/cube.obj"));
        List<ObjGeometry.Side> sides = new ArrayList<>();
        for (var quad : cube.buildQuads(cube.allMeshes().get(0)))
            sides.add(ObjGeometry.cullSide(quad.positions(), quad.normals()[0]));

        assertEquals(List.of(
                ObjGeometry.Side.DOWN, ObjGeometry.Side.UP,
                ObjGeometry.Side.NORTH, ObjGeometry.Side.SOUTH,
                ObjGeometry.Side.WEST, ObjGeometry.Side.EAST), sides);
    }

    @Test
    void innerFacesAreNotCulled() {
        float[][] pos = {{0.5f, 0, 0}, {0.5f, 1, 0}, {0.5f, 1, 1}, {0.5f, 0, 1}};
        assertNull(ObjGeometry.cullSide(pos, new float[] {1, 0, 0}));
        // On the boundary but facing inwards
        float[][] west = {{0, 0, 0}, {0, 1, 0}, {0, 1, 1}, {0, 0, 1}};
        assertNull(ObjGeometry.cullSide(west, new float[] {1, 0, 0}));
    }

    @Test
    void flipVInvertsTextureV() throws IOException {
        var normal = parse(ObjSettings.defaults("test:objtest/cube.obj"));
        var flipped = parse(new ObjSettings("test:objtest/cube.obj", true, true, true, true, null, true));

        float v = normal.buildQuads(normal.allMeshes().get(0)).get(0).uvs()[2][1];
        float vFlipped = flipped.buildQuads(flipped.allMeshes().get(0)).get(0).uvs()[2][1];
        assertEquals(1f, v);
        assertEquals(0f, vFlipped);
    }

    @Test
    void mtlOverrideReplacesMtllib() throws IOException {
        var settings = new ObjSettings("test:objtest/cube.obj", true, true, false, true, "other:objtest/cube.mtl", true);
        parse(settings);
        assertEquals(List.of("other:objtest/cube.mtl"), loadedLibraries);
    }

    @Test
    void triangleBecomesDegenerateQuadWithComputedNormal() throws IOException {
        ObjGeometry shapes = parse(ObjSettings.defaults("test:objtest/shapes.obj"));
        var tri = shapes.getParts().get("tri").meshes().get(0);
        assertNull(tri.mat);

        var quads = shapes.buildQuads(tri);
        assertEquals(1, quads.size());
        var quad = quads.get(0);
        assertArrayEquals(quad.positions()[2], quad.positions()[3]);
        // (0.6,0,0) x (0.3,0,0.6) points down
        assertEquals(-1f, quad.normals()[0][1], 1e-5f);
        assertEquals(ObjGeometry.Side.DOWN, ObjGeometry.nearestSide(quad.normals()[0]));
    }

    @Test
    void hexagonIsFanSplitWithoutLosingVertices() throws IOException {
        ObjGeometry shapes = parse(ObjSettings.defaults("test:objtest/shapes.obj"));
        var hex = shapes.getParts().get("hex").meshes().get(0);
        var quads = shapes.buildQuads(hex);

        // 6 vertices: quad (0,1,2,3) + triangle (0,3,4) + ... fan of two quads covers 0..5
        assertEquals(2, quads.size());
        assertArrayEquals(new float[] {0, 0, 0}, quads.get(0).positions()[0]);
        assertArrayEquals(new float[] {1, 0, 1}, quads.get(0).positions()[3]);
        assertArrayEquals(new float[] {1, 0, 1}, quads.get(1).positions()[1]);
        assertArrayEquals(new float[] {-0.5f, 0, 0.5f}, quads.get(1).positions()[3]);
    }

    @Test
    void groupsUnderObjectsFormATree() throws IOException {
        ObjGeometry groups = parse(ObjSettings.defaults("test:objtest/groups.obj"));
        assertEquals(List.of("Body", "Lid"), List.copyOf(groups.getParts().keySet()));
        assertEquals(List.of("left", "right"), List.copyOf(groups.getParts().get("Body").parts().keySet()));
        assertEquals("Body/left", groups.getParts().get("Body").parts().get("left").name());
        assertEquals(3, groups.allMeshes().size());
    }

    @Test
    void homogeneousCoordinatesAreDivided() throws IOException {
        var geometry = parseText("v 2 4 6 2\nf 1 1 1\n");
        assertArrayEquals(new float[] {1, 2, 3}, geometry.positions.get(0));
    }

    @Test
    void badIndexReportsFileAndLine() {
        var error = assertThrows(IOException.class, () -> parseText("v 0 0 0\nv 1 0 0\n\nf 1 2 7\n"));
        assertTrue(error.getMessage().startsWith("test:inline.obj line 4:"), error.getMessage());
    }

    @Test
    void unknownMaterialFallsBackToDefault() throws IOException {
        var geometry = parseText("v 0 0 0\nv 1 0 0\nv 1 1 0\nusemtl missing\nf 1 2 3\n");
        assertNull(geometry.allMeshes().get(0).mat);
    }

    @Test
    void materialLibraryReadsColoursAndMaps() throws IOException {
        var lib = loadLibrary("test:objtest/cube.mtl");
        var glow = lib.getMaterial("glow");
        assertEquals(1f, glow.ambientLevel());
        assertEquals("examplemod:block/lamp_glow", glow.diffuseColorMap);
        assertEquals(0.8f, lib.getMaterial("stone").diffuseColor[0]);
        assertEquals(0f, lib.getMaterial("stone").ambientLevel());
    }

    @Test
    void tokenizerJoinsContinuationsAndSkipsComments() throws IOException {
        var tokenizer = new ObjTokenizer(new ByteArrayInputStream(
                "# comment\n  v 1 2 \\\n 3\n\nmap_Kd #texture0\n".getBytes(StandardCharsets.UTF_8)));
        assertArrayEquals(new String[] {"v", "1", "2", "3"}, tokenizer.readAndSplitLine(true));
        assertArrayEquals(new String[] {"map_Kd", "#texture0"}, tokenizer.readAndSplitLine(true));
        assertNull(tokenizer.readAndSplitLine(true));
    }

    @Test
    void shippedRampModelParsesAndCullsItsOuterFaces() throws IOException {
        var settings = new ObjSettings("betafront:assets/betafront/stationapi/models/block/obj_ramp.obj", true, true, true, true, null, true);
        ObjGeometry ramp = parse(settings);
        assertEquals(List.of("betafront:assets/betafront/stationapi/models/block/obj_ramp.mtl"), loadedLibraries);

        var mesh = ramp.allMeshes().get(0);
        assertEquals("#ramp", mesh.mat.diffuseColorMap);
        List<ObjGeometry.Side> sides = new ArrayList<>();
        for (var quad : ramp.buildQuads(mesh))
            sides.add(ObjGeometry.cullSide(quad.positions(), quad.normals()[0]));
        // bottom, back, slope (not on the boundary), west and east triangles
        assertEquals(java.util.Arrays.asList(
                ObjGeometry.Side.DOWN, ObjGeometry.Side.NORTH, null,
                ObjGeometry.Side.WEST, ObjGeometry.Side.EAST), sides);
    }

    @Test
    void blenderCrystalWithMissingMtlUsesHashTextureMaterial() throws IOException {
        var settings = new ObjSettings("betafront:assets/betafront/stationapi/models/block/example_crystal.obj", true, true, true, true, null, true);
        ObjGeometry crystal = parse(settings);

        var meshes = crystal.allMeshes();
        assertEquals(1, meshes.size());
        assertEquals("#texture0", meshes.get(0).mat.diffuseColorMap);
        assertEquals(45, meshes.get(0).faces.size());
        assertEquals(45, crystal.buildQuads(meshes.get(0)).size());
    }
}

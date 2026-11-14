package io.github.davevermeulen.objmodelsbabric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ObjModelsBabric: Wavefront OBJ block and item models for StationAPI on Babric b1.7.3.
 * <p>
 * Point a StationAPI model JSON at an .obj file with {@code "loader": "objmodelsbabric:obj"}.
 */
public final class ObjModelsBabric {
    public static final String NAMESPACE = "objmodelsbabric";
    public static final Logger LOGGER = LogManager.getLogger("ObjModelsBabric");

    private ObjModelsBabric() {}
}

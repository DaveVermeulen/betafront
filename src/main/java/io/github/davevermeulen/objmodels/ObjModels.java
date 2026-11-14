package io.github.davevermeulen.objmodels;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * OBJ Models: Wavefront OBJ block and item models for StationAPI on Babric b1.7.3.
 * <p>
 * Point a StationAPI model JSON at an .obj file with {@code "loader": "objmodels:obj"}.
 */
public final class ObjModels {
    public static final String NAMESPACE = "objmodels";
    public static final Logger LOGGER = LogManager.getLogger("OBJ Models");

    private ObjModels() {}
}

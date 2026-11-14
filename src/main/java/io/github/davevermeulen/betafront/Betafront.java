package io.github.davevermeulen.betafront;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Betafront: Wavefront OBJ block and item models for StationAPI on Babric b1.7.3.
 * <p>
 * Point a StationAPI model JSON at an .obj file with {@code "loader": "betafront:obj"}.
 */
public final class Betafront {
    public static final String NAMESPACE = "betafront";
    public static final Logger LOGGER = LogManager.getLogger("Betafront");

    private Betafront() {}
}

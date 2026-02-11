package org.mapfish.print;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * Utility class for handling WGS 84 with Pseudo-Mercator projection specific calculations.
 *
 * @author fdiaz
 */
public final class PseudoMercatorUtils {

  private PseudoMercatorUtils() {}

  /**
   * Checks if a given CoordinateReferenceSystem is WGS 84 with Pseudo-Mercator projection.
   *
   * @param crs The CoordinateReferenceSystem to check.
   * @return {@code true} if the CRS is Pseudo-Mercator, {@code false} otherwise.
   */
  public static boolean isPseudoMercator(final CoordinateReferenceSystem crs) {
    String crsNameCode = crs.getName().getCode();
    String crsId = crs.getIdentifiers().iterator().next().toString().toLowerCase();
    return (crsNameCode.contains("wgs 84")
            && (crsNameCode.contains("pseudo-mercator")
                || crsNameCode.contains("pseudo mercator")
                || crsNameCode.contains("web-mercator")
                || crsNameCode.contains("web mercator")))
        || "EPSG:3857".equalsIgnoreCase(crsId);
  }
}

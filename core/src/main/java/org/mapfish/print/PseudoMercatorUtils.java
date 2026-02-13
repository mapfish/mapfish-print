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
    // Possible improvement : Some CRS implementations use URNs (e.g. urn:ogc:def:crs:EPSG::3857) or
    // provide multiple identifiers. Consider iterating all identifiers and/or using GeoTools
    // utilities like CRS.lookupEpsgCode(...) / IdentifiedObjects.lookupIdentifier(...) to reliably
    // detect EPSG:3857 (also guard against crs/crs.getName() being null).

    // Check CRS name (case-insensitive)
    String crsNameCode = crs.getName().getCode().toLowerCase();
    boolean nameMatch =
        crsNameCode.contains("wgs 84")
            && (crsNameCode.contains("pseudo-mercator")
                || crsNameCode.contains("pseudo mercator")
                || crsNameCode.contains("web-mercator")
                || crsNameCode.contains("web mercator"));

    // Check identifiers (safely handle missing identifiers)
    if (nameMatch) {
      return true;
    }

    if (crs.getIdentifiers() != null && crs.getIdentifiers().iterator().hasNext()) {
      String crsId = crs.getIdentifiers().iterator().next().toString();
      return "EPSG:3857".equalsIgnoreCase(crsId);
    }

    return false;
  }
}

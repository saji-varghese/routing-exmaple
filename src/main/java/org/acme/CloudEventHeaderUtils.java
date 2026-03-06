package org.acme;


import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for extracting CloudEvent attributes from HTTP headers.
 *
 * CloudEvent HTTP headers follow the format:
 *   ce-id
 *   ce-type
 *   ce-source
 *   ce-specversion
 *   ce-time
 *
 * This utility:
 *  - extracts all ce-* headers
 *  - normalizes header names to lowercase
 *  - returns attributes as Map<String,Object>
 */
public final class CloudEventHeaderUtils {

  private static final String CE_PREFIX = "ce-";

  private CloudEventHeaderUtils() {
    // Utility class
  }

  /**
   * Extract CloudEvent attributes from JAX-RS HttpHeaders.
   */
  public static Map<String, String> extract(HttpHeaders headers) {
    return extract(headers.getRequestHeaders());
  }

  /**
   * Extract CloudEvent attributes from MultivaluedMap headers.
   */
  public static Map<String, String> extract(MultivaluedMap<String, String> headers) {

    Map<String, String> result = new HashMap<>();

    if (headers == null) {
      return result;
    }

    headers.forEach((name, values) -> {
      if (name == null) {
        return;
      }

      String lower = name.toLowerCase(Locale.ROOT);

      if (lower.startsWith(CE_PREFIX)) {
        if (values != null && !values.isEmpty()) {
          result.put(lower, values.getFirst());
        }
      }
    });

    return result;
  }

  /**
   * Extract CloudEvent attributes from a simple Map of headers.
   */
  public static Map<String, String> extract(Map<String, String> headers) {

    Map<String, String> result = new HashMap<>();

    if (headers == null) {
      return result;
    }

    headers.forEach((name, value) -> {
      if (name == null) {
        return;
      }

      String lower = name.toLowerCase(Locale.ROOT);

      if (lower.startsWith(CE_PREFIX)) {
        result.put(lower, value);
      }
    });

    return result;
  }
}
package org.acme;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parses the routing configuration (JSON) supplied by the user.
 *
 * <p>The JSON file must be an array of objects that contain:
 *
 * <ul>
 *   <li><b>filters</b>: a map of CloudEvent attribute name → expected value. The
 *       attribute names must match the HTTP header name that the CloudEvent binary
 *       mode uses (e.g. {@code "ce-source"}, {@code "ce-type"}, or any custom
 *       {@code "ce-…"} attribute).
 *   <li><b>endpoint</b>: a string containing the target HTTP URL to which matching
 *       requests must be forwarded.
 * </ul>
 *
 * Example configuration (the same shape you described):
 *
 * <pre>{@code
 * [
 *   {
 *     "filters": {
 *       "ce-source": "/mobile/skybet/ios",
 *       "ce-type"  : "sports.recommendation.request",
 *       "ce-subject": "football"
 *     },
 *     "endpoint": "http://decision.serviceOne.com"
 *   },
 *   {
 *     "filters": {
 *       "ce-source": "/mobile/betfair/ios",
 *       "ce-type"  : "sports.recommendation.request",
 *       "custom-attr": "gold"
 *     },
 *     "endpoint": "http://decision.serviceTwo.com"
 *   },
 *   {
 *     "filters": {},               // catch‑all / dead‑letter rule
 *     "endpoint": "http://deadletter.service"
 *   }
 * ]
 * }</pre>
 *
 * The parser returns an **immutable**, ordered {@code List<Rule>}.  The order of the
 * list is the order of the entries in the JSON file (first‑match semantics).
 */
@ApplicationScoped
public final class RouteParser {

  private RouteParser(){}

  /** Jackson mapper – thread‑safe after configuration, so we can keep a static instance. */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Parses a JSON string into a list of {@link Rule}s.
   *
   * @param jsonContent JSON text (must represent an array of filter/endpoint objects)
   * @return an immutable list of routing rules, in the same order as the JSON array
   * @throws IllegalArgumentException if parsing fails
   */
  public static List<Rule> parse(String jsonContent) {
    Objects.requireNonNull(jsonContent, "jsonContent");
    try {
      // Read the raw structure first – a list of DTO objects.
      List<RawRule> rawRules = MAPPER.readValue(
          jsonContent,
          new TypeReference<List<RawRule>>() {});

      // Convert each DTO into the public domain object {@link Rule}.

      return rawRules.stream()
          .map(r -> new Rule(
              r.getFilters() != null ? r.getFilters() : Collections.emptyMap(),
              r.getEndpoint()))
          .toList();
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse routing configuration", e);
    }
  }

  /**
   * Reads a JSON file from the supplied {@link Path} and parses it.
   *
   * @param configPath path to the routing JSON file (commonly a mounted ConfigMap)
   * @return immutable list of {@link Rule}s
   * @throws IllegalArgumentException if the file cannot be read or parsed
   */
  public static List<Rule> parse(Path configPath) {
    Objects.requireNonNull(configPath, "configPath");
    try {
      String content = Files.readString(configPath);
      return parse(content);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Unable to read routing configuration from " + configPath, e);
    }
  }

  /* -------------------------------------------------------------------- *
   *  Private DTO used only for Jackson deserialization.
   *  Keeping it private hides the parsing model from the public API.
   * -------------------------------------------------------------------- */
  @RegisterForReflection
  private static class RawRule {
    private Map<String, String> filters;
    private String endpoint;

    public Map<String, String> getFilters() {
      return filters;
    }

    public void setFilters(Map<String, String> filters) {
      this.filters = filters;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }
  }
}

package org.acme;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Immutable representation of a routing rule.
 *
 * <p>A rule consists of:
 *
 * <ul>
 *   <li>A map of CloudEvent attribute filters (e.g. {@code "ce-source"} → {@code "/mobile/skybet/ios"}).
 *   <li>The HTTP endpoint to which a request that matches the filters will be forwarded.
 * </ul>
 *
 * <p>The {@link #matches(Map)} method implements the exact‑match semantics required for the MVP:
 * a rule matches when **every** entry in {@code filters} is present in the supplied attribute
 * map with the same value. An empty filter map matches any request (useful for a catch‑all
 * / dead‑letter rule).
 *
 * @param filters  CloudEvent attribute filters – never {@code null}.
 * @param endpoint Destination endpoint (e.g. {@code http://decision.serviceOne.com}).
 */
@RegisterForReflection
public record Rule(Map<String, String> filters, String endpoint) {

  /**
   * Creates a rule.
   *
   * @param filters  map of attribute → expected value; may be {@code null} or empty
   * @param endpoint non‑null target URL
   */
  public Rule(Map<String, String> filters, String endpoint) {
    // Defensive copy – the internal map is immutable
    this.filters = Collections.unmodifiableMap(
        filters != null ? new HashMap<>(filters) : Collections.emptyMap());
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
  }

  /**
   * Returns {@code true} when the supplied attribute map contains **all** entries defined in
   * {@link #filters} with exactly the same values.
   *
   * <p>If {@code filters} is empty the method returns {@code true} (catch‑all rule).
   *
   * @param attributes map of CloudEvent attribute name → actual value (case‑sensitive)
   * @return {@code true} if the request matches this rule
   */
  public boolean matches(Map<String, String> attributes) {
    for (Entry<String, String> e : filters.entrySet()) {
      if (!e.getValue().equals(attributes.get(e.getKey()))) {
        return false;
      }
    }
    return true;
  }

}

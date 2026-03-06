package org.acme;



import io.quarkus.rest.client.reactive.HeaderFiller;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Copies all inbound CloudEvent headers (those starting with “ce-”)
 * into the outbound request that the REST client will send.
 */
@ApplicationScoped
public class HeaderFillerImpl implements HeaderFiller {


  private final HttpHeaders inbound;                // request‑scoped – gives access to incoming headers

  public HeaderFillerImpl(HttpHeaders inbound) {
    this.inbound = inbound;
  }

  @Override
  public void addHeaders(MultivaluedMap<String, String> out) {
    inbound.getRequestHeaders().forEach((name, values) -> {
      if (name.toLowerCase().startsWith("ce-")) {
        out.add(name, values.getFirst());   // take first value; CloudEvents use single values
      }
    });
    // Additional static headers (e.g. auth token) can be added here if needed.
  }
}


package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;
import java.util.Locale;

/**
 * Propagates only CloudEvent HTTP headers from the incoming request.
 */
@ApplicationScoped
public class CloudEventHeadersFactory implements ClientHeadersFactory {

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> incomingHeaders,
      MultivaluedMap<String, String> clientOutgoingHeaders) {

    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();

    if (incomingHeaders == null) {
      return result;
    }

    incomingHeaders.forEach((name, values) -> {
      if (name != null && name.toLowerCase(Locale.ROOT).startsWith("ce-")) {
        result.put(name, values == null ? List.of() : values);
      }
    });

    return result;
  }
}
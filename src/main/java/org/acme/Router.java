package org.acme;

import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/v1")
//@Consumes(MediaType.APPLICATION_OCTET_STREAM)
//@Produces(MediaType.APPLICATION_OCTET_STREAM)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Router {

  private static final Logger LOGGER = Logger.getLogger(Router.class);

  private final DecisionClient client;

  @ConfigProperty(name = "router.routes")
  String routesJson;

  private List<Rule> rules;

  public Router(@RestClient DecisionClient client) {
    this.client = Objects.requireNonNull(client);
  }

  @PostConstruct
  void init() {
    try {
      java.nio.file.Path path = Paths.get(routesJson);
      LOGGER.infof("Loading routes from: %s", path.toAbsolutePath());

      if (!java.nio.file.Files.exists(path)) {
        throw new IllegalArgumentException("Route config file does not exist: " + path);
      }

      rules = RouteParser.parse(path);
      LOGGER.infof("Found routes: %s", rules);
    } catch (Exception e) {
      LOGGER.error("Failed to initialise router", e);
      throw e;
    }
  }

  @POST
  @Path("/decision")
  public Response route(
      @Context HttpHeaders hdr,
      InputStream body,
      @Context UriInfo uri,
      @Context HttpServerRequest request) {

    LOGGER.info("Entered /v1/decision");

    Map<String, String> attrs = CloudEventHeaderUtils.extract(hdr);
    LOGGER.infof("Extracted CloudEvent headers: %s", attrs);

    Optional<Rule> matching = rules.stream()
        .filter(r -> r.matches(attrs))
        .findFirst();



    matching.ifPresent(r -> LOGGER.infof("Matched rule: %s", r));

    String endpoint = matching
        .map(Rule::endpoint)
        .orElse("http://deadletter.service");

    LOGGER.infof("Forwarding to endpoint: %s", endpoint);

    try {
      Response upstream = client.forward(endpoint, body);
      LOGGER.infof("Upstream status: %d", upstream.getStatus());
      return Response.fromResponse(upstream).build();
    }catch (WebApplicationException e) {
      LOGGER.errorf(e, "Failed to forward request to %s", endpoint);
      return Response.fromResponse(e.getResponse()).build();
    }
    catch (Exception e) {
      LOGGER.errorf(e, "Failed to forward request to %s", endpoint);
      return Response.serverError().build();
    }
  }
}
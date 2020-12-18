package org.acme.getting.started;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;

@Path("/gingerbread")
public class ReactiveGreetingResource {

    @Inject
    ReactiveGreetingService service;


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/prova/{name}")
    public Uni<String> prova(@PathParam String name) {
            return service.endpoint(name);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/quarkus_gingerbread")
    public Uni<ReactiveGreetingService.Gingerbread> gingerBread(@PathParam String name) {
        return service.prepareGingerbread();
    }


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}

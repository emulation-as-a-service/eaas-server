package de.bwl.bwfla.emil.utils;

import de.bwl.bwfla.common.services.security.AuthenticatedUser;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import de.bwl.bwfla.common.services.security.UserContext;
import de.bwl.bwfla.emil.Components;
import de.bwl.bwfla.emil.datatypes.ComputeRequest;
import de.bwl.bwfla.emil.datatypes.ComputeResponse;
import de.bwl.bwfla.emil.session.HeadlessSession;
import de.bwl.bwfla.emil.session.Session;
import de.bwl.bwfla.emil.session.SessionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Path("/compute")
@ApplicationScoped
public class Compute {

    @Inject
    private SessionManager sessions;

    @Inject
    private Components endpoint;

    @Inject
    @AuthenticatedUser
    private UserContext authenticatedUser;

    protected final static Logger LOG = Logger.getLogger(Compute.class.getName());

    @POST
    @Secured(roles = {Role.PUBLIC})
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(ComputeRequest request) {
        if (request.getComponents() == null || request.getComponents().size() == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        HeadlessSession headlessSession = new HeadlessSession(request.getComponents(), getUserContext());

        sessions.register(headlessSession);
        sessions.setLifetime(headlessSession.id(), request.getTimeout(), TimeUnit.MINUTES, UUID.randomUUID().toString());

        ComputeResponse response = new ComputeResponse();
        response.setId(headlessSession.id());
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Secured(roles = {Role.PUBLIC})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{sessionId}")
    public Response state(@PathParam("sessionId") String sessionId) {
        LOG.info("Getting state for session " + sessionId);
        Session session = sessions.get(sessionId);
        if (session == null) {
            LOG.info("Session " + sessionId + " not found!");
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        HeadlessSession headlessSession = (HeadlessSession) session;

        ComputeResponse response = new ComputeResponse();
        response.setId(headlessSession.id());
        response.setResult(headlessSession.getResult(endpoint, LOG));

        return Response.status(Response.Status.OK).entity(response).build();
    }

    @POST
    @Secured(roles = {Role.PUBLIC})
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{sessionId}/stop")
    public Response stopSession(@PathParam("sessionId") String sessionId){
        LOG.info("Got request to stop session:" + sessionId);
        Session session = sessions.get(sessionId);
        if (session == null) {
            LOG.info("Session " + sessionId + " not found - can't stop.");
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        HeadlessSession headlessSession = (HeadlessSession) session;
        LOG.info(" --- Manually stopping session " + sessionId);
        headlessSession.onTimeout(endpoint, LOG);
        return Response.status(Response.Status.OK).entity("Manually called timeout, session should stop momentarily.").build();

    }

    private UserContext getUserContext() {
        return (authenticatedUser != null) ? authenticatedUser.clone() : new UserContext();
    }
}

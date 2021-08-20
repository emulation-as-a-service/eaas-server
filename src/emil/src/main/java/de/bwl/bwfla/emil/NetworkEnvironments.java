package de.bwl.bwfla.emil;

import com.openslx.eaas.common.databind.Streamable;
import de.bwl.bwfla.api.blobstore.BlobStore;
import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import de.bwl.bwfla.common.utils.NetworkUtils;
import de.bwl.bwfla.emil.datatypes.NetworkEnvironment;
import de.bwl.bwfla.emil.datatypes.NetworkEnvironmentElement;
import de.bwl.bwfla.common.services.rest.ErrorInformation;
import de.bwl.bwfla.emucomp.api.NetworkConfiguration;
import org.apache.tamaya.inject.api.Config;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


@Path("network-environments")
@ApplicationScoped
public class NetworkEnvironments extends EmilRest {
    
    @Inject
    private DatabaseEnvironmentsAdapter envHelper;
    @Inject
    private EmilEnvironmentRepository emilEnvRepo;

    private BlobStore blobstore;

    @Inject
    @Config(value = "rest.blobstore")
    private String blobStoreRestAddress;


    @Inject
    @Config(value = "ws.blobstore")
    private String blobStoreWsAddress;

    @Inject
    private BlobStoreClient blobStoreClient;

    @PostConstruct
    public void init()
    {
        try {
            this.blobstore = blobStoreClient.getBlobStorePort(blobStoreWsAddress);
        } catch (BWFLAException e) {
            e.printStackTrace();
        }
    }

    @Secured(roles = {Role.RESTRICTED})
    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createNetworkEnvironment(NetworkEnvironment envNetworkEnv) {
        try {
            envNetworkEnv.getEmilEnvironments().forEach((networkElement -> {
                    if(networkElement.getMacAddress() != null && networkElement.getMacAddress().equals(""))
                        networkElement.setMacAddress(NetworkUtils.getRandomHWAddress());
            }));
            emilEnvRepo.saveNetworkEnvironment(envNetworkEnv);

            final JsonObject json = Json.createObjectBuilder()
                    .add("status", "0")
                    .build();

            return Emil.createResponse(Response.Status.OK, json.toString());

        } catch (Throwable t) {
            t.printStackTrace();
            return Emil.errorMessageResponse(t.getMessage());
        }
    }

    @Secured(roles = {Role.PUBLIC})
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNetworkEnvironments(@Context final HttpServletResponse response) {
        try {
            Stream<NetworkEnvironment> environments = emilEnvRepo.getNetworkEnvironments();
            return Response.ok(Streamable.of(environments))
                    .build();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new BadRequestException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorInformation(t.getMessage()))
                    .build());
        }
    }

    @Secured(roles = {Role.PUBLIC})
    @GET
    @Path("/{envId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNetworkEnvironment(@PathParam("envId") String envId,
                                          @QueryParam("jsonUrl") boolean jsonUrl, // deprecated, kept for compat reasons
                                          @QueryParam("json") boolean jsonObject,
                                          @Context final HttpServletResponse response ) {
        try {
            NetworkEnvironment env = emilEnvRepo.getEmilNetworkEnvironmentById(envId);

            if(jsonUrl || jsonObject)
            {
                NetworkConfiguration config = new NetworkConfiguration();
                config.setNetwork(env.getNetwork());
                config.setUpstream_dns(env.getUpstream_dns());
                config.setGateway(env.getGateway());
                if (env.getNetworking().isArchivedInternetEnabled()) {
                    config.setArchived_internet_date(env.getNetworking().getArchiveInternetDate());
                }
                NetworkConfiguration.DHCPConfiguration dhcp = new NetworkConfiguration.DHCPConfiguration();
                dhcp.setIp(env.getNetworking().getDhcpNetworkAddress());
                config.setDhcp(dhcp);

                List<NetworkConfiguration.EnvironmentNetworkConfiguration> ecs = new ArrayList<>();
                for(NetworkEnvironmentElement _env : env.getEmilEnvironments())
                {
                    NetworkConfiguration.EnvironmentNetworkConfiguration ec = new NetworkConfiguration.EnvironmentNetworkConfiguration();
                    ec.setMac(_env.getMacAddress());
                    ec.setIp(_env.getServerIp());
                    ec.setWildcard(_env.isWildcard());
                    if (_env.getFqdnList() != null)
                        ec.getHostnames().addAll(Arrays.asList(_env.getFqdnList()));
                    ecs.add(ec);
                }
                config.setEnvironments(ecs);
                if(jsonObject)
                    return Emil.createResponse(Response.Status.OK, config);

                String networkJson = config.jsonValueWithoutRoot(true);

                File tmpfile = File.createTempFile("network.json", null, null);
                Files.write(tmpfile.toPath(), networkJson.getBytes() , StandardOpenOption.CREATE);

                BlobDescription blobDescription = new BlobDescription();
                blobDescription.setDataFromFile(tmpfile.toPath())
                        .setNamespace("random")
                        .setDescription("random")
                        .setName("network")
                        .setType(".json");

                BlobHandle handle = blobstore.put(blobDescription);
                String url = handle.toRestUrl(blobStoreRestAddress);

                final JsonObject json = Json.createObjectBuilder()
                        .add("url", url)
                        .build();

                return Emil.createResponse(Response.Status.OK, json.toString());
            }
            else
                return Response.status(Response.Status.OK).entity(env).build();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new BadRequestException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorInformation(t.getMessage()))
                    .build());
        }
    }

    @Secured(roles = {Role.PUBLIC})
    @DELETE
    @Path("/{envId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteNetworkEnvironment(@PathParam("envId") String envId, @Context final HttpServletResponse response ) {
        try {
            emilEnvRepo.deleteEmilNetworkEnvironment(emilEnvRepo.getEmilNetworkEnvironmentById(envId));
            return Emil.createResponse(Response.Status.OK, "{\"data\": {\"status\": 0}}");
        } catch (Throwable t) {
            t.printStackTrace();
            throw new BadRequestException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorInformation(t.getMessage()))
                    .build());
        }
    }

    @Secured(roles = {Role.PUBLIC})
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateNetworkEnvironment(NetworkEnvironment envNetworkEnv, @Context final HttpServletResponse response) {
        try {
            envNetworkEnv.getEmilEnvironments().forEach((networkElement -> {
                if(networkElement.getMacAddress() != null && networkElement.getMacAddress().equals(""))
                    networkElement.setMacAddress(NetworkUtils.getRandomHWAddress());
            }));
            emilEnvRepo.saveNetworkEnvironment(envNetworkEnv);
            return Emil.createResponse(Response.Status.OK, "{\"status\":\"0\"}");
        } catch (Throwable t) {
            t.printStackTrace();
            throw new BadRequestException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorInformation(t.getMessage()))
                    .build());
        }
    }
}

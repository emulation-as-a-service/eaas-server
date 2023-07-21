/*
 * This file is part of the Emulation-as-a-Service framework.
 *
 * The Emulation-as-a-Service framework is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * The Emulation-as-a-Service framework is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Emulation-as-a-Software framework.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package de.bwl.bwfla.emil;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.webcohesion.enunciate.metadata.rs.TypeHint;
import de.bwl.bwfla.api.emucomp.Component;
import de.bwl.bwfla.api.emucomp.NetworkSwitch;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import de.bwl.bwfla.common.utils.NetworkUtils;
import de.bwl.bwfla.common.utils.TaskStack;
import de.bwl.bwfla.emil.datatypes.rest.NodeTcpComponentRequest;
import de.bwl.bwfla.emil.datatypes.rest.SlirpComponentRequest;
import de.bwl.bwfla.emil.datatypes.rest.SwitchComponentRequest;
import de.bwl.bwfla.emil.session.NetworkSession;
import de.bwl.bwfla.emil.session.Session;
import de.bwl.bwfla.emil.session.SessionComponent;
import de.bwl.bwfla.emil.session.SessionManager;
import de.bwl.bwfla.emucomp.api.NodeTcpConfiguration;
import org.apache.tamaya.ConfigurationProvider;
import org.apache.tamaya.inject.api.Config;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.rest.ErrorInformation;
import de.bwl.bwfla.emil.datatypes.NetworkRequest;
import de.bwl.bwfla.emil.datatypes.NetworkResponse;
import de.bwl.bwfla.emucomp.api.NetworkSwitchConfiguration;
import de.bwl.bwfla.emucomp.api.VdeSlirpConfiguration;
import de.bwl.bwfla.emucomp.client.ComponentClient;


@Path("/networks")
@ApplicationScoped
public class Networks {
    @Inject
    private SessionManager sessions = null;

    @Inject
    private Components components = null;

    @Inject
    @Config(value = "emil.retain_session")
    private String retain_session;

    private Component componentWsClient = null;
    private NetworkSwitch networkSwitchWsClient = null;

    protected final static Logger LOG = Logger.getLogger("NETWORKS");

    @PostConstruct
    private void initialize()
    {
        try {
            final var client = ComponentClient.get();
            final var eaasGwAddress = ConfigurationProvider.getConfiguration()
                    .get("ws.eaasgw");

            this.componentWsClient = client.getComponentPort(eaasGwAddress);
            this.networkSwitchWsClient = client.getNetworkSwitchPort(eaasGwAddress);
        }
        catch (Exception error) {
            throw new IllegalStateException("Initializing networks failed!", error);
        }
    }

    @POST
    @Secured(roles = {Role.PUBLIC})
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    @TypeHint(NetworkResponse.class)
    public Response createNetwork(NetworkRequest networkRequest) {
        if (networkRequest.getComponents() == null) {
            throw new BadRequestException(
                    Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorInformation("No components field given in the input data."))
                    .build());
        }

        NetworkResponse networkResponse = null;
        try {
            // a switch comes included with every network group
            final SwitchComponentRequest switchComponentRequest = new SwitchComponentRequest();
            switchComponentRequest.setConfig(new NetworkSwitchConfiguration());
            final String switchId = components.createComponent(switchComponentRequest).getId();
            final NetworkSession session = new NetworkSession(switchId, networkRequest);
            session.components()
                    .put(switchId, new SessionComponent(switchId));

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            
            sessions.register(session);
            sessions.setLifetime(session.id(), Long.parseLong(retain_session), TimeUnit.MINUTES,
                    "autodetached session @ " + dateFormat.format(new Date()));

            networkResponse = new NetworkResponse(session.id());

            if (networkRequest.hasInternet()) {
                final String slirpMac = new VdeSlirpConfiguration().getHwAddress();

                SlirpComponentRequest slirpConfig = new SlirpComponentRequest();
                slirpConfig.setHwAddress(slirpMac);
                slirpConfig.setDhcp(networkRequest.isDhcp());

                slirpConfig.setGateway(networkRequest.getGateway());

                if (networkRequest.getGateway() != null){
                    slirpConfig.setGateway(networkRequest.getGateway());
                }
                if (networkRequest.getNetwork() != null)
                    slirpConfig.setNetwork(networkRequest.getNetwork());

                final String slirpId = components.createComponent(slirpConfig).getId();
                session.components()
                        .put(slirpId, new SessionComponent(slirpId));

                Map<String, URI> controlUrls = ComponentClient.controlUrlsToMap(componentWsClient.getControlUrls(slirpId));
                String slirpUrl = controlUrls.get("ws+ethernet+" + slirpMac).toString();

                networkSwitchWsClient.connect(switchId, slirpUrl);
            }


//            if(network.isDhcp())
//            {
//                NodeTcpConfiguration nodeConfig = new NodeTcpConfiguration();
//                nodeConfig.setDhcp(true);
//                nodeConfig.setDhcpNetworkAddress(network.getDhcpNetworkAddress());
//                nodeConfig.setDhcpNetworkMask(network.getDhcpNetworkMask());
//                nodeConfig.setHwAddress(NetworkUtils.getRandomHWAddress());
//
//                String dhcpId = eaasClient.getEaasWSPort(eaasGw).createSession(nodeConfig.value(false));
//                sessions.addComponent(session, dhcpId);
//
//                Map<String, URI> controlUrls = ComponentClient.controlUrlsToMap(componentClient.getComponentPort(eaasGw).getControlUrls(dhcpId));
//                String dhcpUrl = controlUrls.get("ws+ethernet+" + nodeConfig.getHwAddress()).toString();
//                componentClient.getNetworkSwitchPort(eaasGw).connect(switchId, dhcpUrl);
//            }

            if(networkRequest.isTcpGateway() && networkRequest.getTcpGatewayConfig() != null) {
                String nodeTcpId = null;
                NetworkRequest.TcpGatewayConfig tcpGatewayConfig = networkRequest.getTcpGatewayConfig();

                NodeTcpConfiguration nodeConfig = new NodeTcpConfiguration();
                nodeConfig.setHwAddress(NetworkUtils.getRandomHWAddress());

                if(tcpGatewayConfig.isSocks())
                {
                    nodeConfig.setSocksMode(true);
                    //      nodeConfig.setSocksUser("eaas");
                    //      nodeConfig.setSocksPasswd("bwfla");
                }
                else {
                    if(tcpGatewayConfig.getServerIp() == null || tcpGatewayConfig.getServerPort() == null)
                        throw new BWFLAException("invalid server/gateway config");

                    nodeConfig.setDestIp(tcpGatewayConfig.getServerIp());
                    nodeConfig.setDestPort(tcpGatewayConfig.getServerPort());
                }

                final NodeTcpComponentRequest nodeComponentRequest = new NodeTcpComponentRequest();
                nodeComponentRequest.setConfig(nodeConfig);
                nodeTcpId = components.createComponent(nodeComponentRequest).getId();
                session.components()
                        .put(nodeTcpId, new SessionComponent(nodeTcpId));

                Map<String, URI> controlUrls = ComponentClient.controlUrlsToMap(componentWsClient.getControlUrls(nodeTcpId));
                String nodeTcpUrl = controlUrls.get("ws+ethernet+" + nodeConfig.getHwAddress()).toString();
                networkSwitchWsClient.connect(switchId, nodeTcpUrl);

                String nodeInfoUrl = controlUrls.get("info").toString();
                System.out.println(nodeInfoUrl);
                networkResponse.addUrl("tcp", URI.create(nodeInfoUrl));
            }

            // add all the other components
            for (NetworkRequest.ComponentSpec component : networkRequest.getComponents()) {
                this.connect(session, component);
            }

            LOG.info("Created network '" + session.id() + "'");
            return Response.status(Response.Status.CREATED)
                    .entity(networkResponse)
                    .build();
        }
        catch (Exception error) {
            LOG.log(Level.WARNING, "Creating network failed!", error);
            throw Components.newInternalError(error);
        }
    }

    @POST
    @Secured(roles = {Role.PUBLIC})
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}/components")
    public void addComponent(@PathParam("id") String id, NetworkRequest.ComponentSpec component) {
        final var network = this.lookup(id);
        this.connect(network, component);
    }

    @POST
    @Secured(roles = {Role.PUBLIC})
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}/addComponentToSwitch")
    public void addComponentToSwitch(@PathParam("id") String id, NetworkRequest.ComponentSpec component) {
        final var network = this.lookup(id);
        this.connect(network, component);
    }

    @POST
    @Secured(roles = {Role.RESTRICTED})
    @Path("/{id}/components/{componentId}/disconnect")
    public void disconnectComponent(@PathParam("id") String id, @PathParam("componentId") String componentId) {
        final var network = this.lookup(id);
        final var component = network.component(componentId);
        try {
            this.disconnect(network, component);
        }
        catch (BWFLAException e)
        {
            LOG.log(Level.WARNING, "Disconnecting component '" + componentId + "' from network '" + id + "' failed!", e);
            throw new ServerErrorException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorInformation(
                            "Could not disconnect component: " + componentId , e.getMessage()))
                    .build());
        }
    }

    @DELETE
    @Secured(roles = {Role.RESTRICTED})
    @Path("/{id}/components/{componentId}")
    public void removeComponent(@PathParam("id") String id, @PathParam("componentId") String componentId) {
        final var network = this.lookup(id);
        final var component = network.component(componentId);
        this.remove(network, component);
    }

    @GET
    @Secured(roles = {Role.PUBLIC})
    @Path("/{id}/wsConnection")
    @Produces(MediaType.APPLICATION_JSON)
    public Response wsConnection(@PathParam("id") String id)
    {
        try {
            final var network = this.lookup(id);
            final var switchId = network.getSwitchId();
            String link = networkSwitchWsClient.wsConnect(switchId);
            final JsonObject json = Json.createObjectBuilder()
                    .add("wsConnection", link)
                    .add("ok", true)
                    .build();

            return Emil.createResponse(Response.Status.OK, json.toString());
        } catch (BWFLAException e) {
            LOG.log(Level.WARNING, "Connecting to network '" + id + "' failed!", e);
            throw new ServerErrorException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorInformation(
                            "Could not find switch for session: " + id , e.getMessage()))
                    .build());
        }
    }

    private void connect(NetworkSession network, NetworkRequest.ComponentSpec cspec)
    {
        final var nid = network.id();
        final var cid = cspec.getComponentId();
        try {
            final Map<String, URI> map = this.getControlUrls(cid);

            URI uri;
            if (cspec.getHwAddress().equals("auto")) {
                uri = map.entrySet().stream()
                        .filter(e -> e.getKey().startsWith("ws+ethernet+"))
                        .findAny()
                        .orElseThrow(() -> new InternalServerErrorException(
                                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity(new ErrorInformation(
                                                "Cannot find suitable ethernet URI for requested component.",
                                                "Requested component has either been stopped or is not suitable for networking"))
                                        .build()))
                        .getValue();
            }
            else {
                uri = map.get("ws+ethernet+" + cspec.getHwAddress());
            }

            if(uri == null) {
                throw new ServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorInformation(
                                "Cannot find suitable ethernet URI for requested component.",
                                "Requested component has either been stopped or is not suitable for networking"))
                        .build());
            }

            this.connect(network, cid, uri.toString());
        }
        catch (BWFLAException error) {
            LOG.log(Level.WARNING, "Connecting component '" + cid + "' to network '" + nid + "' failed!", error);
            throw new ServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorInformation("Could not acquire group information.", error.getMessage()))
                    .build());
        }
    }

    private SessionComponent connect(NetworkSession network, String cid, String ethurl) throws BWFLAException
    {
        networkSwitchWsClient.connect(network.getSwitchId(), ethurl);

        final var component = new SessionComponent(cid);
        component.getNetworkConnectionInfo()
                .setEthernetUrl(ethurl);

        network.components()
                .put(cid, component);

        final var nid = network.id();

        final TaskStack.IRunnable cleanup = () -> {
            try {
                this.remove(network, component);
            }
            catch (Exception error) {
                final var message = "Disconnecting component '" + cid + "' from network '" + nid + "' failed!";
                LOG.log(Level.WARNING, message, error);
            }
        };

        components.registerCleanupTask(cid, "network-disconnect/" + nid, cleanup);
        LOG.info("Connected component '" + cid + "' to network '" + nid + "'");

        return component;
    }

    private void disconnect(NetworkSession network, SessionComponent component) throws BWFLAException
    {
        final SessionComponent.NetworkConnectionInfo netinfo;
        synchronized (component) {
            netinfo = component.getNetworkConnectionInfo();
            if (!netinfo.isConnected())
                return;

            // try to disconnect from network only once...
            component.resetNetworkConnectionInfo();
        }

        networkSwitchWsClient.disconnect(network.getSwitchId(), netinfo.getEthernetUrl());
        LOG.info("Disconnected component '" + component.id() + "' from network '" + network.id() + "'");
    }

    private void remove(NetworkSession network, SessionComponent component)
    {
        final var nid = network.id();
        final var cid = component.id();

        try {
            this.disconnect(network, component);
        }
        catch (BWFLAException error) {
            LOG.log(Level.WARNING, "Disconnecting component '" + cid + "' from network '" + nid + "' failed!", error);
        }

        sessions.remove(nid, cid);

        LOG.info("Removed component '" + cid + "' from network '" + nid + "'");
    }

    private Map<String, URI> getControlUrls(String componentId) throws BWFLAException {
        return ComponentClient.controlUrlsToMap(componentWsClient.getControlUrls(componentId));
    }

    private NetworkSession lookup(String id) throws NotFoundException
    {
        final Session session = sessions.get(id);
        if (session == null || !(session instanceof NetworkSession))
            throw new NotFoundException("Network not found: " + id);

        return (NetworkSession) session;
    }
    }
}

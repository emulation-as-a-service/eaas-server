package de.bwl.bwfla.automation.client.sikuli;

import de.bwl.bwfla.automation.api.sikuli.*;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.logging.Logger;


public class SikuliClient
{
	private final WebTarget target;

	private static final Logger LOG = Logger.getLogger("SIKULI-CLIENT");


	public SikuliClient(URI sikuliConnectorAdress)
	{
		target = ClientBuilder.newClient().target(sikuliConnectorAdress);
	}

	public Response executeSikuliScript(SikuliExecutionRequest request)
	{

		LOG.info("Sending execute request to sikuli component backend on emucomp.");
		return sikuliPostRequest(request, "/execute");
	}

	public Response downloadSikuliScript(SikuliDownloadRequest request)
	{
		LOG.info("Sending download request to sikuli component backend on emucomp.");

		return sikuliPostRequest(request, "/downloads");
	}

	public Response uploadSikuliScript(SikuliUploadRequest request)
	{

		LOG.info("Sending upload request to sikuli component backend on emucomp.");

		return sikuliPostRequest(request, "/uploads");
	}

	public SikuliLogResponse getSikuliLogs()
	{
		LOG.info("Sending log request to sikuli component backend on emucomp.");
		var targetRequest = target.path("/logs").request();
		var response = targetRequest.get();
		return response.readEntity(SikuliLogResponse.class);
	}


	private Response sikuliPostRequest(SikuliRequest request, String urlPath)
	{
		if (!urlPath.startsWith("/")) {
			urlPath = "/" + urlPath;
		}
		var targetRequest = target.path(urlPath).request();
		var response = targetRequest.post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));

		return response;
	}

}
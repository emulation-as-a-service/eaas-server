package com.openslx.eaas.automation.client.sikuli;

import com.openslx.eaas.automation.api.sikuli.*;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;

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

	public ProcessResultUrl downloadSikuliScript(SikuliDownloadRequest request)
	{
		LOG.info("Sending download request to sikuli component backend on emucomp.");

		var resp = sikuliPostRequest(request, "/downloads");
		return resp.readEntity(ProcessResultUrl.class);
	}

	public Response uploadSikuliScript(SikuliUploadRequest request)
	{

		LOG.info("Sending upload request to sikuli component backend on emucomp.");

		return sikuliPostRequest(request, "/uploads");
	}

	public Response getLatestScreenshot()
	{
		LOG.info("Sending screenshot request to sikuli component backend on emucomp.");

		var targetRequest = target.path("/screenshot").request();
		return targetRequest.get();
	}

	public SikuliLogResponse getSikuliLogs()
	{
		LOG.info("Sending log request to sikuli component backend on emucomp.");
		var targetRequest = target.path("/logs").request();
		var response = targetRequest.get();
		return response.readEntity(SikuliLogResponse.class);
	}

	public ProcessResultUrl getDebugURL()
	{
		LOG.info("Sending debug request to sikuli component backend on emucomp.");
		var targetRequest = target.path("/debug").request();
		var response = targetRequest.get();
		return response.readEntity(ProcessResultUrl.class);
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
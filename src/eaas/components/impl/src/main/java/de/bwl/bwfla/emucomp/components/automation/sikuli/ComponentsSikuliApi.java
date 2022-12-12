package de.bwl.bwfla.emucomp.components.automation.sikuli;


import com.openslx.eaas.automation.api.sikuli.SikuliExecutionRequest;
import com.openslx.eaas.automation.api.sikuli.SikuliLogResponse;
import com.openslx.eaas.automation.api.sikuli.SikuliUploadRequest;
import com.openslx.eaas.automation.impl.sikuli.SikuliEmucompTasks;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;


//blocking, Tasks auf gateway
//TODO packages zu Openslx
//TODO check if component exists!
@ApplicationScoped //emucomp/sikuli/
@Path("/components")
public class ComponentsSikuliApi
{
	private static final Logger LOG = Logger.getLogger("SIKULI-EMUCOMP-API");
	private final java.nio.file.Path automationBasePath = java.nio.file.Path.of("/tmp-storage/emucomp-automation");


	public ComponentsSikuliApi()
	{
		try {
			setupDirectories();
		}
		catch (Exception e) {
			throw new RuntimeException("Could not set up directories on emucomp.");
		}
	}

	private void setupDirectories() throws Exception
	{

		if (Files.notExists(automationBasePath)) {
			Files.createDirectory(automationBasePath);
			Files.createDirectory(automationBasePath.resolve("sikuli"));
		}

		else {
			DeprecatedProcessRunner pr = new DeprecatedProcessRunner("sudo");
			pr.addArguments("rm", "-rf", "/tmp-storage/emucomp-automation");
			if (!pr.execute(true))
				throw new BWFLAException("Could not delete automation dir contents!");
			var p = new File("/tmp-storage/emucomp-automation");

			Files.createDirectory(p.toPath());
			Files.createDirectory(automationBasePath.resolve("sikuli"));
		}
	}


	@POST
	@Path("/{componentId}/sikuli/execute")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postExecute(@PathParam("componentId") String componentId, SikuliExecutionRequest request) throws Exception
	{
		LOG.info("Got request to execute sikuli task for component " + componentId);
		SikuliEmucompTasks.executeSikuliScriptInEmulatorContainer(componentId, request);
		//return SikuliEmucompTasks.getDebugFiles(componentId);
		return Response.ok().build();

	}

	@POST
	@Path("/{componentId}/sikuli/downloads")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ProcessResultUrl postDownload(@PathParam("componentId") String componentId) throws BWFLAException
	{
		LOG.info("Got request to download sikuli files for component " + componentId);
		return SikuliEmucompTasks.downloadCurrentSikuliScriptFromEmulatorContainer(componentId);
	}

	@POST
	@Path("/{componentId}/sikuli/uploads")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postUpload(@PathParam("componentId") String componentId, SikuliUploadRequest request) throws BWFLAException
	{
		LOG.info("Got request to upload sikuli files for component " + componentId);

		SikuliEmucompTasks.uploadSikuliScriptToEmulatorContainer(componentId, request.getBlobStoreUrl());
		return Response.ok().build();
	}

	@GET
	@Path("/{componentId}/sikuli/logs")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public SikuliLogResponse getLogfiles(@PathParam("componentId") String componentId) throws Exception
	{
		LOG.info("Got request for sikuli log files for component " + componentId);
		return SikuliEmucompTasks.getLogFiles(componentId);
	}

	@GET
	@Path("/{componentId}/sikuli/debug")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public ProcessResultUrl getDebugInfo(@PathParam("componentId") String componentId) throws Exception
	{
		LOG.info("Got request for sikuli debug files for component " + componentId);
		return SikuliEmucompTasks.getDebugURL(componentId);
	}

	//TODO for all, check if component is running here? Needs to be checked somewhere (is checked in client, but that is not enough)
	@GET
	@Path("/{componentId}/sikuli/screenshot")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLatestScreenshot(@PathParam("componentId") String componentId) throws Exception
	{
		LOG.info("Got request for latest sikuli screenshot for component " + componentId);
		return SikuliEmucompTasks.getLastScreenshot(componentId);
	}

}


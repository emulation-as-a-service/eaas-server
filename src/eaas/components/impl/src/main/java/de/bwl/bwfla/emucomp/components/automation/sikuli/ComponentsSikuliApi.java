package de.bwl.bwfla.emucomp.components.automation.sikuli;


import de.bwl.bwfla.automation.api.sikuli.SikuliExecutionRequest;
import de.bwl.bwfla.automation.api.sikuli.SikuliUploadRequest;
import de.bwl.bwfla.automation.impl.sikuli.SikuliEmucompTasks;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;


//blocking, Tasks auf gateway
//TODO packages zu Openslx
//TODO check if component exists!
@ApplicationScoped //emucomp/sikuli/
@Path("/components")
public class ComponentsSikuliApi
{
	private static final Logger LOG = Logger.getLogger("SIKULI-EMUCOMP-API");

	@POST
	@Path("/{componentId}/sikuli/execute")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postExecute(@PathParam("componentId") String componentId, SikuliExecutionRequest request) throws Exception
	{
		LOG.info("Got request to execute sikuli task for component " + componentId);
		SikuliEmucompTasks.executeSikuliScriptInEmulatorContainer(componentId, request, "1");
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


}


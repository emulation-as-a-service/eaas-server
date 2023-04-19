package de.bwl.bwfla.sikuli;

import com.openslx.eaas.automation.api.sikuli.SikuliCreateScriptRequest;
import com.openslx.eaas.automation.api.sikuli.SikuliDownloadRequest;
import com.openslx.eaas.automation.api.sikuli.SikuliExecutionRequest;
import com.openslx.eaas.automation.api.sikuli.SikuliUploadRequest;
import de.bwl.bwfla.apiutils.WaitQueueResponse;

import com.openslx.eaas.automation.client.sikuli.SikuliClient;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;
import de.bwl.bwfla.common.datatypes.WaitQueueUserData;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import de.bwl.bwfla.common.taskmanager.TaskInfo;
import de.bwl.bwfla.common.utils.apiutils.WaitQueueCreatedResponse;
import de.bwl.bwfla.emil.Components;
import de.bwl.bwfla.emil.datatypes.rest.ComponentStateResponse;
import de.bwl.bwfla.emucomp.api.ComponentState;
import de.bwl.bwfla.restutils.ResponseUtils;
import de.bwl.bwfla.sikuli.task.CreateSikuliScriptTask;
import de.bwl.bwfla.sikuli.task.DownloadSikuliScriptTask;
import de.bwl.bwfla.sikuli.task.ExecuteSikuliTask;
import de.bwl.bwfla.sikuli.task.UploadSikuliScriptTask;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
@Path("/sikuli/api/v1") //FIXME directory needs to be cleaned upon startup, on emucomp as well!!!
public class SikuliApi
{
	private static final Logger LOG = Logger.getLogger("SIKULI-GATEWAY-API");

	@Inject
	private Components components;

	private final TaskManager taskmgr;

	public SikuliApi() throws BWFLAException
	{
		try {
			this.taskmgr = new TaskManager();
		}
		catch (Exception error) {
			throw new BWFLAException("Initializing Sikuli API failed!", error);
		}
	}

	//TODO rename to something like create?
	@POST
	@Path("/scripts")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postBuild(SikuliCreateScriptRequest request, @Context UriInfo uri)
	{

		final String taskID;
		try {
			taskID = taskmgr.submit(new CreateSikuliScriptTask(request));
		}
		catch (Throwable throwable) {
			LOG.log(Level.WARNING, "Starting the Task failed!", throwable);
			return ResponseUtils.createInternalErrorResponse(throwable);
		}

		return createWaitQueue(uri, taskID, "scripts");

	}

	@POST
	@Path("/execute")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postExecute(SikuliExecutionRequest request, @Context UriInfo uri)
	{

		final String taskID;
		try {
			taskID = taskmgr.submit(new ExecuteSikuliTask(request, getSikuliClient(request.getComponentId())));
		}
		catch (Throwable throwable) {
			LOG.log(Level.WARNING, "Starting the Task failed!", throwable);
			return ResponseUtils.createInternalErrorResponse(throwable);
		}

		return createWaitQueue(uri, taskID, "execute");

	}

	@POST
	@Path("/downloads")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postDownload(SikuliDownloadRequest request, @Context UriInfo uri)
	{
		final String taskID;
		try {
			taskID = taskmgr.submit(new DownloadSikuliScriptTask(request, getSikuliClient(request.getComponentId())));
		}
		catch (Throwable throwable) {
			LOG.log(Level.WARNING, "Starting the Task failed!", throwable);
			return ResponseUtils.createInternalErrorResponse(throwable);
		}

		return createWaitQueue(uri, taskID, "downloads");
	}

	@POST
	@Path("/uploads")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postUpload(SikuliUploadRequest request, @Context UriInfo uri)
	{

		final String taskID;
		try {
			taskID = taskmgr.submit(new UploadSikuliScriptTask(request, getSikuliClient(request.getComponentId())));
		}
		catch (Throwable throwable) {
			LOG.log(Level.WARNING, "Starting the Task failed!", throwable);
			return ResponseUtils.createInternalErrorResponse(throwable);
		}

		return createWaitQueue(uri, taskID, "uploads");
	}


	@GET
	@Path("/logs/tasks/{taskId}")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.TEXT_PLAIN)
	public Response getLogsForTaskId(@PathParam("taskId") String taskId) throws Exception
	{

		return getSikuliLogsForTaskId(taskId);
	}

	@GET
	@Path("/debug/tasks/{taskId}")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public ProcessResultUrl getDebugInfoForTaskId(@PathParam("taskId") String taskId)
	{

		return SikuliUtils.getDebugInfoForTaskId(taskId);
	}

	@GET
	@Path("/waitqueue/{id}")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response poll(@PathParam("id") String id)
	{
		try {
			final TaskInfo<Object> info = taskmgr.lookup(id);
			if (info == null) {
				String message = "Passed ID is invalid: " + id;
				return ResponseUtils.createMessageResponse(Response.Status.NOT_FOUND, message);
			}

			Response.Status status = Response.Status.OK;
			final WaitQueueUserData userdata = info.userdata(WaitQueueUserData.class);

			WaitQueueResponse response = new WaitQueueResponse();
			response.setId(id);
			response.setResultUrl(userdata.getResultLocation());


			if (info.result().isCompletedExceptionally()) {
				//TODO this does not always trigger on error - fix python script
				response.setHasError(true);
				response.setStatus("Error");
			}

			else {
				response.setHasError(false);
				if (info.result().isDone()) {
					// Result is available!
					response.setStatus("Done");
					response.setDone(true);
				}
				else {
					// Result is not yet available!
					response.setStatus("Processing");
					response.setDone(false);
				}
			}


			return ResponseUtils.createResponse(status, response);
		}
		catch (Throwable throwable) {
			return ResponseUtils.createInternalErrorResponse(throwable);
		}
	}

	@GET
	@Path("/downloads/{id}")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDownloadResult(@PathParam("id") String id)
	{
		return getResponse(id);
	}

	@GET
	@Path("/uploads/{id}")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUploadResult(@PathParam("id") String id)
	{
		return getResponse(id);
	}

	@GET
	@Path("/scripts/{id}")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getScriptResult(@PathParam("id") String id)
	{
		return getResponse(id);
	}

	@GET
	@Path("/execute/{id}")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getExecutionResult(@PathParam("id") String id)
	{
		return getResponse(id);
	}


	//TODO generalize this so that it can be used in multiple APIs (img proposer, sikuli, cwl, automation)
	private Response createWaitQueue(UriInfo uri, String taskID, String upload)
	{
		final String waitLocation = SikuliApi.getLocationUrl(uri, "waitqueue", taskID);
		final String resultLocation = SikuliApi.getLocationUrl(uri, upload, taskID);
		final TaskInfo<Object> swhInfo = taskmgr.lookup(taskID);
		swhInfo.setUserData(new WaitQueueUserData(waitLocation, resultLocation));

		final WaitQueueCreatedResponse response = new WaitQueueCreatedResponse();
		response.setTaskId(taskID);
		response.setWaitQueueUrl(waitLocation);

		return ResponseUtils.createLocationResponse(Response.Status.ACCEPTED, waitLocation, response);
	}

	private Response getResponse(String id)
	{
		try {
			if (id == null || id.isEmpty()) {
				String message = "ID was not specified or is invalid!";
				return ResponseUtils.createMessageResponse(Response.Status.BAD_REQUEST, message);
			}

			final TaskInfo<Object> info = taskmgr.lookup(id);
			if (info == null) {
				String message = "Passed ID is invalid: " + id;
				return ResponseUtils.createMessageResponse(Response.Status.NOT_FOUND, message);
			}

			if (!info.result().isDone()) {
				String message = "This task with id " + id + " is still being processed.";
				return ResponseUtils.createMessageResponse(Response.Status.OK, message);
			}

			try {
				// Result is available!
				final Future<Object> future = info.result();
				return ResponseUtils.createResponse(Response.Status.OK, future.get());
			}
			finally {
				taskmgr.remove(id);
			}
		}
		catch (Throwable throwable) {
			return ResponseUtils.createInternalErrorResponse(throwable);
		}
	}


	private SikuliClient getSikuliClient(String componentId)
	{
		//TODO check if componentId is active
		try {
			var controlUrls = components.getControlUrls(componentId);
			URI sikuliURI;

			sikuliURI = controlUrls.get("sikuli");
			if (null == sikuliURI) {
				throw new RuntimeException();
			}
			return new SikuliClient(sikuliURI);
		}
		catch (Exception e) {
			throw new NotFoundException("Could not get sikuli Control URL for component: " + componentId);
		}

	}

	public Response getSikuliLogsForTaskId(String taskId) throws Exception
	{
		var basePath = java.nio.file.Path.of("/tmp-storage/automation/sikuli").resolve(taskId);
		if (Files.notExists(basePath)) {
			throw new NotFoundException("Could not find directory for taskId " + taskId);
		}
		if (Files.notExists(basePath.resolve("logs.txt"))) {
			LOG.info("Log path does not exist, trying to redirect to component stream log!");
			//task is created but still running, as logs.txt is only written upon task completion
			//this means we need to grab the logs from the component directly

			if (Files.notExists(basePath.resolve("component.txt"))) {
				throw new NotFoundException("No component was found for sikuli task with id " + taskId);
			}
			var componentId = Files.readString(basePath.resolve("component.txt"));

			String componentState = ((ComponentStateResponse) components.getState(componentId)).getState();

			if (!componentState.equals(ComponentState.RUNNING.name())) {
				throw new NotFoundException("Component is not running, can't access real time logs for task with id " + taskId);
			}

			var urls = components.getControlUrls(componentId);
			var logurl = urls.get("sikulilog");
			return Response.temporaryRedirect(logurl).build();
		}
		else {
			LOG.info("Log path does exist, returning contents");
			try {
				String logText = Files.readString(basePath.resolve("logs.txt"));
				return Response.ok(logText).build();
			}
			catch (IOException e) {
				throw new InternalServerErrorException("Could not read file, although log file is present");
			}

		}
	}


	private static String getLocationUrl(UriInfo uri, String subres, String id)
	{
		return ResponseUtils.getLocationUrl(SikuliApi.class, uri, subres, id);
	}


	private static class TaskManager extends de.bwl.bwfla.common.taskmanager.TaskManager<Object>
	{
		public TaskManager() throws NamingException
		{
			super("SIKULI-TASKS", InitialContext.doLookup("java:jboss/ee/concurrency/executor/io"));
		}
	}
}
package de.bwl.bwfla.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bwl.bwfla.apiutils.WaitQueueCreatedResponse;
import de.bwl.bwfla.apiutils.WaitQueueResponse;
import de.bwl.bwfla.automation.api.AllAutomationsResponse;
import de.bwl.bwfla.automation.api.AutomationConfigurationRequest;
import de.bwl.bwfla.automation.api.AutomationRequest;
import de.bwl.bwfla.automation.impl.AutomationTask;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import de.bwl.bwfla.common.taskmanager.TaskInfo;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.envproposer.impl.UserData;
import de.bwl.bwfla.restutils.ResponseUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
@Path("/automation/api/v1")
public class AutomationAPI
{
	private static final Logger LOG = Logger.getLogger("AUTOMATION");

	private final TaskManager taskmgr;

	private final ArrayList<String> automations;

	public AutomationAPI() throws BWFLAException, IOException
	{
		try {
			this.taskmgr = new TaskManager();
		}
		catch (Exception error) {
			throw new BWFLAException("Initializing Automation API failed!", error);
		}

		automations = new ArrayList<>();

		java.nio.file.Path path = java.nio.file.Path.of("/tmp-storage/automation");
		if (Files.notExists(path)) {
			Files.createDirectory(path);
			//Files.createDirectory(path.resolve("sikuli"));
		}

		else {
			DeprecatedProcessRunner pr = new DeprecatedProcessRunner("sudo");
			pr.addArguments("rm", "-rf", "/tmp-storage/automation");
			if (!pr.execute(true))
				throw new BWFLAException("Could not delete automation dir contents!");
			var p = new File("/tmp-storage/automation");

			Files.createDirectory(p.toPath());
		}
		//FileUtils.cleanDirectory(new File("/tmp-storage/automation"));

	}



	@GET
	@Path("/automations")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllAutomations(@Context UriInfo uri) throws IOException
	{

		System.out.println(uri.getQueryParameters());
		System.out.println(uri.getPathParameters());



		ArrayList<AllAutomationsResponse.AutomationResult> resultList = new ArrayList<>();
		for (String taskId : automations) {

			final TaskInfo<Object> info = taskmgr.lookup(taskId);
			if (info == null) {
				LOG.warning("Could not find task " + taskId);
				continue;
			}


			var configData =
					new ObjectMapper().readValue(new File("/tmp-storage/automation/" + taskId + "/config.json"), AutomationRequest.class);


			AllAutomationsResponse.AutomationResult result = new AllAutomationsResponse.AutomationResult();

			if (info.result().isCompletedExceptionally()) {
				result.setError(true);
				result.setStatus("Error");
				result.setResult("Error");

			}
			else if (info.result().isDone()) {
				// Result is available!
				result.setStatus("Done");
				result.setIsDone(true);
			}
			else {
				// Result is not yet available!
				result.setStatus("Processing");
				result.setIsDone(false);
				result.setResult("---");
			}


			File resultFile = new File("/tmp-storage/automation/" + taskId + "/status.json");

			result.setId(taskId);
			result.setAutomationType(configData.getAutomationType());
			result.setExecutable(configData.getExecutableLocation());
			result.setOriginalEnvId(configData.getEnvironmentId());

			if (resultFile.exists()) {

				var pyResult =
						new ObjectMapper().readValue(resultFile, AllAutomationsResponse.PythonResultFile.class);
				//result.setIsDone(true);
				result.setResult(pyResult.getResult());
			}

			resultList.add(result);

		}

		AllAutomationsResponse response = new AllAutomationsResponse();
		response.setResults(resultList);

		return Response.ok(response).build();
	}


	@POST
	@Path("/config")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postConfiguration(AutomationConfigurationRequest request, @Context UriInfo uri)
	{
		ObjectMapper mapper = new ObjectMapper();
		String configName = "/tmp-storage/automation/automation_config.json";

		try {
			mapper.writeValue(new File(configName), request);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		return Response.ok().build();
	}

	@GET
	@Path("/reset")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response poll() throws BWFLAException, IOException
	{

		//FileUtils.cleanDirectory(new File("/tmp-storage/automation"));
//			var p = new File("/tmp-storage/automation");
//			FileUtils.deleteDirectory(p);
//			Files.createDirectory(p.toPath());
		DeprecatedProcessRunner pr = new DeprecatedProcessRunner("sudo");
		pr.addArguments("rm", "-rf", "/tmp-storage/automation");
		if (!pr.execute(true))
			throw new BWFLAException("Could not delete automation dir contents!");
		var p = new File("/tmp-storage/automation");

		Files.createDirectory(p.toPath());

		automations.clear();

		return Response.ok().build();
	}

	@POST
	@Path("/execute")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postExecute(AutomationRequest request, @Context UriInfo uri)
	{




		final String taskID;
		try {
			taskID = taskmgr.submit(new AutomationTask(request));

			automations.add(taskID);
		}
		catch (Throwable throwable) {
			LOG.log(Level.WARNING, "Starting the Task failed!", throwable);
			return ResponseUtils.createInternalErrorResponse(throwable);
		}

		return createWaitQueue(uri, taskID, "executions");


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
			final UserData userdata = info.userdata(UserData.class);

			WaitQueueResponse response = new WaitQueueResponse();
			response.setId(id);
			response.setResultUrl(userdata.getResultLocation());

			if (info.result().isCompletedExceptionally()) {
				response.setHasError(true);
				response.setStatus("Error");
			}
			else if (info.result().isDone()) {
				// Result is available!
				response.setStatus("Done");
				response.setDone(true);
			}
			else {
				// Result is not yet available!
				response.setStatus("Processing");
				response.setDone(false);
			}
			return ResponseUtils.createResponse(status, response);
		}
		catch (Throwable throwable) {
			return ResponseUtils.createInternalErrorResponse(throwable);
		}
	}

	@GET
	@Path("/executions/{id}")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDownloadResult(@PathParam("id") String id)
	{
		return getResponse(id);
	}

	private Response createWaitQueue(UriInfo uri, String taskID, String upload)
	{
		final String waitLocation = AutomationAPI.getLocationUrl(uri, "waitqueue", taskID);
		final String resultLocation = AutomationAPI.getLocationUrl(uri, upload, taskID);
		final TaskInfo<Object> swhInfo = taskmgr.lookup(taskID);
		swhInfo.setUserData(new UserData(waitLocation, resultLocation));

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


	private static String getLocationUrl(UriInfo uri, String subres, String id)
	{
		return ResponseUtils.getLocationUrl(AutomationAPI.class, uri, subres, id);
	}

	private static class TaskManager extends de.bwl.bwfla.common.taskmanager.TaskManager<Object>
	{
		public TaskManager() throws NamingException
		{
			super("AUTOMATION-TASKS", InitialContext.doLookup("java:jboss/ee/concurrency/executor/io"));
		}
	}
}

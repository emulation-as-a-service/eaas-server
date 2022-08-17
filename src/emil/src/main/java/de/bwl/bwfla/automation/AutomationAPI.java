package de.bwl.bwfla.automation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import de.bwl.bwfla.api.blobstore.BlobStore;
import de.bwl.bwfla.apiutils.WaitQueueCreatedResponse;
import de.bwl.bwfla.apiutils.WaitQueueResponse;
import de.bwl.bwfla.automation.api.*;
import de.bwl.bwfla.automation.impl.AutomationTask;
import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.security.AuthenticatedUser;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import de.bwl.bwfla.common.services.security.UserContext;
import de.bwl.bwfla.common.taskmanager.TaskInfo;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.emil.datatypes.rest.ProcessResultUrl;
import de.bwl.bwfla.envproposer.impl.UserData;
import de.bwl.bwfla.restutils.ResponseUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
@Path("/automation/api/v1")
public class AutomationAPI
{

	@Inject
	@AuthenticatedUser
	private UserContext authenticatedUser;

	private static final Logger LOG = Logger.getLogger("AUTOMATION");

	private final TaskManager taskmgr;

	private ArrayList<String> automations;

	private final java.nio.file.Path automationBasePath = java.nio.file.Path.of("/tmp-storage/automation");

	public AutomationAPI() throws BWFLAException, IOException
	{
		try {
			this.taskmgr = new TaskManager();
		}
		catch (Exception error) {
			throw new BWFLAException("Initializing Automation API failed!", error);
		}
		resetAutomations();
	}

	@DELETE
	@Path("/automations")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response clearAutomations(@Context UriInfo uri) throws IOException, BWFLAException
	{
		resetAutomations();
		return Response.ok().build();
	}

	@GET
	@Path("/automations")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllAutomations(@Context UriInfo uri) throws IOException, ExecutionException, InterruptedException
	{

		ArrayList<AllAutomationsResponse.AutomationResult> resultList = new ArrayList<>();
		for (String taskId : automations) {

			final TaskInfo<Object> info = taskmgr.lookup(taskId);

			if (info == null) {
				LOG.warning("Could not find task " + taskId);
				continue;
			}

			var mapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS)
					.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.registerModule(new JaxbAnnotationModule());

			AllAutomationsResponse.AutomationResult result = new AllAutomationsResponse.AutomationResult();
			result.setId(taskId);

			Instant startTime;
			Instant endTime;

			var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

			if ((startTime = info.task().getStartTime()) != null) {
				result.setStartedAt(formatter.format(startTime));

				if ((endTime = info.task().getEndTime()) != null) {
					result.setFinishedAt(formatter.format(endTime));
					Duration duration = Duration.between(startTime, endTime);
					result.setDuration(DurationFormatUtils.formatDuration(duration.toMillis(), "HH:mm:ss", true));
				}
				else {
					result.setFinishedAt("---");
					Duration duration = Duration.between(startTime, Instant.now());
					result.setDuration(DurationFormatUtils.formatDuration(duration.toMillis(), "HH:mm:ss", true));
				}
			}

			boolean readSuccessfully = false;

			try {

				AutomationTemplateRequest configData = mapper.readValue(new File("/tmp-storage/automation/" + taskId + "/config.json"), AutomationTemplateRequest.class);
				LOG.fine("Successfully read AutomationTemplateRequest for task " + taskId);
				readSuccessfully = true;

				result.setAutomationType(configData.getAutomationType());
				result.setExecutable(configData.getExecutableLocation());
				result.setOriginalEnvId(configData.getTargetId());
				result.setName(configData.getName());

			}
			catch (Exception e) {
				LOG.fine("Could not read config file as AutomationTemplateRequest, will try AutomationSikuliRequest ...");
			}

			if (!readSuccessfully) {
				try {
					AutomationSikuliRequest configData2 = mapper.readValue(new File("/tmp-storage/automation/" + taskId + "/config.json"), AutomationSikuliRequest.class);
					LOG.fine("Successfully read AutomationSikuliRequest for task " + taskId);
					result.setOriginalEnvId(configData2.getTargetId());
					result.setName(configData2.getName());

				}
				catch (Exception e) {
					LOG.warning("Could not read config file for task " + taskId + " as AutomationTemplateRequest or AutomationSikuliRequest!");
				}
			}

			boolean resultSet = false;

			if (info.result().isCompletedExceptionally()) {
				result.setError(true);
				result.setStatus("Error");
				result.setResult("Error");

			}
			else if (info.result().isDone()) {
				// Result is available!
				result.setStatus("Done");
				result.setIsDone(true);
				result.setResult((String) info.result().get());
				resultSet = true;
			}
			else {
				// Result is not yet available!
				result.setStatus("Processing");
				result.setIsDone(false);
				result.setResult("---");
			}

			//TODO read this in actual automation task ?
			File resultFile = new File("/tmp-storage/automation/" + taskId + "/status.json");
			if (resultFile.exists()) {

				var pyResult =
						new ObjectMapper().readValue(resultFile, AllAutomationsResponse.PythonResultFile.class);
				if (!resultSet) {
					result.setResult(pyResult.getResult());
				}
				if (pyResult.isHasError()) {
					result.setStatus("Error");
					result.setResult("Error");
					result.setError(true);
				}

				if (null != pyResult.getSikuliTaskId()) {
					result.setSikuliTaskId(pyResult.getSikuliTaskId());

					var logFilePath = java.nio.file.Path.of("/tmp-storage/automation/sikuli").resolve(pyResult.getSikuliTaskId()).resolve("logs.txt");

					if (Files.exists(logFilePath)) {
						ArrayList<String> lines = (ArrayList<String>) Files.readAllLines(logFilePath);
						result.setLogs(lines);
					}
				}
			}
			resultList.add(result);
		}

		AllAutomationsResponse response = new AllAutomationsResponse();
		response.setResults(resultList);

		return Response.ok(response).build();
	}

	@GET
	@Path("/debug/{id}")
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_JSON)
	public ProcessResultUrl getSikuliDebugScreenshots(@PathParam("id") String id) throws BWFLAException
	{
		DeprecatedProcessRunner tarRunner = new DeprecatedProcessRunner("tar");
		tarRunner.setWorkingDirectory(automationBasePath.resolve("sikuli"));
		tarRunner.addArguments("-zcvf", "sikuliDebug" + id + ".tgz", id);
		tarRunner.execute(true);

		final Configuration config = ConfigurationProvider.getConfiguration();
		final BlobStore blobstore = BlobStoreClient.get()
				.getBlobStorePort(config.get("ws.blobstore"));
		final String blobStoreAddress = config.get("rest.blobstore");

		final BlobDescription blob = new BlobDescription()
				.setDescription("SikuliX Debug Screenshots")
				.setNamespace("SikuliX")
				.setDataFromFile(automationBasePath.resolve("sikuli").resolve("sikuliDebug" + id + ".tgz"))
				.setType(".tgz")
				.setName("sikuliDebug" + id);

		BlobHandle handle = blobstore.put(blob);

		//TODO remove tar gz?
		try {
			Files.delete(automationBasePath.resolve("sikuli").resolve("sikuliDebug" + id + ".tgz"));
		}
		catch (IOException e) {
			System.out.println("Could not delete file!");
		}

		ProcessResultUrl returnResult = new ProcessResultUrl();
		returnResult.setUrl(handle.toRestUrl(blobStoreAddress));

		System.out.println("Returning: " + returnResult.getUrl());

		return returnResult;
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
		//TODO make path configurable and properly use path everywhere

		try {
			mapper.writeValue(new File(configName), request);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		return Response.ok().build();
	}


	@POST
	@Path("/execute")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postExecute(AutomationBaseRequest request, @Context UriInfo uri)
	{
		return startAutomationTask(request, uri);
	}


	//TODO remove this, as it does the same as /execute?
	@POST
	@Path("/sikuli")
	@Secured(roles = {Role.PUBLIC})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postExecuteInAlreadyRunningComponent(AutomationBaseRequest request, @Context UriInfo uri)
	{
		return startAutomationTask(request, uri);
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

	private Response startAutomationTask(AutomationBaseRequest request, @Context UriInfo uri)
	{
		final String taskID;
		try {
			taskID = taskmgr.submit(new AutomationTask(request, authenticatedUser.getToken()));
			automations.add(taskID);
		}
		catch (Throwable throwable) {
			LOG.log(Level.WARNING, "Starting the Task failed!", throwable);
			return ResponseUtils.createInternalErrorResponse(throwable);
		}

		return createWaitQueue(uri, taskID, "executions");
	}

	private void resetAutomations() throws IOException, BWFLAException
	{
		automations = new ArrayList<>();

		if (Files.notExists(automationBasePath)) {
			Files.createDirectory(automationBasePath);
			Files.createDirectory(automationBasePath.resolve("sikuli"));
		}

		else {
			DeprecatedProcessRunner pr = new DeprecatedProcessRunner("sudo");
			pr.addArguments("rm", "-rf", "/tmp-storage/automation");
			if (!pr.execute(true))
				throw new BWFLAException("Could not delete automation dir contents!");
			var p = new File("/tmp-storage/automation");

			Files.createDirectory(p.toPath());
			Files.createDirectory(automationBasePath.resolve("sikuli"));
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

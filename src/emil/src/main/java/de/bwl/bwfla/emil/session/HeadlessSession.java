package de.bwl.bwfla.emil.session;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.security.UserContext;
import de.bwl.bwfla.emil.Components;
import de.bwl.bwfla.emil.datatypes.ComputeRequest;
import de.bwl.bwfla.emil.datatypes.ComputeResponse;
import de.bwl.bwfla.emil.datatypes.rest.ComponentResponse;
import de.bwl.bwfla.emil.datatypes.rest.ComponentStateResponse;
import de.bwl.bwfla.emil.datatypes.rest.ProcessResultUrl;
import de.bwl.bwfla.emil.datatypes.rest.SnapshotResponse;
import de.bwl.bwfla.emil.datatypes.snapshot.SaveNewEnvironmentRequest;
import de.bwl.bwfla.emucomp.api.ComponentState;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class HeadlessSession extends Session
{

	private boolean isFinished = false;
	private final UserContext userContext;

	public boolean finished()
	{
		return isFinished;
	}

	private final List<ComputeRequest.ComponentSpec> headlessComponents;
	private final Set<String> componentsToComplete;
	private final Set<ComputeRequest.ComponentSpec> componentsToSave;
	private final HashMap<String, SnapshotResponse> saveEnvironmentTasks = new HashMap<>();
	private final HashMap<String, String> outputTasks = new HashMap<>();

	public HeadlessSession(List<ComputeRequest.ComponentSpec> componentSpecList, UserContext userContext)
	{
		this.userContext = userContext;
		this.componentsToComplete = Collections.synchronizedSet(
				componentSpecList.stream()
						.filter(c -> !c.shouldSaveEnvironment())
						.map(ComputeRequest.ComponentSpec::getComponentId)
						.collect(Collectors.toCollection(HashSet::new))
		);

		this.componentsToSave = Collections.synchronizedSet(
				componentSpecList.stream()
						.filter(c -> c.getSaveEnvironmentLabel() != null)
						.filter(c -> c.getEnvironmentId() != null)
						.collect(Collectors.toCollection(HashSet::new))
		);

		headlessComponents = componentSpecList;
		headlessComponents.forEach(c -> components().add(new SessionComponent(c.getComponentId())));
	}

	public List<ComputeResponse.ComputeResult> getResult(Components endpoint, Logger log)
	{
		if (!isFinished) {
			return null;
		}

		final List<ComputeResponse.ComputeResult> result = new ArrayList<>();
		headlessComponents
				.forEach(
						c -> {
							ComputeResponse.ComputeResult cr = new ComputeResponse.ComputeResult();
							cr.setComponentId(c.getComponentId());
							try {
								final var response = endpoint.getState(cr.getComponentId());
								cr.setState(((ComponentStateResponse) response).getState());
							}
							catch (Exception error) {
								log.log(Level.WARNING, "Fetching component's state failed!", error);
								cr.setState(ComponentState.FAILED.toString());
							}

							if (c.shouldSaveEnvironment()) {
								SnapshotResponse taskResponse;
								if ((taskResponse = saveEnvironmentTasks.get(c.getComponentId())) != null) {
									cr.setEnvironmentId(taskResponse.getEnvId());
								}
							}
							else {
								cr.setResultBlob(outputTasks.get(c.getComponentId()));
							}

							result.add(cr);
						}
				);

		return result;
	}

	@Override
	public void onTimeout(Components endpoint, Logger log)
	{

		log.info("In on Timeout for session: " + id() + " (this should only be called once per session)");

		// TODO only call when some flag is set? (e.g. shouldOperateOnTimeout)
		// this stuff is necessary e.g. for Windows 98 environments, where the backend only registers the shutdown
		// 20% of the time, we can use the timeout to operate
		// however, after reaching this timeout, the session is no longer available, meaning output can't be retrieved
		// via API anymore

		if (componentsToComplete.isEmpty() && componentsToSave.isEmpty()) {
			log.info("Headless Sessions seems to be done already!");

		}
		else {
			log.warning("Found ongoing components, will force shutdown and try to save/export Output");

			componentsToComplete.forEach(component -> {
				try {
					stopEnvironment(endpoint, log, component);
				}
				catch (BWFLAException e) {
					throw new RuntimeException(e);
				}
			});
			componentsToSave.forEach(c -> saveEnvironment(endpoint, c));

		}
		isFinished = true;
	}

	@Override
	public void keepalive(Components endpoint, Logger log)
	{
		super.keepalive(endpoint, log);

		log.info("Headless Keepalive, id " + id() + " ComponentsToComplete: " + componentsToComplete.size() + " ComponentsToSave: " + componentsToSave.size());

		this.componentsToComplete.removeIf(
				component -> {
					try {
						ComponentResponse response = endpoint.getState(component);
						String componentState = ((ComponentStateResponse) response).getState();
						if (componentState.equals(ComponentState.STOPPED.name())) {
							stopEnvironment(endpoint, log, component);
							return true;
						}
						else if (componentState.equals(ComponentState.FAILED.name())) {
							outputTasks.put(component, "Execution of " + component + " failed: Component Status is FAILED.");
							return true;
						}
						return false;
					}
					catch (Exception e) {
						e.printStackTrace();
						outputTasks.put(component, "Execution of " + component + " failed: " + e);
						return true;
					}
				}
		);

		this.componentsToSave.forEach(c -> {
			SnapshotResponse t = saveEnvironmentTasks.get(c.getComponentId());
			if (t == null) {
				try {
					ComponentResponse response = endpoint.getState(c.getComponentId());
					String componentState = ((ComponentStateResponse) response).getState();
					if (componentState.equals(ComponentState.STOPPED.name())) {
						saveEnvironment(endpoint, c);
					}
					else if (componentState.equals(ComponentState.FAILED.name())) {
						saveEnvironmentTasks.put(c.getComponentId(), new SnapshotResponse(
								new BWFLAException("Execution of " + c.getComponentId() + " failed: Component Status is FAILED.")));
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					saveEnvironmentTasks.put(c.getComponentId(), new SnapshotResponse(
							new BWFLAException("Execution of " + c.getComponentId() + " failed: Component Status is FAILED.")));
				}
			}
		});

		this.componentsToSave.removeIf(
				component -> {
					SnapshotResponse t = saveEnvironmentTasks.get(component.getComponentId());
					return t != null;
				}
		);

		if (componentsToComplete.isEmpty() && componentsToSave.isEmpty()) {
			isFinished = true;
		}
	}

	private void stopEnvironment(Components endpoint, Logger log, String component) throws BWFLAException
	{
		var stopResponse = sendEnvironmentStopRequest(component);
		this.outputTasks.put(component, stopResponse.getUrl());
		log.info(" Got response from stopping component (to complete): " + stopResponse.getUrl());
	}

	private void saveEnvironment(Components endpoint, ComputeRequest.ComponentSpec c)
	{
		System.out.println("In headless Session, saveEnvironment");
		SaveNewEnvironmentRequest request = new SaveNewEnvironmentRequest();
		request.setEnvId(c.getEnvironmentId());
		request.setTitle(c.getSaveEnvironmentLabel());
		request.setCleanRemovableDrives(true);
		try {

			if (saveEnvironmentTasks.get(c.getComponentId()) == null) {
				saveEnvironmentTasks.put(c.getComponentId(), new SnapshotResponse("PLACEHOLDER!"));
				saveEnvironmentTasks.put(c.getComponentId(), sendSaveEnvironmentRequest(c.getComponentId(), request, userContext));
			}
			else {
				System.out.println("--------------- Trying to put something in saveEnvTasks for " + c.getComponentId() + "but something already exists!");
			}

			//saveEnvironmentTasks.put(c.getComponentId(), endpoint.snapshot2(c.getComponentId(), request, userContext));

		}
		catch (Exception e) {
			e.printStackTrace();
			BWFLAException error = e instanceof BWFLAException ? (BWFLAException) e : new BWFLAException(e);
			saveEnvironmentTasks.put(c.getEnvironmentId(), new SnapshotResponse(error));
		}
	}

	private SnapshotResponse sendSaveEnvironmentRequest(String componentId, SaveNewEnvironmentRequest body, UserContext userContext) throws BWFLAException
	{
		var client = ClientBuilder.newClient();
		var baseUrl = "http://eaas:8080/emil";
		var target = client.target(baseUrl);

		Invocation.Builder request = target
				.path("components/" + componentId + "/snapshot")
				.queryParam("access_token", userContext.getToken())
				.request(MediaType.APPLICATION_JSON_TYPE);
		Response response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));

		if (Response.Status.fromStatusCode(response.getStatus()) == Response.Status.OK) {
			return response.readEntity(SnapshotResponse.class);
		}
		else {
			throw new BWFLAException("Snapshot did no return 200 but " + response.getStatus());
		}
	}

	private ProcessResultUrl sendEnvironmentStopRequest(String componentId) throws BWFLAException
	{
		var client = ClientBuilder.newClient();
		var baseUrl = "http://eaas:8080/emil";
		var target = client.target(baseUrl);

		Invocation.Builder request = target
				.path("components/" + componentId + "/stop")
				.queryParam("access_token", userContext.getToken()).request();
		Response response = request.get();

		if (Response.Status.fromStatusCode(response.getStatus()) == Response.Status.OK) {
			return response.readEntity(ProcessResultUrl.class);
		}
		else {
			throw new BWFLAException("Snapshot did no return 200 but " + response.getStatus());
		}
	}

}

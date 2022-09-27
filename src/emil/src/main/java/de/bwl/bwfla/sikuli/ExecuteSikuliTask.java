package de.bwl.bwfla.sikuli;

import de.bwl.bwfla.automation.api.sikuli.SikuliExecutionRequest;
import de.bwl.bwfla.automation.client.sikuli.SikuliClient;
import de.bwl.bwfla.common.taskmanager.BlockingTask;


public class ExecuteSikuliTask extends BlockingTask<Object>
{

	private final SikuliExecutionRequest request;
	private final SikuliClient client;

	public ExecuteSikuliTask(SikuliExecutionRequest request, SikuliClient client)
	{
		this.request = request;
		this.client = client;
	}

	@Override
	protected Object execute() throws Exception
	{
		request.setTaskId(getTaskId());
		client.executeSikuliScript(request);
		//TODO pass any result here?
		return null;
	}


}

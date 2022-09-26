package de.bwl.bwfla.sikuli;

import de.bwl.bwfla.automation.api.sikuli.SikuliDownloadRequest;
import de.bwl.bwfla.automation.client.sikuli.SikuliClient;
import de.bwl.bwfla.common.taskmanager.BlockingTask;


public class DownloadSikuliScriptTask extends BlockingTask<Object>
{

	private final SikuliDownloadRequest request;
	private final SikuliClient client;


	public DownloadSikuliScriptTask(SikuliDownloadRequest request, SikuliClient client)
	{

		this.request = request;
		this.client = client;
	}

	@Override
	protected Object execute() throws Exception
	{
		return client.downloadSikuliScript(request);
	}
}

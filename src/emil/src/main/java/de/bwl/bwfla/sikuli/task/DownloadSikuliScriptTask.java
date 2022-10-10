package de.bwl.bwfla.sikuli.task;

import com.openslx.automation.api.sikuli.SikuliDownloadRequest;
import com.openslx.automation.client.sikuli.SikuliClient;
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

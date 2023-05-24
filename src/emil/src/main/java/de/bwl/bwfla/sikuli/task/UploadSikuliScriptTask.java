package de.bwl.bwfla.sikuli.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openslx.eaas.automation.api.sikuli.SikuliUploadRequest;
import com.openslx.eaas.automation.api.sikuli.SikuliUploadResponse;
import com.openslx.eaas.automation.client.sikuli.SikuliClient;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.sikuli.SikuliUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;


public class UploadSikuliScriptTask extends BlockingTask<Object>
{

	private final SikuliUploadRequest request;
	private final SikuliClient client;

	public UploadSikuliScriptTask(SikuliUploadRequest request, SikuliClient client)
	{
		this.request = request;
		this.client = client;
	}


	@Override
	protected Object execute() throws Exception
	{

		log.info("Starting Sikuli Upload Task on Gateway");

		client.uploadSikuliScript(request);
		return null;
	}


}

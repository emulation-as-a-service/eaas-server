package de.bwl.bwfla.sikuli.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bwl.bwfla.automation.api.sikuli.SikuliUploadRequest;
import de.bwl.bwfla.automation.api.sikuli.SikuliUploadResponse;
import de.bwl.bwfla.automation.client.sikuli.SikuliClient;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.sikuli.SikuliUtils;

import java.nio.file.Files;
import java.nio.file.Path;


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

		if (request.isComputeUiFields()) {
			Path tmpPath = Files.createTempDirectory("sikuli");

			String blobStoreUrl = request.getBlobStoreUrl();
			SikuliUtils.extractTarFromBlobstore(tmpPath, blobStoreUrl);

			DeprecatedProcessRunner reverseScriptRunner = new DeprecatedProcessRunner("sudo");
			reverseScriptRunner.setWorkingDirectory(tmpPath);
			reverseScriptRunner.addArguments("python3", "/libexec/sikuli-script-creator/reverse.py", tmpPath.toString());
			reverseScriptRunner.execute(true);

			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.readValue(tmpPath.resolve("reverse_script.json").toFile(), SikuliUploadResponse.class);
		}
		else
			return null;

	}


}

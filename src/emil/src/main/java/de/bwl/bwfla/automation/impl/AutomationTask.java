package de.bwl.bwfla.automation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bwl.bwfla.automation.api.AutomationBaseRequest;
import de.bwl.bwfla.automation.api.AutomationSikuliRequest;
import de.bwl.bwfla.automation.api.AutomationTemplateRequest;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.imagebuilder.api.ImageContentDescription;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;


public class AutomationTask extends BlockingTask<Object>
{
	private final String token;
	private AutomationBaseRequest request;

	public AutomationTask(AutomationBaseRequest request, String token)
	{
		this.request = request;
		this.token = token;
	}

	@Override
	protected Object execute() throws Exception
	{

		String taskPath = "/tmp-storage/automation/" + getTaskId();
		Path pathOf = Path.of(taskPath);
		Files.createDirectory(pathOf);
		Files.createDirectory(pathOf.resolve("files"));


		boolean isSikuliTask = false;

		if (request.getClass().equals(AutomationSikuliRequest.class)) {
			log.info("Got AutomationSikuliRequest...");
			isSikuliTask = true;
		}

		else if (request.getClass().equals(AutomationTemplateRequest.class)) {
			log.info("Got AutomationTemplateRequest...");

			if (((AutomationTemplateRequest) request).getAutomationType().equals("migration")) {
				var allFiles = ((AutomationTemplateRequest) request).getAllFiles();
				for (var file : allFiles) {
					String filePath = taskPath + "/files/" + file.getName();
					curlToFileFromBlobstore(filePath, file.getUrl());

					if (file.getAction().equals(ImageContentDescription.Action.EXTRACT)) {
						extractTar(Path.of(taskPath + "/files"), filePath);
						Files.delete(Path.of(filePath));
					}
				}
			}

			else {

				String filePath = taskPath + "/files/" + ((AutomationTemplateRequest) request).getFileName();
				curlToFileFromBlobstore(filePath, ((AutomationTemplateRequest) request).getFilePath());

				((AutomationTemplateRequest) request).setFilePath(filePath);
			}
		}

		else {
			throw new BWFLAException("Could not read request, was neither AutomationTemplateRequest nor AutomationSikuliRequest!");
		}

		ObjectMapper mapper = new ObjectMapper();
		String configName = taskPath + "/config.json";

		log.info("Writing to file: " + mapper.writeValueAsString(request));
		mapper.writeValue(new File(configName), request);

		DeprecatedProcessRunner automationScriptRunner = new DeprecatedProcessRunner("sudo");
		automationScriptRunner.setWorkingDirectory(Path.of("/libexec/migration-templating"));
		automationScriptRunner.addArguments("python3", "/libexec/migration-templating/main.py", configName, "-t", getTaskId());

		if (Files.exists(Path.of("/tmp-storage/automation/automation_config.json"))) {
			automationScriptRunner.addArguments("-c", "/tmp-storage/automation/automation_config.json");
		}

		if (isSikuliTask) {
			automationScriptRunner.addArgument("--sikuli");
		}

		if (token != null) {
			log.info("Adding token to python script!");
			automationScriptRunner.addArguments("-a", token);
		}

		if (automationScriptRunner.execute(true)) {
			return "Successfully executed automation task";
		}
		else {
			throw new RuntimeException("Error while executing automation task " + getTaskId());
		}

	}

	private void curlToFileFromBlobstore(String filePath, String url) throws BWFLAException
	{
		DeprecatedProcessRunner pr = new DeprecatedProcessRunner("curl");
		pr.setLogger(log);
		pr.addArguments("-L", "-o", filePath);
		pr.addArgument(url);
		if (!pr.execute(true)) throw new BWFLAException("failed to download input file!");
	}

	private void extractTar(Path targetDir, String filePath) throws BWFLAException
	{
		DeprecatedProcessRunner pr = new DeprecatedProcessRunner("sudo");
		pr.setLogger(log);
		pr.setWorkingDirectory(targetDir);
		pr.addArguments("tar", "xvf", filePath);
		if (!pr.execute(true))
			throw new BWFLAException("failed to extract tar");
	}

}

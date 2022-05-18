package de.bwl.bwfla.automation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bwl.bwfla.automation.api.AutomationRequest;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.imagebuilder.api.ImageContentDescription;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;


public class AutomationTask extends BlockingTask<Object>
{
	private AutomationRequest request;

	public AutomationTask(AutomationRequest request)
	{
		this.request = request;
	}

	@Override
	protected Object execute() throws Exception
	{

		String taskPath = "/tmp-storage/automation/" + getTaskId();
		Path pathOf = Path.of(taskPath);
		Files.createDirectory(pathOf);
		Files.createDirectory(pathOf.resolve("files"));


		if (request.isSikuliOnly()) {
			log.info("Sikuli Automation only!");
		}

		else if (request.getAutomationType().equals("migration")) {
			var x = request.getAllFiles();
			for (var file : x) {
				String filePath = taskPath + "/files/" + file.getName();
				curlToFileFromBlobstore(filePath, file.getUrl());

				if (file.getAction().equals(ImageContentDescription.Action.EXTRACT)) {
					extractTar(Path.of(taskPath + "/files"), filePath);
					Files.delete(Path.of(filePath));
				}
			}
		}

		else {

//			String filePath = taskPath + "/files/input." + request.getFileType();
			String filePath = taskPath + "/files/" + request.getFileName();
			curlToFileFromBlobstore(filePath, request.getFilePath());

			request.setFilePath(filePath);
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

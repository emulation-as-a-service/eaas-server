package de.bwl.bwfla.automation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bwl.bwfla.api.blobstore.BlobStore;
import de.bwl.bwfla.automation.api.AllAutomationsResponse;
import de.bwl.bwfla.automation.api.AutomationBaseRequest;
import de.bwl.bwfla.automation.api.AutomationSikuliRequest;
import de.bwl.bwfla.automation.api.AutomationTemplateRequest;
import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.imagebuilder.api.ImageContentDescription;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

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
	protected String execute() throws Exception
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

			// TODO introduce a separate class for the config file for the python script?
			// TODO right now, the request gets modified (if necessary) and is then passed as the config file
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
		String configPath = taskPath + "/config.json";

		log.info("Writing to config file (" + configPath + "):" + mapper.writeValueAsString(request));
		mapper.writeValue(new File(configPath), request);

		DeprecatedProcessRunner automationScriptRunner = new DeprecatedProcessRunner("sudo");
		automationScriptRunner.setWorkingDirectory(Path.of("/libexec/migration-templating"));
		automationScriptRunner.addArguments("python3", "/libexec/migration-templating/main.py", configPath, "-t", getTaskId());

		//TODO make hardcoded path configurable
		if (Files.exists(Path.of("/tmp-storage/automation/automation_config.json"))) {
			automationScriptRunner.addArguments("-c", "/tmp-storage/automation/automation_config.json");
		}

		if (isSikuliTask) {
			automationScriptRunner.addArgument("--sikuli");
		}

		if (token != null) {
			log.info("Adding token to python script arguments!");
			automationScriptRunner.addArguments("-a", token);
		}

		if (automationScriptRunner.execute(true)) {
			log.info("Successfully executed automation task!");

			File resultFile = new File("/tmp-storage/automation/" + getTaskId() + "/status.json");
			var pyResult =
					new ObjectMapper().readValue(resultFile, AllAutomationsResponse.PythonResultFile.class);

			//if result is a new environment, simply return environment id
			if (pyResult.getAutomationType().equals("environment")) {
				return pyResult.getResult();
			}
			//otherwise upload the result files to the blobstore
			//this is necessary, as the result that is uploaded by the python script can't be given a type and name
			else {
				Path outputPath = Path.of("/tmp-storage/automation").resolve(getTaskId()).resolve("result.tgz");
				if (Files.exists(outputPath)) {

					log.info("Got output folder: " + outputPath);

					final Configuration config = ConfigurationProvider.getConfiguration();
					final BlobStore blobstore = BlobStoreClient.get()
							.getBlobStorePort(config.get("ws.blobstore"));
					final String blobStoreAddress = config.get("rest.blobstore");

					final BlobDescription blob = new BlobDescription()
							.setDescription("Result of Automation Task " + getTaskId())
							.setNamespace("automation")
							.setDataFromFile(outputPath)
							.setType(".tgz")
							.setName("output");

					BlobHandle handle = blobstore.put(blob);

					log.info("Blobstore URL: " + handle.toRestUrl(blobStoreAddress));

					return handle.toRestUrl(blobStoreAddress);
				}
				else {
					log.warning("Result is set to 'files', however no output was found at " + outputPath);
					throw new BWFLAException("Error while executing automation task " + getTaskId());
				}

			}
		}
		else {
			log.warning("Python automation script did not return code 0!");
			throw new BWFLAException("Error while executing automation task " + getTaskId());
		}
	}


	//TODO move all these together with SikuliUtils into own class?
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

	private void tarOutputFolder(String dirToTar, String resultPath, Path workingDir) throws BWFLAException
	{
		DeprecatedProcessRunner pr = new DeprecatedProcessRunner("sudo");
		pr.setLogger(log);
		pr.setWorkingDirectory(workingDir);
		pr.addArguments("tar", "czvf", resultPath, dirToTar);
		if (!pr.execute(true))
			throw new BWFLAException("failed to extract tar");
	}
}
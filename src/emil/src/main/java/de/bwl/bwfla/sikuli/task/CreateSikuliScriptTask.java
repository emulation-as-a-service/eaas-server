package de.bwl.bwfla.sikuli.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.openslx.eaas.common.util.RuncStateInformation;
import de.bwl.bwfla.api.blobstore.BlobStore;
import de.bwl.bwfla.automation.api.sikuli.SikuliCreateScriptRequest;
import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.sikuli.SikuliUtils;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class CreateSikuliScriptTask extends BlockingTask<Object>
{

	private final SikuliCreateScriptRequest request;

	public CreateSikuliScriptTask(SikuliCreateScriptRequest request)
	{
		this.request = request;
	}

	@Override
	protected ProcessResultUrl execute() throws Exception
	{

		log.info("Creating Sikuli Script...");

		ObjectWriter objectWriter = new ObjectMapper().writer().with(SerializationFeature.INDENT_OUTPUT).withDefaultPrettyPrinter();
		String json;
		try {
			json = objectWriter.writeValueAsString(request.getEntries());
		}
		catch (JsonProcessingException e) {
			throw new BWFLAException("Could not unmarshall entries to JSON Object", e);
		}

		Path workingDir = Files.createTempDirectory("sikuli");

		try {
			Files.writeString(workingDir.resolve("sikuli.json"), json);
		}
		catch (IOException e) {
			throw new BWFLAException("Could not write sikuli entries to JSON file.", e);
		}

		DeprecatedProcessRunner createScriptRunner = new DeprecatedProcessRunner("sudo");
		createScriptRunner.setLogger(log);
		createScriptRunner.setWorkingDirectory(workingDir);
		createScriptRunner.addArgument("python3");
		createScriptRunner.addArgument("/libexec/sikuli-script-creator/main.py");
		createScriptRunner.addArgument(workingDir.resolve("sikuli.json").toString());
		createScriptRunner.addArguments(workingDir.toString());
		if (createScriptRunner.execute(true)) {

			String blobstoreUrl = SikuliUtils.tarSikuliDirectoryAndUploadToBlobstore(workingDir);
			ProcessResultUrl returnResult = new ProcessResultUrl();
			returnResult.setUrl(blobstoreUrl);
			return returnResult;
		}
		else {
			throw new BWFLAException("Sikuli Script creation failed! Python script did not exit with status code 0.");
		}
	}
}

package de.bwl.bwfla.sikuli.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.openslx.eaas.automation.api.sikuli.SikuliCreateScriptRequest;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.sikuli.SikuliUtils;

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
			log.info("Reading entries from request...");
			json = objectWriter.writeValueAsString(request.getEntries());
			log.info("Successfully read entries.");
		}
		catch (JsonProcessingException e) {
			throw new BWFLAException("Could not unmarshall entries to JSON Object", e);
		}

		Path workingDir = Files.createTempDirectory("sikuli");


		try {
			Files.createFile(workingDir.resolve("sikuli.json"));
			Files.writeString(workingDir.resolve("sikuli.json"), json);
			if(Files.exists(workingDir.resolve("sikuli.json"))){
				log.info("Successfully wrote to " + workingDir.resolve("sikuli.json"));
			}
			else{
				log.warning("File does not exist!");
			}
		}
		catch (IOException e) {
			throw new BWFLAException("Could not write sikuli entries to JSON file.", e);
		}

		DeprecatedProcessRunner createScriptRunner = new DeprecatedProcessRunner("sudo");
		createScriptRunner.setLogger(log);
		createScriptRunner.addArgument("python3");
		createScriptRunner.addArgument("/libexec/sikuli-script-creator/main.py");
		createScriptRunner.addArgument(workingDir.resolve("sikuli.json").toString());
		createScriptRunner.addArguments(workingDir.toString());
		if (createScriptRunner.execute(true)) {

			String blobstoreUrl = SikuliUtils.tarSikuliDirectoryAndUploadToBlobstore(workingDir.resolve("output"));
			ProcessResultUrl returnResult = new ProcessResultUrl();
			returnResult.setUrl(blobstoreUrl);
			return returnResult;
		}
		else {
			throw new BWFLAException("Sikuli Script creation failed! Python script did not exit with status code 0.");
		}
	}
}

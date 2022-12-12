package de.bwl.bwfla.sikuli.task;

import com.openslx.eaas.automation.api.sikuli.SikuliExecutionRequest;
import com.openslx.eaas.automation.client.sikuli.SikuliClient;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.sikuli.SikuliUtils;

import javax.ws.rs.InternalServerErrorException;
import java.nio.file.Files;
import java.nio.file.Path;


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
		log.info("Starting Sikuli Execution Task");
		Path taskPath = Path.of("/tmp-storage/automation/sikuli/").resolve(getTaskId());
		Files.createDirectories(taskPath);
		Path componentTxtPath = taskPath.resolve("component.txt");

		Files.createFile(componentTxtPath);
		Files.writeString(componentTxtPath, request.getComponentId());
		var executionResponse = client.executeSikuliScript(request);
		var debugURL = client.getDebugURL();

		log.info("Got response from emucomp backend, execution is stopped!");
		var debugInfoBlobstoreUrl = debugURL.getUrl();

		Path urlTxtPath = taskPath.resolve("url.txt");
		Files.createFile(urlTxtPath);
		Files.writeString(urlTxtPath, debugInfoBlobstoreUrl);

		//TODO only download logs once? Screenshots aren't needed on the gateway
		log.info("Downloading Debug info from " + debugInfoBlobstoreUrl + " to: " + taskPath);
		//this is necessary to access logs instantly from the UI even after execution
		//full debug information is passed via url written in url.txt always
		SikuliUtils.extractTarFromBlobstore(taskPath, debugInfoBlobstoreUrl);

		if(executionResponse.getStatus()==200){
			return debugURL.getUrl();
		}
		else {
			throw new InternalServerErrorException("Sikuli script did not complete with status code 200, but " + executionResponse.getStatus());
		}

	}



}

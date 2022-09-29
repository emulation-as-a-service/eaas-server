package de.bwl.bwfla.sikuli.task;

import de.bwl.bwfla.automation.api.sikuli.SikuliExecutionRequest;
import de.bwl.bwfla.automation.client.sikuli.SikuliClient;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;

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
		var response = client.executeSikuliScript(request);

		if (response.getStatus() == 200) {
			var result = response.readEntity(ProcessResultUrl.class);

			log.info("Got response from emucomp backend, execution is done!");
			var debugInfoBlobstoreUrl = result.getUrl();

			Path urlTxtPath = taskPath.resolve("url.txt");
			Files.createFile(urlTxtPath);
			Files.writeString(urlTxtPath, debugInfoBlobstoreUrl);

			log.info("Downloading Debug info from " + debugInfoBlobstoreUrl + " to: " + taskPath);
			downloadAndExtractTar(taskPath, debugInfoBlobstoreUrl);

			return response;
		}
		else {
			throw new InternalServerErrorException("Sikuli script did not complete with status code 200, but " + response.getStatus());
		}

	}

	//TODO a generalized version of this should probably be in commons
	// see SikuliUtils in automation module
	public static void downloadAndExtractTar(Path workDir, String blobStoreUrl) throws Exception
	{

		if (Files.notExists(workDir)) {
			Files.createDirectories(workDir);
		}

		DeprecatedProcessRunner pr = new DeprecatedProcessRunner("curl");
		pr.addArguments("-L", "-o", workDir.toString() + "/out.tgz");
		pr.addArgument(blobStoreUrl);
		if (!pr.execute(true))
			throw new BWFLAException("failed to download " + blobStoreUrl);

		pr = new DeprecatedProcessRunner("sudo");
		pr.setWorkingDirectory(workDir);
		pr.addArguments("tar", "xvf", workDir.toString() + "/out.tgz");
		if (!pr.execute(true))
			throw new BWFLAException("failed to extract tar");
	}

}

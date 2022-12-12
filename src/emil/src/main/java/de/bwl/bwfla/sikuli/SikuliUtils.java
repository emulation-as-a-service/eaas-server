package de.bwl.bwfla.sikuli;

import de.bwl.bwfla.api.blobstore.BlobStore;
import com.openslx.eaas.automation.api.sikuli.SikuliLogResponse;
import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;


public class SikuliUtils
{


	public static String tarSikuliDirectoryAndUploadToBlobstore(Path sikuliDirectory) throws Exception
	{

		Path tempDir = Files.createTempDirectory("sikuli");
		DeprecatedProcessRunner tarRunner = new DeprecatedProcessRunner("tar");
		tarRunner.setWorkingDirectory(sikuliDirectory);
		tarRunner.addArguments("-zcvf", tempDir.resolve("sikulix.tar.gz").toString(), sikuliDirectory.toString());
		tarRunner.execute(true);

		final Configuration config = ConfigurationProvider.getConfiguration();
		final BlobStore blobstore = BlobStoreClient.get()
				.getBlobStorePort(config.get("ws.blobstore"));

		final String blobStoreAddress = config.get("rest.blobstore");

		final BlobDescription blob = new BlobDescription()
				.setDescription("SikuliX Script")
				.setNamespace("sikuli")
				.setDataFromFile(tempDir.resolve("sikulix.tar.gz"))
				.setType(".tgz")
				.setName("sikuliScript");

		BlobHandle handle = blobstore.put(blob);
		return handle.toRestUrl(blobStoreAddress);
	}


	public static void extractTarFromBlobstore(Path workDir, String blobStoreUrl) throws BWFLAException
	{

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

	public static SikuliLogResponse getLogsForTaskId(String taskId)
	{
		var basePath = Path.of("/tmp-storage/automation/sikuli").resolve(taskId);
		if (Files.notExists(basePath)) {
			throw new NotFoundException("Could not find directory for taskId " + taskId);
		}
		var response = new SikuliLogResponse();
		if (Files.notExists(basePath.resolve("logs.txt"))) {
			//task is created but still running, as logs.txt is only written upon task completion
			//this means we need to grab the logs from the component directly

			return response;
		}
		else {
			try {
				response.setLines((ArrayList<String>) Files.readAllLines(basePath.resolve("logs.txt")));
				return response;
			}
			catch (IOException e) {
				throw new InternalServerErrorException("Could not read file, although log file is present");
			}

		}
	}

	public static ProcessResultUrl getDebugInfoForTaskId(String taskId)
	{
		var basePath = Path.of("/tmp-storage/automation/sikuli").resolve(taskId);
		if (Files.notExists(basePath)) {
			throw new NotFoundException("Could not find directory for taskId " + taskId);
		}
		var response = new ProcessResultUrl();
		if (Files.notExists(basePath.resolve("url.txt"))) {
			throw new NotFoundException("Could not find url.txt for taskId " + taskId);
		}
		else {
			try {
				response.setUrl(Files.readString(basePath.resolve("url.txt")));
				return response;
			}
			catch (IOException e) {
				throw new InternalServerErrorException("Could not read file, although url file is present");
			}

		}
	}
}

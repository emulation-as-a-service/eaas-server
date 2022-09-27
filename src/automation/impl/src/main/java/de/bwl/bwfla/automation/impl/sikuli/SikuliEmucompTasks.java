package de.bwl.bwfla.automation.impl.sikuli;

import com.openslx.eaas.common.util.RuncStateInformation;
import de.bwl.bwfla.api.blobstore.BlobStore;
import de.bwl.bwfla.automation.api.sikuli.SikuliExecutionRequest;
import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import org.apache.commons.io.IOUtils;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;


public class SikuliEmucompTasks
{

	private static final Logger LOG = Logger.getLogger("SIKULI-TASKS");

	public static void executeSikuliScriptInEmulatorContainer(String componentId, SikuliExecutionRequest request, String taskId) throws Exception
	{
		LOG.info("Executing Sikuli Script...");

		int x, y;
		if (request.getResolution() != null) {
			LOG.info("Got custom resolution...");
			x = request.getResolution().getX();
			y = request.getResolution().getY();
		}
		else {
			LOG.info("Resolution not specified, defaulting to 1280x1024");
			x = 1280;
			y = 1024;
		}

		LOG.info("Setting resolution to x: " + x + ", y: " + y);
		DeprecatedProcessRunner resolutionXpraRunner = new DeprecatedProcessRunner("sudo");
		resolutionXpraRunner.setLogger(LOG);
		resolutionXpraRunner.addArguments(
				"runc", "exec", "-e", "DISPLAY=:7000", "-e", "LD_PRELOAD=", request.getComponentId(),
				"sh", "-c", "x=\"$0\"; y=\"$1\"; export DISPLAY=:7000; xrandr --newmode \"${x}x${y}\" 0 \"$x\" 0 0 0 \"$y\" 0 0 0; xrandr --addmode screen \"${x}x${y}\"; xrandr -s \"${x}x${y}\"",
				Integer.toString(x), Integer.toString(y));
		resolutionXpraRunner.execute(true);

		if (request.isHeadless()) {
			Thread.sleep(1000);
			LOG.info("Component running in headless mode, exiting xpra...");
			DeprecatedProcessRunner exitXpraRunner = new DeprecatedProcessRunner("sudo");
			exitXpraRunner.setLogger(LOG);
			exitXpraRunner.addArguments(
					"runc", "exec", request.getComponentId(), "xpra", "exit", "socket://emucon/data/sockets/xpra-iosocket");
			exitXpraRunner.execute(true);
		}

		Thread.sleep(1000);

		RuncStateInformation info = RuncStateInformation.getRuncStateInformationForComponent(request.getComponentId());

		Path tmpPath = Path.of(info.getBundle());
		Path scriptPathAppserver = SikuliUtils.getSikuliFilenameForDirectory(tmpPath.resolve("data/uploads"));

		Path parentDir = scriptPathAppserver.getParent();
		Path parentName = parentDir.getName(parentDir.getNameCount() - 1);
		Path fileName = scriptPathAppserver.getFileName();

		Path scriptPathContainer = Path.of("/emucon/data/uploads/").resolve(parentName).resolve(fileName);
		LOG.info("Executing Sikuli Script at: " + scriptPathContainer);

		DeprecatedProcessRunner sikuliRunner = new DeprecatedProcessRunner("sudo");
		sikuliRunner.setLogger(LOG);
		sikuliRunner.addArguments(
				"runc", "exec", "-e", "DISPLAY=:7000", "-e", "LD_PRELOAD=", request.getComponentId(),
				"java", "-jar", "/sikulix.jar", "-c", "-r", scriptPathContainer.toString());

		boolean DEBUG = true; //TODO move to request eventually

		if (DEBUG) {
			sikuliRunner.addArguments("--", "DEBUG");

			//TODO separate from DEBUG eventually
			if (request.getParameters() != null && !request.getParameters().isEmpty()) {
				sikuliRunner.addArguments(request.getParameters());
			}
		}

		if (!sikuliRunner.start()) {
			throw new BWFLAException("Starting sikuli subprocess failed!");
		}
		int success;

		//TODO make configurable
		Path logDir = Path.of("/tmp-storage/automation/sikuli/" + taskId);
		if (!Files.exists(logDir)) {
			Files.createDirectories(logDir);
		}
		Path logPath = Files.createFile(logDir.resolve("logs.txt"));
		File sikuliLogFile = new File(logPath.toUri());

		try (InputStreamReader reader = (InputStreamReader) sikuliRunner.getStdOutReader();
			 OutputStream outputStream = new FileOutputStream(sikuliLogFile)) {

			while (!sikuliRunner.isProcessFinished()) {
				IOUtils.copy(reader, outputStream);
			}
		}
		catch (IOException error) {
			throw new BWFLAException("IO Error while writing to log file!", error);
		}

		success = sikuliRunner.waitUntilFinished();
		LOG.info("Sikuli StdOut/StdErr");
		sikuliRunner.printStdOut();
		sikuliRunner.printStdErr();
		sikuliRunner.cleanup();

		DeprecatedProcessRunner screenshotRunner = new DeprecatedProcessRunner("sudo");
		screenshotRunner.addArgument("/libexec/findSikuliScreenshots.sh");
		screenshotRunner.addArgument(info.getPid());
		screenshotRunner.addArgument(parentName.toString());
		screenshotRunner.addArgument(taskId);
		screenshotRunner.execute();


		if (success == 0) {
			LOG.info("--------------- SUCCESSFULLY EXECUTED SIKULI SCRIPT! -----------------");

			if (request.isHeadless()) {

				//stopComponentAfterExecution();
			}
		}
		else {
			LOG.info("Sikuli returned Status code: " + success);
			if (request.isHeadless()) {
				//stopComponentAfterExecution();
			}
			throw new BWFLAException("Sikuli exited with non-zero status code!");
		}

	}

	public static void uploadSikuliScriptToEmulatorContainer(String componentId, String blobstoreURL) throws BWFLAException
	{

		LOG.info("Starting Sikuli Upload Task...");

		Path tmpPath = Path.of(RuncStateInformation.getRuncStateInformationForComponent(componentId).getBundle());

		SikuliUtils.extractTar(tmpPath, blobstoreURL);

		//TODO move everything below to gateway with new API (/reverse e.g.)
//		Path scriptPath = SikuliUtils.getSikuliFilenameForDirectory(tmpPath.resolve("data/uploads"));
//
//		DeprecatedProcessRunner reverseScriptRunner = new DeprecatedProcessRunner("sudo");
//		reverseScriptRunner.setWorkingDirectory(tmpPath);
//		reverseScriptRunner.addArguments("python3", "/libexec/sikuli-script-creator/reverse.py", scriptPath.toString());
//		reverseScriptRunner.execute(true);
//
//		ObjectMapper objectMapper = new ObjectMapper();
//		return objectMapper.readValue(tmpPath.resolve("reverse_script.json").toFile(), SikuliUploadResponse.class);
	}

	public static ProcessResultUrl downloadCurrentSikuliScriptFromEmulatorContainer(String componentId) throws BWFLAException
	{
		LOG.info("Compressing Sikuli Script and uploading to Blobstore...");
		Path tmpPath = Path.of(RuncStateInformation.getRuncStateInformationForComponent(componentId).getBundle());

		DeprecatedProcessRunner tarRunner = new DeprecatedProcessRunner("tar");
		tarRunner.setLogger(LOG);
		tarRunner.setWorkingDirectory(tmpPath.resolve("data/uploads"));
		tarRunner.addArguments("-zcvf", tmpPath.resolve("sikulix.tar.gz").toString(), "output.sikuli");
		tarRunner.execute(true);

		final Configuration config = ConfigurationProvider.getConfiguration();
		final BlobStore blobstore = BlobStoreClient.get()
				.getBlobStorePort(config.get("ws.blobstore"));
		final String blobStoreAddress = config.get("rest.blobstore");

		final BlobDescription blob = new BlobDescription()
				.setDescription("SikuliX Script")
				.setNamespace("sikuli")
				.setDataFromFile(tmpPath.resolve("sikulix.tar.gz"))
				.setType(".tgz")
				.setName("sikuliScript");

		BlobHandle handle = blobstore.put(blob);

		ProcessResultUrl returnResult = new ProcessResultUrl();
		returnResult.setUrl(handle.toRestUrl(blobStoreAddress));

		LOG.info("Blob Store Address for Sikuli Script: " + handle.toRestUrl(blobStoreAddress));

		return returnResult;
	}

}

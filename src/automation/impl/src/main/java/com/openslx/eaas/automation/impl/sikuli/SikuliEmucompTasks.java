package com.openslx.eaas.automation.impl.sikuli;

import com.openslx.eaas.common.util.RuncStateInformation;
import de.bwl.bwfla.api.blobstore.BlobStore;
import com.openslx.eaas.automation.api.sikuli.SikuliExecutionRequest;
import com.openslx.eaas.automation.api.sikuli.SikuliLogResponse;
import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.datatypes.ProcessResultUrl;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.objectarchive.util.ObjectArchiveHelper;
import de.bwl.bwfla.emucomp.api.FileCollection;
import org.apache.commons.io.IOUtils;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class SikuliEmucompTasks
{

	private static final Logger LOG = Logger.getLogger("SIKULI-TASKS");
	private static final Path sikuliBasePath = Path.of("/tmp-storage/emucomp-automation/sikuli"); //TODO config?


	public static void executeSikuliScriptInEmulatorContainer(String componentId, SikuliExecutionRequest request) throws Exception
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

		//TODO change sikuli scripts to work without this
		sikuliRunner.addArguments("--", "DEBUG");

		if (request.getParameters() != null && !request.getParameters().isEmpty()) {
			sikuliRunner.addArguments(request.getParameters());
		}


		if (!sikuliRunner.start()) {
			throw new BWFLAException("Starting sikuli subprocess failed!");
		}
		int success;

		Path logDir = Path.of("/tmp-storage/emucomp-automation/sikuli/" + componentId);
		if (!Files.exists(logDir)) {
			Files.createDirectories(logDir);
		}

		Path logPath = logDir.resolve("logs.txt");
		if (Files.notExists(logPath)) {
			Files.createFile(logPath);
		}

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
		LOG.info("Sikuli StdOut/StdErr:");
		sikuliRunner.printStdOut();
		sikuliRunner.printStdErr();
		sikuliRunner.cleanup();

		copyScreenshots(componentId, info.getPid(), parentName);

		if (success == 0) {
			LOG.info("--------------- SUCCESSFULLY EXECUTED SIKULI SCRIPT! -----------------");
		}
		else {
			LOG.info("Sikuli returned Status code: " + success);
			throw new BWFLAException("Sikuli exited with non-zero status code!");
		}

	}


	public static void uploadSikuliScriptToEmulatorContainer(String componentId, String blobstoreURL) throws BWFLAException
	{

		LOG.info("Starting Sikuli Upload Task...");
		Path tmpPath = Path.of(RuncStateInformation.getRuncStateInformationForComponent(componentId).getBundle());
		SikuliUtils.extractTarToUploadDirectory(tmpPath, blobstoreURL);

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

	public static SikuliLogResponse getLogFiles(String componentId) throws IOException
	{

		var logFilePath = sikuliBasePath.resolve(componentId).resolve("logs.txt");

		ArrayList<String> lines = null;

		if (Files.exists(logFilePath)) {
			lines = (ArrayList<String>) Files.readAllLines(logFilePath);
		}

		var resp = new SikuliLogResponse();
		resp.setLines(lines);

		return resp;

	}

	public static ProcessResultUrl getDebugURL(String componentId) throws Exception
	{
		DeprecatedProcessRunner tarRunner = new DeprecatedProcessRunner("tar");
		tarRunner.setWorkingDirectory(sikuliBasePath.resolve(componentId));
		Path tgzPath = sikuliBasePath.resolve("sikuliDebug_" + componentId + ".tgz");
		tarRunner.addArguments("-zcvf", tgzPath.toString(), ".");
		tarRunner.execute(true);

		final Configuration config = ConfigurationProvider.getConfiguration();
		final BlobStore blobstore = BlobStoreClient.get()
				.getBlobStorePort(config.get("ws.blobstore"));
		final String blobStoreAddress = config.get("rest.blobstore");

		final BlobDescription blob = new BlobDescription()
				.setDescription("Sikuli Debug Screenshots and Logs")
				.setNamespace("Sikuli")
				.setDataFromFile(tgzPath)
				.setType(".tgz")
				.setName("sikuli_debug");

		BlobHandle handle = blobstore.put(blob);

		try {
			Files.delete(tgzPath);
		}
		catch (IOException e) {
			LOG.warning("Could not delete file!");
		}

		ProcessResultUrl returnResult = new ProcessResultUrl();
		returnResult.setUrl(handle.toRestUrl(blobStoreAddress));

		return returnResult;
	}

	public static Response getLastScreenshot(String componentId) throws Exception
	{
		//TODO better util functions to get folders...
		LOG.info("Getting latest screenshot...");
		RuncStateInformation info = RuncStateInformation.getRuncStateInformationForComponent(componentId);

		Path tmpPath = Path.of(info.getBundle());
		Path scriptPathAppserver = SikuliUtils.getSikuliFilenameForDirectory(tmpPath.resolve("data/uploads"));

		Path parentDir = scriptPathAppserver.getParent();
		Path parentName = parentDir.getName(parentDir.getNameCount() - 1);

		copyScreenshots(componentId, info.getPid(), parentName);


//		Path finalDir = Path.of("/proc").resolve(info.getPid()).resolve(Path.of("root/emucon/data/uploads")).resolve(parentName);
		Path finalDir = sikuliBasePath.resolve(componentId);

		LOG.info("Searching for screenshots in " + finalDir);
		final Pattern scPattern = Pattern.compile(".*sc\\d+\\.png");

		Optional<Path> scriptPathOpt = Files.find(finalDir,
						Integer.MAX_VALUE,
						(path, basicFileAttributes) -> scPattern.matcher(path.toString()).matches())
						.max(Comparator.comparingLong(f -> f.toFile().lastModified()));

		if (scriptPathOpt.isPresent()) {
			Path lastScreenshot = scriptPathOpt.get();
			LOG.info("Got path for latest screenshot: " + lastScreenshot);
			StreamingOutput stream = out -> {
				try (out) {
					out.write(Files.readAllBytes(lastScreenshot));
					out.flush();
				}
				finally {
					System.out.println("Closing!");
				}
			};

			return Response.status(Response.Status.OK)
					.entity(stream).build();
		}
		else {
			LOG.info("Not screenshots found (yet)!");
			throw new NotFoundException("Could not get Screenshot for componentId " + componentId + ".");
		}

	}

	private static void copyScreenshots(String componentId, String pid, Path targetDir)
	{
		DeprecatedProcessRunner screenshotRunner = new DeprecatedProcessRunner("sudo");
		screenshotRunner.addArgument("/libexec/findSikuliScreenshots.sh");
		screenshotRunner.addArgument(pid);
		screenshotRunner.addArgument(targetDir.toString());
		screenshotRunner.addArgument(componentId);
		screenshotRunner.execute();
	}
}

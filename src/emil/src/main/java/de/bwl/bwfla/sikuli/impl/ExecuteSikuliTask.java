package de.bwl.bwfla.sikuli.impl;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.sikuli.api.SikuliExecutionRequest;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


public class ExecuteSikuliTask extends BlockingTask<Object>
{

	private final SikuliExecutionRequest request;

	public ExecuteSikuliTask(SikuliExecutionRequest request)
	{
		this.request = request;
	}

	@Override
	protected Object execute() throws Exception
	{

		log.info("Executing Sikuli Script...");

		int x, y;
		if (request.getResolution() != null) {
			log.info("Got custom resolution...");
			x = request.getResolution().getX();
			y = request.getResolution().getY();
		}
		else {
			log.info("Resolution not specified, defaulting to 1280x1024");
			x = 1280;
			y = 1024;
		}


		log.info("Setting resolution to x: " + x + ", y: " + y);
		DeprecatedProcessRunner resolutionXpraRunner = new DeprecatedProcessRunner("sudo");
		resolutionXpraRunner.setLogger(log);
		resolutionXpraRunner.addArguments(
				"runc", "exec", "-e", "DISPLAY=:7000", "-e", "LD_PRELOAD=", request.getComponentId(),
				"sh", "-c", "x=\"$0\"; y=\"$1\"; export DISPLAY=:7000; xrandr --newmode \"${x}x${y}\" 0 \"$x\" 0 0 0 \"$y\" 0 0 0; xrandr --addmode screen \"${x}x${y}\"; xrandr -s \"${x}x${y}\"",
				Integer.toString(x), Integer.toString(y));
		resolutionXpraRunner.execute(true);

		if (request.isHeadless()) {
			Thread.sleep(1000);
			log.info("Component running in headless mode, exiting xpra...");
			DeprecatedProcessRunner exitXpraRunner = new DeprecatedProcessRunner("sudo");
			exitXpraRunner.setLogger(log);
			exitXpraRunner.addArguments(
					"runc", "exec", request.getComponentId(), "xpra", "exit", "socket://emucon/data/sockets/xpra-iosocket");
			exitXpraRunner.execute(true);
		}

		Thread.sleep(1000);

		RuncListInformation info = SikuliUtils.getRuncListInformationForComponent(request.getComponentId(), log);

		Path tmpPath = Path.of(info.getBundle());
		Path scriptPathAppserver = SikuliUtils.getSikuliFilenameForDirectory(tmpPath.resolve("data/uploads"));

		Path parentDir = scriptPathAppserver.getParent();
		Path parentName = parentDir.getName(parentDir.getNameCount() - 1);
		Path fileName = scriptPathAppserver.getFileName();

		Path scriptPathContainer = Path.of("/emucon/data/uploads/").resolve(parentName).resolve(fileName);
		log.info("Executing Sikuli Script at: " + scriptPathContainer);

		DeprecatedProcessRunner sikuliRunner = new DeprecatedProcessRunner("sudo");
		sikuliRunner.setLogger(log);
		sikuliRunner.addArguments(
				"runc", "exec", "-e", "DISPLAY=:7000", "-e", "LD_PRELOAD=", request.getComponentId(),
				"java", "-jar", "/sikulix.jar", "-c", "-r", scriptPathContainer.toString());

		boolean DEBUG = true; //TODO request

		if (DEBUG) {
			sikuliRunner.addArguments("--", "DEBUG");

			//TODO separate from DEBUG
			if (request.getParameters() != null && !request.getParameters().isEmpty()) {
				sikuliRunner.addArguments(request.getParameters());
			}
		}

		//Optional<DeprecatedProcessRunner.Result> result = sikuliRunner.executeWithResult(true);

		if (!sikuliRunner.start())
			throw new BWFLAException("Starting untar failed!");
		int success;

		log.info("--------- Starting reader! -----------");


		Path logDir = Path.of("/tmp-storage/automation/sikuli/" + getTaskId());
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
			throw new BWFLAException("Extracting tar archive failed!", error);
		}

		success = sikuliRunner.waitUntilFinished();
		sikuliRunner.cleanup();


//		String output = result.get().stdout();
//		boolean success = result.get().successful();
//
//
		DeprecatedProcessRunner screenshotRunner = new DeprecatedProcessRunner("sudo");
		screenshotRunner.addArgument("/libexec/findSikuliScreenshots.sh");
		screenshotRunner.addArgument(info.getPid());
		screenshotRunner.addArgument(parentName.toString());
		screenshotRunner.addArgument(getTaskId());
		screenshotRunner.execute();


		if (success == 0) {
			log.info("--------------- SUCCESSFULLY EXECUTED SIKULI SCRIPT! -----------------");

			if (request.isHeadless()) {
				//Thread.sleep(60000); //TODO properly check if component has shutdown correctly
				stopComponentAfterExecution();
			}

			return "Successfully executed Sikuli Script at: " + scriptPathContainer;
		}
		else {
			if (request.isHeadless()) {
				stopComponentAfterExecution();
			}
			throw new BWFLAException("Sikuli exited with non-zero status code!");
		}
	}

	public int stopComponentAfterExecution()
	{
		//TODO do this only if status is not stopped already!!!!
		return 0;
//		log.info("Stopping Component after Sikuli Execution");
//		var target = ClientBuilder.newClient().target("http://eaas:8080/emil");
//
//		Invocation.Builder restRequest = target.path("/components/" + request.getComponentId() + "/stop").request();
//
//		Response response = restRequest.get();
//		log.info("Stopping returned: " + response.getStatus());
//
//		return response.getStatus();

	}
}

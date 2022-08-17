package de.bwl.bwfla.sikuli.impl;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.emil.datatypes.rest.ComponentStateResponse;
import de.bwl.bwfla.emucomp.api.ComponentState;
import de.bwl.bwfla.sikuli.api.SikuliExecutionRequest;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
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
			throw new BWFLAException("IO Error while writing to log file!", error);
		}

		success = sikuliRunner.waitUntilFinished();
		log.info("Sikuli StdOut/StdErr");
		sikuliRunner.printStdOut();
		sikuliRunner.printStdErr();
		sikuliRunner.cleanup();

		DeprecatedProcessRunner screenshotRunner = new DeprecatedProcessRunner("sudo");
		screenshotRunner.addArgument("/libexec/findSikuliScreenshots.sh");
		screenshotRunner.addArgument(info.getPid());
		screenshotRunner.addArgument(parentName.toString());
		screenshotRunner.addArgument(getTaskId());
		screenshotRunner.execute();


		if (success == 0) {
			log.info("--------------- SUCCESSFULLY EXECUTED SIKULI SCRIPT! -----------------");

			if (request.isHeadless()) {

				stopComponentAfterExecution();
			}

			return "Successfully executed Sikuli Script at: " + scriptPathContainer;
		}
		else {
			log.info("Sikuli returned Status code: " + success);
			if (request.isHeadless()) {
				stopComponentAfterExecution();
			}
			throw new BWFLAException("Sikuli exited with non-zero status code!");
		}
	}

	public void stopComponentAfterExecution()
	{
		try {
			log.info("Waiting 30 seconds to see if component has shutdown properly!");
			Thread.sleep(30000);

			log.info("Stopping Component after Sikuli Execution");
			var target = ClientBuilder.newClient().target("http://eaas:8080/emil");

			Invocation.Builder stateRequest = target.path("/components/" + request.getComponentId() + "/state").request();
			Response stateResponse = stateRequest.get();
			var resp = stateResponse.readEntity(ComponentStateResponse.class);
			var state = resp.getState();
			if (state.equals(ComponentState.STOPPED.toString()) || state.equals(ComponentState.FAILED.toString())) {
				log.info("Component was properly stopped (or failed) after sikuli execution!");
				return;
			}

			log.info("Component was not properly stopped after sikuli execution, shutting down now! If the machine runs Win95/98, this is a common occurrence.");
			Invocation.Builder restRequest = target.path("/components/" + request.getComponentId() + "/stop").request();
			Response response = restRequest.get();
			response.getStatus();
			log.info("Stopping returned: " + response.getStatus());
		}
		catch (Exception e) {
			log.warning("Could not properly shutdown component (if necessary): " + e);
		}
	}
}

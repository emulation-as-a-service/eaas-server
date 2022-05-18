package de.bwl.bwfla.sikuli.impl;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.sikuli.api.SikuliExecutionRequest;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.nio.file.Path;


public class ExecuteSikuliAutomationTask extends BlockingTask<Object>
{

	private final SikuliExecutionRequest request;

	public ExecuteSikuliAutomationTask(SikuliExecutionRequest request)
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
			log.info("Resolution not specified, defaulting to 1024x768");
			x = 1024;
			y = 768;
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

		Path tmpPath = SikuliUtils.getWorkingDirForComponent(request.getComponentId(), log);
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
				"java", "-jar", "/sikulix.jar", "-r", scriptPathContainer.toString());

		boolean DEBUG = true; //TODO request

		if (DEBUG) {
			sikuliRunner.addArguments("--", "DEBUG");
		}

		if (sikuliRunner.execute(true)) {
			log.info("--------------- SUCCESSFULLY EXECUTED SIKULI SCRIPT! -----------------");
			//Files.createFile(Path.of("/tmp/automation/sikuli/done.txt"));
			log.info("Created done.txt!");

			if (request.isHeadless()) {
				Thread.sleep(60000); //TODO properly check if component has shutdown correctly
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
		log.info("Stopping Component after Sikuli Execution");
		var target = ClientBuilder.newClient().target("http://eaas:8080/emil");

		Invocation.Builder restRequest = target.path("/components/" + request.getComponentId() + "/stop").request();

		Response response = restRequest.get();
		log.info("Stopping returned: " + response.getStatus());

		return response.getStatus();

	}
}

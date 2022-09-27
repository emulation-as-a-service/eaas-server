package de.bwl.bwfla.automation.impl.sikuli;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class SikuliUtils
{

	private static final Pattern SCRIPT_FILENAME_PATTERN = Pattern.compile(".*/[\\w\\d-]+\\.sikuli/[\\w\\d-]+\\.py");

	public static Path getSikuliFilenameForDirectory(Path directory) throws Exception
	{


		Optional<Path> scriptPathOpt = Files.find(directory,
						Integer.MAX_VALUE,
						(path, basicFileAttributes) -> SCRIPT_FILENAME_PATTERN.matcher(path.toString()).matches())
				.findFirst();

		if (scriptPathOpt.isEmpty()) {
			throw new BWFLAException("Could not find Sikuli python Script for given directory: " + directory);
		}
		return scriptPathOpt.get();
	}

	public static void extractTar(Path workDir, String blobStoreUrl) throws BWFLAException
	{

		DeprecatedProcessRunner pr = new DeprecatedProcessRunner("curl");
		pr.addArguments("-L", "-o", workDir.toString() + "/out.tgz");
		pr.addArgument(blobStoreUrl);
		if (!pr.execute(true))
			throw new BWFLAException("failed to download " + blobStoreUrl);

		pr = new DeprecatedProcessRunner("sudo");
		pr.setWorkingDirectory(workDir.resolve("data/uploads"));
		pr.addArguments("tar", "xvf", workDir.toString() + "/out.tgz");
		if (!pr.execute(true))
			throw new BWFLAException("failed to extract tar");
	}

	public void stopComponentAfterExecution()
	{
		return;
		/*
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
		*/

	}


}

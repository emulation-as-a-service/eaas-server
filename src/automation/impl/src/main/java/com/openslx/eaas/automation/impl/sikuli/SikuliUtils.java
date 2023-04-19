package com.openslx.eaas.automation.impl.sikuli;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;


public class SikuliUtils
{

	private static final Pattern SCRIPT_FILENAME_PATTERN = Pattern.compile(".*[\\w\\d-]+\\.py");
	// Pattern.compile(".*/[\\w\\d-]+\\.+sikuli/[\\w\\d-]+\\.py");
	// TODO just use endswith (possible because dir is called sikuli always and not xyz.sikuli anymore)?


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

	//TODO a generalized version of this should probably be in commons
	public static void extractTarToUploadDirectory(Path workDir, String blobStoreUrl) throws BWFLAException
	{
		workDir = workDir.resolve("data/uploads/sikuli");
		try {
			if (!Files.exists(workDir)) {
				Files.createDirectory(workDir);
			}
			else {
				// FileUtils.cleanDirectory(workDir.toFile()); --- no permissions
				Runtime runtime = Runtime.getRuntime();
				var process = runtime.exec("sudo rm -rf " + workDir); //Process Runner does not support *
				process.waitFor();
				Files.createDirectory(workDir);
				//alternatively: could delete with Process Runner and then create folder again
			}

		}
		catch (IOException e) {
			System.out.println("----------- Could not create/clear Sikuli Directory ----------------");
			System.out.println(e);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}


		System.out.println("workdir after: " + workDir);

		DeprecatedProcessRunner pr = new DeprecatedProcessRunner("curl");
		pr.setWorkingDirectory(workDir);
		pr.addArguments("-L", "-o", "out.tgz");
		pr.addArgument(blobStoreUrl);
		if (!pr.execute(true))
			throw new BWFLAException("failed to download " + blobStoreUrl);

		pr = new DeprecatedProcessRunner("sudo");
		pr.setWorkingDirectory(workDir);
		pr.addArguments("tar", "xvf", "out.tgz");
		if (!pr.execute(true))
			throw new BWFLAException("failed to extract tar");

		pr = new DeprecatedProcessRunner("sudo");
		pr.setWorkingDirectory(workDir);
		pr.addArguments("rm", "./out.tgz");
		if (!pr.execute(true))
			throw new BWFLAException("failed to delete tar");

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
			log.warning("Could not properly shut down component (if necessary): " + e);
		}
		*/

	}


}
package de.bwl.bwfla.sikuli;

import de.bwl.bwfla.api.blobstore.BlobStore;
import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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
		pr.setWorkingDirectory(workDir.resolve("data/uploads"));
		pr.addArguments("tar", "xvf", workDir.toString() + "/out.tgz");
		if (!pr.execute(true))
			throw new BWFLAException("failed to extract tar");
	}

}

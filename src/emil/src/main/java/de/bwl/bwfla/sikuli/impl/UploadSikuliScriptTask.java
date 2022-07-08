package de.bwl.bwfla.sikuli.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.sikuli.api.SikuliUploadRequest;
import de.bwl.bwfla.sikuli.api.SikuliUploadResponse;

import java.nio.file.Path;


public class UploadSikuliScriptTask extends BlockingTask<Object> {

    private final SikuliUploadRequest request;

    public UploadSikuliScriptTask(SikuliUploadRequest request) {
        this.request = request;
    }


    @Override
    protected Object execute() throws Exception {

        log.info("Starting Sikuli Upload Task...");

        Path tmpPath = Path.of(SikuliUtils.getRuncListInformationForComponent(request.getComponentId(), log).getBundle());

        String blobStoreUrl = request.getBlobStoreUrl();
        extractTar(tmpPath, blobStoreUrl);

        Path scriptPath = SikuliUtils.getSikuliFilenameForDirectory(tmpPath.resolve("data/uploads"));

        DeprecatedProcessRunner reverseScriptRunner = new DeprecatedProcessRunner("sudo");
        reverseScriptRunner.setWorkingDirectory(tmpPath);
        reverseScriptRunner.addArguments("python3", "/libexec/sikuli-script-creator/reverse.py", scriptPath.toString());
        reverseScriptRunner.execute(true);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(tmpPath.resolve("reverse_script.json").toFile(), SikuliUploadResponse.class);
    }

    private void extractTar(Path workDir, String blobStoreUrl) throws BWFLAException {

        DeprecatedProcessRunner pr = new DeprecatedProcessRunner("curl");
        pr.setLogger(log);
        pr.addArguments("-L", "-o", workDir.toString() + "/out.tgz");
        pr.addArgument(blobStoreUrl);
        if (!pr.execute(true))
            throw new BWFLAException("failed to download " + blobStoreUrl);

        pr = new DeprecatedProcessRunner("sudo");
        pr.setLogger(log);
        pr.setWorkingDirectory(workDir.resolve("data/uploads"));
        pr.addArguments("tar", "xvf", workDir.toString() + "/out.tgz");
        if (!pr.execute(true))
            throw new BWFLAException("failed to extract tar");
    }


}

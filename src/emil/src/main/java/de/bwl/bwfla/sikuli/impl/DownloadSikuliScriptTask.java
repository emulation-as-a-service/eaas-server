package de.bwl.bwfla.sikuli.impl;

import de.bwl.bwfla.api.blobstore.BlobStore;
import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.emil.datatypes.rest.ProcessResultUrl;
import de.bwl.bwfla.sikuli.api.SikuliDownloadRequest;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

import java.nio.file.Path;

public class DownloadSikuliScriptTask extends BlockingTask<Object> {

    private final SikuliDownloadRequest request;

    public DownloadSikuliScriptTask(SikuliDownloadRequest request) {

        this.request = request;
    }

    @Override
    protected Object execute() throws Exception {

        log.info("Compressing Sikuli Script and uploading to Blobstore...");
        Path tmpPath = SikuliUtils.getWorkingDirForComponent(request.getComponentId(), log);

        DeprecatedProcessRunner tarRunner = new DeprecatedProcessRunner("tar");
        tarRunner.setLogger(log);
        tarRunner.setWorkingDirectory(tmpPath.resolve("data/uploads"));
        tarRunner.addArguments("-zcvf", tmpPath.resolve("sikulix.tar.gz").toString(), "output.sikuli");
        tarRunner.execute(true);

        final Configuration config = ConfigurationProvider.getConfiguration();
        final BlobStore blobstore = BlobStoreClient.get()
                .getBlobStorePort(config.get("ws.blobstore"));
        final String blobStoreAddress = config.get("rest.blobstore");

        final BlobDescription blob = new BlobDescription()
                .setDescription("SikuliX Script")
                .setNamespace("SikuliX")
                .setDataFromFile(tmpPath.resolve("sikulix.tar.gz"))
                .setType(".tgz")
                .setName("sikuliScript");

        BlobHandle handle = blobstore.put(blob);

        ProcessResultUrl returnResult = new ProcessResultUrl();
        returnResult.setUrl(handle.toRestUrl(blobStoreAddress));

        log.info("Blob Store Address for Sikuli Script: " + handle.toRestUrl(blobStoreAddress));

        return returnResult;

    }
}

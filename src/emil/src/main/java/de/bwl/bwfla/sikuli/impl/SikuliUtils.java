package de.bwl.bwfla.sikuli.impl;

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

public class SikuliUtils {

    private static final Pattern SCRIPT_FILENAME_PATTERN = Pattern.compile(".*/[\\w\\d-]+\\.sikuli/[\\w\\d-]+\\.py");

    public static Path getWorkingDirForComponent(String componentId, Logger logger) throws Exception {
        logger.info("Getting directory for component: " + componentId);
        DeprecatedProcessRunner runcListRunner = new DeprecatedProcessRunner("sudo");
        runcListRunner.addArguments("runc", "list", "--format", "json");

        Optional<DeprecatedProcessRunner.Result> result = runcListRunner.executeWithResult(true);
        String output = result.get().stdout();

        ObjectMapper runcMapper = new ObjectMapper();
        List<RuncListInformation> myObjects = Arrays.asList(runcMapper.readValue(output, RuncListInformation[].class));

        Optional<String> tmpPathOpt = myObjects.stream().filter(e -> e.getId().equals(componentId)).findFirst().map(RuncListInformation::getBundle);

        if (tmpPathOpt.isEmpty()) {
            throw new BWFLAException("Could not find session for given ID: " + componentId);
        }

        logger.info("Returning: " + tmpPathOpt.get());
        return Path.of(tmpPathOpt.get());
    }

    public static Path getSikuliFilenameForDirectory(Path directory) throws Exception {


        Optional<Path> scriptPathOpt = Files.find(directory,
                        Integer.MAX_VALUE,
                        (path, basicFileAttributes) -> SCRIPT_FILENAME_PATTERN.matcher(path.toString()).matches())
                .findFirst();

        if (scriptPathOpt.isEmpty()) {
            throw new BWFLAException("Could not find Sikuli python Script for given directory: " + directory);
        }
        return scriptPathOpt.get();
    }

}

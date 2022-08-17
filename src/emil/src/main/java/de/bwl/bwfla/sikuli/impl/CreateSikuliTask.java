package de.bwl.bwfla.sikuli.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.taskmanager.BlockingTask;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.sikuli.api.SikuliCreateScriptRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreateSikuliTask extends BlockingTask<Object> {

    private final SikuliCreateScriptRequest request;

    public CreateSikuliTask(SikuliCreateScriptRequest request) {
        this.request = request;
    }

    @Override
    protected Object execute() throws BWFLAException {

        log.info("Creating Sikuli Script...");

        ObjectWriter objectWriter = new ObjectMapper().writer().with(SerializationFeature.INDENT_OUTPUT).withDefaultPrettyPrinter();
        String json;
        try {
            json = objectWriter.writeValueAsString(request.getEntries());
        } catch (JsonProcessingException e) {
            throw new BWFLAException("Could not unmarshall entries to JSON Object", e);
        }

        Path workingDir;
        try {
            workingDir = Path.of(SikuliUtils.getRuncListInformationForComponent(request.getComponentId(), log).getBundle());
        } catch (Exception e) {
            throw new BWFLAException("Could not get working directory for component " + request.getComponentId(), e);
        }

        try {
            Files.writeString(workingDir.resolve("sikuli.json"), json);
        } catch (IOException e) {
            throw new BWFLAException("Could not write sikuli entries to JSON file.", e);
        }


        DeprecatedProcessRunner createScriptRunner = new DeprecatedProcessRunner("sudo");
        createScriptRunner.setLogger(log);
        createScriptRunner.setWorkingDirectory(workingDir);
        createScriptRunner.addArgument("python3");
        createScriptRunner.addArgument("/libexec/sikuli-script-creator/main.py");
        createScriptRunner.addArgument(workingDir.resolve("sikuli.json").toString());
        createScriptRunner.addArguments(workingDir.toString());
        if (createScriptRunner.execute(true)) {
            return "Successfully created sikuli script!";
        } else {
            throw new BWFLAException("Sikuli Script creation failed! Python script did not exit with status code 0.");
        }
    }
}
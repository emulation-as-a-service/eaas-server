package de.bwl.bwfla.emil.datatypes.rest;

import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.bwl.bwfla.api.imagebuilder.ImageBuilderMetadata;

import java.util.ArrayList;

/**
 * ImportContainerRequest
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportContainerRequest extends EmilRequestType {

    @XmlElement
    private String name;

    @XmlElement
    private CreateContainerImageRequest.ContainerType imageType;



    @XmlElement
    private String imageUrl;

    @XmlElement
    private ImageBuilderMetadata metadata;

    @XmlElement
    private String runtimeId;

    @XmlElement
    private String outputFolder;

    @XmlElement
    private String inputFolder;

    @XmlElement
    private String title;

    @XmlElement
    private ArrayList<String> processArgs;

    @XmlElement
    private ArrayList<String> processEnvs;

    @XmlElement
    private boolean guiRequired;

    @XmlElement
    private String description;

    @XmlElement
    private String author;

    
    private boolean enableNetwork;

    private String customSubdir;
    private boolean serviceContainer;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isEnableNetwork() {
        return enableNetwork;
    }

    public String getRuntimeId() {
        return runtimeId;
    }

    public boolean isServiceContainer() {
        return serviceContainer;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public boolean guiRequired() {
        return guiRequired;
    }

    public void setGuiRequired(boolean guiRequired) {
        this.guiRequired = guiRequired;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public ArrayList<String> getProcessArgs() {
        return processArgs;
    }

    public void setProcessArgs(ArrayList<String> processArgs) {
        this.processArgs = processArgs;
    }

    public ArrayList<String> getProcessEnvs() {
        return processEnvs;
    }

    public void setProcessEnvs(ArrayList<String> processEnvs) {
        this.processEnvs = processEnvs;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    public String getCustomSubdir() {
        return customSubdir;
    }

    public void setCustomSubdir(String customSubdir) {
        this.customSubdir = customSubdir;
    }

    public CreateContainerImageRequest.ContainerType getImageType() {
        return imageType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public ImageBuilderMetadata getMetadata() {
        return metadata;
    }
}
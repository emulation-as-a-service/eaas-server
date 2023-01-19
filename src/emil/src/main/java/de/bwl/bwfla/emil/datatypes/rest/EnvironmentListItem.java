package de.bwl.bwfla.emil.datatypes.rest;

import de.bwl.bwfla.emil.datatypes.EmilContainerEnvironment;
import de.bwl.bwfla.emil.datatypes.EmilEnvironment;
import de.bwl.bwfla.emil.datatypes.EmilObjectEnvironment;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class EnvironmentListItem {

    EnvironmentListItem() {}

    @XmlElement
    private String envId;
    
    @XmlElement
    private String title;
    
    @XmlElement
    private String archive;

    @XmlElement
    private String owner;

    @XmlElement
    private String objectId;

    @XmlElement
    private String objectArchive;

    @XmlElement
    private String envType;

    @XmlElement
    private String operatingSystem;

    @XmlElement
    private String timestamp;

    @XmlElement
    private String description;

    @XmlElement
    private boolean linuxRuntime;

    @XmlElement
    private boolean networkEnabled;

    @XmlElement
    private boolean internetEnabled;

    @XmlElement
    private boolean serviceContainer;

    @XmlElement
    private EmilEnvironment.OutputType hasOutput;

    public EnvironmentListItem(EmilEnvironment emilenv) {

        this.envId =  emilenv.getEnvId();
        this.title = emilenv.getTitle();
        this.archive = emilenv.getArchive();
        this.envType = "base";
        this.linuxRuntime = emilenv.isLinuxRuntime();
        this.timestamp = emilenv.getTimestamp();
        this.operatingSystem = emilenv.getOs();
        this.description = emilenv.getDescription();
        this.hasOutput = emilenv.getHasOutput();

        if (emilenv.getNetworking() != null) {
            this.networkEnabled = (emilenv.getNetworking().isConnectEnvs() || emilenv.getNetworking().isEnableInternet() || emilenv.getNetworking().isServerMode() || emilenv.getNetworking().isLocalServerMode());

            if(emilenv.getNetworking().isEnableInternet())
                this.internetEnabled = true;
        }
        else this.networkEnabled = false;

        if( emilenv.getOwner() != null)
           this.owner = emilenv.getOwner().getUsername();
        else
            this.owner = "shared";

        if(emilenv instanceof EmilObjectEnvironment)
        {
            EmilObjectEnvironment emilObjEnv = (EmilObjectEnvironment) emilenv;
            this.objectId = emilObjEnv.getObjectId();
            this.objectArchive = emilObjEnv.getObjectArchiveId();
            this.envType = "object";
        }

        if(emilenv instanceof EmilContainerEnvironment)
        {
            this.envType = "container";
            this.serviceContainer = ((EmilContainerEnvironment) emilenv).isServiceContainer();
        }
    }

    public boolean isLinuxRuntime() {
        return linuxRuntime;
    }

    public void setLinuxRuntime(boolean linuxRuntime) {
        linuxRuntime = linuxRuntime;
    }

    public String getEnvId() {
        return envId;
    }

    public void setEnvId(String envId) {
        this.envId = envId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArchive() {
        return archive;
    }

    public void setArchive(String archive) {
        this.archive = archive;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectArchive() {
        return objectArchive;
    }

    public void setObjectArchive(String objectArchive) {
        this.objectArchive = objectArchive;
    }

    public String getEnvType() {
        return envType;
    }

    public void setEnvType(String envType) {
        this.envType = envType;
    }

    public boolean isNetworkEnabled() {
        return networkEnabled;
    }

    public void setNetworkEnabled(boolean networkEnabled) {
        this.networkEnabled = networkEnabled;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public boolean isInternetEnabled() {
        return internetEnabled;
    }

    public boolean isServiceContainer() {
        return serviceContainer;
    }

    public EmilEnvironment.OutputType getHasOutput()
    {
        return hasOutput;
    }

    public void setHasOutput(EmilEnvironment.OutputType hasOutput)
    {
        this.hasOutput = hasOutput;
    }
}

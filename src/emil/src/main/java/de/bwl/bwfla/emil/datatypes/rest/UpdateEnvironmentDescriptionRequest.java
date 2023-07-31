package de.bwl.bwfla.emil.datatypes.rest;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.bwl.bwfla.emil.datatypes.EmilEnvironment;
import de.bwl.bwfla.emil.datatypes.EnvironmentCreateRequest;
import de.bwl.bwfla.emucomp.api.Drive;

import java.util.List;


@Entity
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateEnvironmentDescriptionRequest extends EmilRequestType
{
	private String envId;
	private String title;
	private String author;
	private String description;
	private String helpText;
	private String time;
	private String userTag;
	private String os;
	private String nativeConfig;
	private String containerEmulatorVersion;
	private String containerEmulatorName;

	private boolean enablePrinting;
	private boolean enableRelativeMouse;
	private boolean shutdownByOs;
	private boolean disableGhostCursor;
	private boolean useXpra;
	private boolean useWebRTC;
	private String xpraEncoding;
	private boolean canProcessAdditionalFiles;
	private List<Drive> drives;
	private boolean linuxRuntime;
	private EmilEnvironment.OutputType hasOutput;

	private List<EnvironmentCreateRequest.DriveSetting> driveSettings;

	public boolean isLinuxRuntime() {
		return linuxRuntime;
	}

	public void setLinuxRuntime(boolean islinuxRuntime) {
		linuxRuntime = islinuxRuntime;
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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getHelpText() {
		return helpText;
	}
	public void setHelpText(String helpText) {
		this.helpText = helpText;
	}
	public String getTime() { return time;}
	public void setTime(String time) {this.time = time; }

	public boolean isEnablePrinting() {
		return enablePrinting;
	}

	public boolean canProcessAdditionalFiles() {
		return canProcessAdditionalFiles;
	}

	public void setProcessAdditionalFiles(boolean canProcessAdditionalFiles) {
		this.canProcessAdditionalFiles = canProcessAdditionalFiles;
	}

	public void setEnablePrinting(boolean enablePrinting) {
		this.enablePrinting = enablePrinting;
	}

	public boolean isEnableRelativeMouse() {
		return enableRelativeMouse;
	}

	public void setEnableRelativeMouse(boolean enableRelativeMouse) {
		this.enableRelativeMouse = enableRelativeMouse;
	}

	public boolean isShutdownByOs() {
		return shutdownByOs;
	}

	public void setShutdownByOs(boolean shutdownByOs) {
		this.shutdownByOs = shutdownByOs;
	}

	public boolean isDisableGhostCursor()
	{
		return disableGhostCursor;
	}

	public void setDisableGhostCursor(boolean disableGhostCursor)
	{
		this.disableGhostCursor = disableGhostCursor;
	}

	public String getUserTag() {
		return userTag;
	}

	public void setUserTag(String userTag) {
		this.userTag = userTag;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

    public String getNativeConfig() {
		return nativeConfig;
    }

	public void setNativeConfig(String nativeConfig) {
		this.nativeConfig = nativeConfig;
	}

	public boolean isUseXpra() {
		return useXpra;
	}

	public void setUseXpra(boolean useXpra) {
		this.useXpra = useXpra;
	}

	public String getXpraEncoding() {
		return xpraEncoding;
	}

	public void setXpraEncoding(String xpraEncoding) {
		this.xpraEncoding = xpraEncoding;
	}

	public String getContainerEmulatorVersion() {
		return containerEmulatorVersion;
	}

	public void setContainerEmulatorVersion(String containerEmulatorVersion) {
		this.containerEmulatorVersion = containerEmulatorVersion;
	}

	public String getContainerEmulatorName() {
		return containerEmulatorName;
	}

	public void setContainerEmulatorName(String containerEmulatorName) {
		this.containerEmulatorName = containerEmulatorName;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public List<Drive> getDrives() {
		return drives;
	}

	public void setDrives(List<Drive> drives) {
		this.drives = drives;
	}

	public boolean isUseWebRTC() {
		return useWebRTC;
	}

	public void setUseWebRTC(boolean useWebRTC) {
		this.useWebRTC = useWebRTC;
	}

	public List<EnvironmentCreateRequest.DriveSetting> getDriveSettings() {
		return driveSettings;
	}

	public void setDriveSettings(List<EnvironmentCreateRequest.DriveSetting> driveSettings) {
		this.driveSettings = driveSettings;
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

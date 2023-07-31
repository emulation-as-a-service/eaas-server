package de.bwl.bwfla.emil.datatypes;

import javax.xml.bind.annotation.*;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import de.bwl.bwfla.common.utils.jaxb.JaxbType;
import de.bwl.bwfla.emil.datatypes.rest.NetworkingType;
import de.bwl.bwfla.common.services.security.EmilEnvironmentOwner;
import de.bwl.bwfla.common.services.security.EmilEnvironmentPermissions;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@XmlRootElement
@XmlSeeAlso({
	EmilObjectEnvironment.class,
	EmilContainerEnvironment.class,
	EmilSessionEnvironment.class
})
@XmlAccessorType(XmlAccessType.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
	@JsonSubTypes.Type(value = EmilEnvironment.class, name = EmilEnvironment.Names.ENVIRONMENT),
	@JsonSubTypes.Type(value = EmilEnvironment.class, name = EmilEnvironment.Names.ENVIRONMENT_COMPAT),
	@JsonSubTypes.Type(value = EmilObjectEnvironment.class, name = EmilEnvironment.Names.OBJECT_ENVIRONMENT),
	@JsonSubTypes.Type(value = EmilContainerEnvironment.class, name = EmilEnvironment.Names.CONTAINER_ENVIRONMENT),
	@JsonSubTypes.Type(value = EmilSessionEnvironment.class, name = EmilEnvironment.Names.SESSION),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmilEnvironment extends JaxbType implements Comparable<EmilEnvironment> {

	/*
	properties to be inherited (as defaults) by clones/derivatives
	 */
	@XmlElement(required = true)
	protected String envId;

	@XmlElement(required = false, defaultValue = "default")
	protected String archive = "default";

	@XmlElement(required = false)
	private String os;

	@XmlElement(required = false)
	private String title;

	@XmlElement(required = false)
	private String description;

	@XmlElement(required = false)
	private String version;

	@XmlElement(required = false)
	private String emulator;

	@XmlElement(required = false)
	private String timeContext;

	@XmlElement(required = false)
	private String author;

	@XmlElement(required = false)
	private boolean linuxRuntime;

	@XmlElement(required = false)
	private boolean enableRelativeMouse = false;

	@XmlElement(required = false)
	private boolean enablePrinting = false;

	@XmlElement(required = false)
	private boolean shutdownByOs = false;

	@XmlElement(required = false)
	private OutputType hasOutput;

	@XmlElement(required = false)
	private boolean disableGhostCursor = false;

	@XmlElement(required = false)
	private EmilEnvironmentOwner owner;

	@XmlElement(required = false)
	private EmilEnvironmentPermissions permissions;

	@XmlElement(required = false)
	private boolean canProcessAdditionalFiles;

	@XmlElement(required = false, defaultValue = "jpeg")
	private String xpraEncoding;

	@XmlElement(required = false)
	private NetworkingType networking;

	@XmlElement
	private String helpText;

	public enum OutputType{

		YES("yes"),
		NO("no"),
		ASK("ask");

		@JsonValue
		public final String value;

		OutputType(String value){
			this.value = value;
		}

	}

	public EmilEnvironment(EmilEnvironment template)
	{
		envId = template.envId;
		archive = template.archive;
		os = template.os;
		title = template.title;
		description = template.description;
		version = template.version;
		emulator = template.emulator;
		timeContext = template.timeContext;
		author = template.author;
		linuxRuntime = template.linuxRuntime;
		enableRelativeMouse = template.enableRelativeMouse;
		enablePrinting = template.enablePrinting;
		shutdownByOs = template.shutdownByOs;
		disableGhostCursor = template.disableGhostCursor;
		owner = template.owner;
		permissions = template.permissions;
		canProcessAdditionalFiles = template.canProcessAdditionalFiles;
		xpraEncoding = template.xpraEncoding;
		networking = template.networking;
		helpText = template.helpText;
		deleted = template.deleted;
		hasOutput = template.hasOutput;
	}

	/*
	additional properties to be set explicitly;
	 */
	@XmlElement(required = false)
	private String parentEnvId;

	@XmlElement(required = false)
	private Set<String> childrenEnvIds = new HashSet<>();

	@XmlElement(required = false)
	private Set<String> branches = new HashSet<>();

	@XmlElement(required = false)
	private String type = getClass().getCanonicalName();

	@XmlElement
	private String timestamp = Instant.now().toString();

	@XmlElement(required = false, defaultValue = "false")
	private boolean deleted = false;

	public EmilEnvironment() {}

	public String getArchive() {
		return archive;
	}

	public void setArchive(String archive) {
		this.archive = archive;
	}

	public String getTimeContext() {
		return timeContext;
	}
	public void setTimeContext(String timeContext) {
		this.timeContext = timeContext;
	}
	public boolean isEnableRelativeMouse() {
		return enableRelativeMouse;
	}
	public void setEnableRelativeMouse(boolean enableRelativeMouse) {
		this.enableRelativeMouse = enableRelativeMouse;
	}
	public String getParentEnvId() {
		return parentEnvId;
	}
	public void setParentEnvId(String envId) {
		this.parentEnvId = envId;
	}
	public String getEnvId() {
		return envId;
	}
	public void setEnvId(String envId) {
		this.envId = envId;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getOs() {
		return os;
	}
	public void setOs(String os) {
		this.os = os;
	}
	public String getEmulator() {
		return emulator;
	}
	public void setEmulator(String emulator) {
		this.emulator = emulator;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public boolean isEnablePrinting() {
		return enablePrinting;
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

	public void setEnablePrinting(boolean enablePrinting) {
		this.enablePrinting = enablePrinting;
	}

	public OutputType getHasOutput()
	{
		return hasOutput;
	}

	public void setHasOutput(OutputType hasOutput)
	{
		this.hasOutput = hasOutput;
	}

	@Override
	public int compareTo(EmilEnvironment other) {
		if(title == null || other == null || other.title == null)
			return -1;
		return title.compareTo(other.title);
	}
	
	public boolean isVisible() {
		if(childrenEnvIds == null || childrenEnvIds.size() == 0)
			return true;
		return false;
	}

	public Set<String> getChildrenEnvIds() {
		return childrenEnvIds;
	}

	public void addChildEnvId(String envId) {
		this.childrenEnvIds.add(envId);
	}
	public void removeChildEnvId(String envId) {
		this.childrenEnvIds.remove(envId);
	}

	public Set<String> getBranches() {
		return branches;
	}

	public void addBranchId(String envId) {
		this.branches.add(envId);
	}

	public void removeBranchEnvId(String envId) {
		this.branches.remove(envId);
	}

	@JsonIgnore
	public String getIdDBkey() {
		return idDBkey; // example: "envId" : "ad14d2bc-9ace-48df-b473-397dac19b2e915",
	}

    @JsonIgnore
	private static final String idDBkey = "envId";

	public EmilEnvironmentPermissions getPermissions() {
		return permissions;
	}

	public void setPermissions(EmilEnvironmentPermissions permissions) {
		this.permissions = permissions;
	}

	public EmilEnvironmentOwner getOwner() {
		return owner;
	}

	public void setOwner(EmilEnvironmentOwner owner) {
		this.owner = owner;
	}

	public boolean isLinuxRuntime() {
		return linuxRuntime;
	}

	public void setLinuxRuntime(boolean linuxRuntime) {
		this.linuxRuntime = linuxRuntime;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}


	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getXpraEncoding() {
		return xpraEncoding;
	}

	public void setXpraEncoding(String xpraEncoding) {
		this.xpraEncoding = xpraEncoding;
	}

	public NetworkingType getNetworking() {
		return networking;
	}

	public void setNetworking(NetworkingType networking) {
		this.networking = networking;
	}

	public String getHelpText() {
		return helpText;
	}

	public void setHelpText(String helpText) {
		this.helpText = helpText;
	}

	public boolean isCanProcessAdditionalFiles() {
		return canProcessAdditionalFiles;
	}

	public void setCanProcessAdditionalFiles(boolean canProcessAdditionalFiles) {
		this.canProcessAdditionalFiles = canProcessAdditionalFiles;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}


	/** Class names to use as type-information (compile-time constants) */
	public static class Names
	{
		public static final String ENVIRONMENT_COMPAT = "EmilEnvironment";
		public static final String ENVIRONMENT = "de.bwl.bwfla.emil.datatypes.EmilEnvironment";
		public static final String OBJECT_ENVIRONMENT = "de.bwl.bwfla.emil.datatypes.EmilObjectEnvironment";
		public static final String CONTAINER_ENVIRONMENT = "de.bwl.bwfla.emil.datatypes.EmilContainerEnvironment";
		public static final String SESSION = "de.bwl.bwfla.emil.datatypes.EmilSessionEnvironment";
	}
}

package de.bwl.bwfla.automation.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.bwl.bwfla.emil.datatypes.rest.ComponentWithExternalFilesRequest;

import java.util.ArrayList;


@JsonIgnoreProperties(ignoreUnknown = true)
public class AutomationRequest
{
	@JsonProperty("automationType")
	private String automationType;

	@JsonProperty("executableLocation")
	private String executableLocation;

	@JsonProperty("fileName")
	private String fileName;

	@JsonProperty("filePath")
	private String filePath;

	@JsonProperty("fileType")
	private String fileType;

	@JsonProperty("outputType")
	private String outputType;

	@JsonProperty("environmentId")
	private String environmentId;

	@JsonProperty("os")
	private String os;

	@JsonProperty("timeout")
	private int timeout;

	// if output = environment, store with name
	@JsonProperty("environmentName")
	private String environmentName;

	@JsonProperty("installAutostart")
	private boolean installAutostart;

	@JsonProperty("sikuliOnly")
	private boolean sikuliOnly;

	@JsonProperty("allFiles")
	private ArrayList<ComponentWithExternalFilesRequest.FileURL> allFiles;

	//migration only
	@JsonProperty("outputFileFormat")
	private String outputFileFormat;

	@JsonProperty("outputParameter")
	private String outputParameter;

	public AutomationRequest()
	{
	}

	public String getAutomationType()
	{
		return automationType;
	}

	public void setAutomationType(String automationType)
	{
		this.automationType = automationType;
	}

	public String getExecutableLocation()
	{
		return executableLocation;
	}

	public void setExecutableLocation(String executableLocation)
	{
		this.executableLocation = executableLocation;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public String getFilePath()
	{
		return filePath;
	}

	public String getFileType()
	{
		return fileType;
	}

	public void setFileType(String fileType)
	{
		this.fileType = fileType;
	}

	public void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}

	public String getOutputType()
	{
		return outputType;
	}

	public void setOutputType(String outputType)
	{
		this.outputType = outputType;
	}

	public String getEnvironmentId()
	{
		return environmentId;
	}

	public void setEnvironmentId(String environmentId)
	{
		this.environmentId = environmentId;
	}

	public String getOs()
	{
		return os;
	}

	public void setOs(String os)
	{
		this.os = os;
	}

	public int getTimeout()
	{
		return timeout;
	}

	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

	public String getEnvironmentName()
	{
		return environmentName;
	}

	public void setEnvironmentName(String environmentName)
	{
		this.environmentName = environmentName;
	}

	public boolean isInstallAutostart()
	{
		return installAutostart;
	}

	public void setInstallAutostart(boolean installAutostart)
	{
		this.installAutostart = installAutostart;
	}

	public boolean isSikuliOnly()
	{
		return sikuliOnly;
	}

	public void setSikuliOnly(boolean sikuliOnly)
	{
		this.sikuliOnly = sikuliOnly;
	}

	public ArrayList<ComponentWithExternalFilesRequest.FileURL> getAllFiles()
	{
		return allFiles;
	}

	public void setAllFiles(ArrayList<ComponentWithExternalFilesRequest.FileURL> allFiles)
	{
		this.allFiles = allFiles;
	}

	public String getOutputFileFormat()
	{
		return outputFileFormat;
	}

	public void setOutputFileFormat(String outputFileFormat)
	{
		this.outputFileFormat = outputFileFormat;
	}

	public String getOutputParameter()
	{
		return outputParameter;
	}

	public void setOutputParameter(String outputParameter)
	{
		this.outputParameter = outputParameter;
	}
}

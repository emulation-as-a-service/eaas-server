package de.bwl.bwfla.automation.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.bwl.bwfla.emil.datatypes.rest.ComponentWithExternalFilesRequest;

import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;


@JsonIgnoreProperties(ignoreUnknown = true)
@XmlType(name = "AutomationTemplateRequest")
public class AutomationTemplateRequest extends AutomationBaseRequest
{

	@JsonProperty("fileName")
	protected String fileName;

	@JsonProperty("filePath")
	protected String filePath;

	@JsonProperty("fileType")
	protected String fileType;

	@JsonProperty("automationType")
	protected String automationType;

	@JsonProperty("executableLocation")
	protected String executableLocation;

	@JsonProperty("installAutostart")
	protected boolean installAutostart;

	//migration only
	@JsonProperty("outputFileFormat")
	protected String outputFileFormat;

	@JsonProperty("outputParameter")
	protected String outputParameter;

	@JsonProperty("allFiles")
	protected ArrayList<ComponentWithExternalFilesRequest.FileURL> allFiles;


	public AutomationTemplateRequest()
	{
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

	public void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}

	public String getFileType()
	{
		return fileType;
	}

	public void setFileType(String fileType)
	{
		this.fileType = fileType;
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

	public boolean isInstallAutostart()
	{
		return installAutostart;
	}

	public void setInstallAutostart(boolean installAutostart)
	{
		this.installAutostart = installAutostart;
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

	public ArrayList<ComponentWithExternalFilesRequest.FileURL> getAllFiles()
	{
		return allFiles;
	}

	public void setAllFiles(ArrayList<ComponentWithExternalFilesRequest.FileURL> allFiles)
	{
		this.allFiles = allFiles;
	}
}

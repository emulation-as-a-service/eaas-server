package de.bwl.bwfla.automation.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;



@JsonIgnoreProperties(ignoreUnknown = true)
public class AutomationConfigurationRequest
{
	@JsonProperty("baseUrl")
	private String baseUrl;

	@JsonProperty("repoUrl")
	private String repoUrl;

	@JsonProperty("repoDirectory")
	private String repoDirectory;

	@JsonProperty("storageDirectory")
	private String storageDirectory;


	public AutomationConfigurationRequest()
	{
	}

	public String getBaseUrl()
	{
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl)
	{
		this.baseUrl = baseUrl;
	}

	public String getRepoUrl()
	{
		return repoUrl;
	}

	public void setRepoUrl(String repoUrl)
	{
		this.repoUrl = repoUrl;
	}

	public String getRepoDirectory()
	{
		return repoDirectory;
	}

	public void setRepoDirectory(String repoDirectory)
	{
		this.repoDirectory = repoDirectory;
	}

	public String getStorageDirectory()
	{
		return storageDirectory;
	}

	public void setStorageDirectory(String storageDirectory)
	{
		this.storageDirectory = storageDirectory;
	}
}

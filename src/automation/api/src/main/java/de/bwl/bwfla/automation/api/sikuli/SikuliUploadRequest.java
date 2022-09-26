package de.bwl.bwfla.automation.api.sikuli;

import com.fasterxml.jackson.annotation.JsonProperty;


public class SikuliUploadRequest implements SikuliRequest
{
	public SikuliUploadRequest()
	{
	}

	@JsonProperty("componentId")
	private String componentId;

	@JsonProperty("blobStoreUrl")
	private String blobStoreUrl;

	@JsonProperty(value = "computeUiFields", defaultValue = "false")
	private boolean computeUiFields;

	public String getComponentId()
	{
		return componentId;
	}

	public void setComponentId(String componentId)
	{
		this.componentId = componentId;
	}

	public String getBlobStoreUrl()
	{
		return blobStoreUrl;
	}

	public void setBlobStoreUrl(String blobStoreUrl)
	{
		this.blobStoreUrl = blobStoreUrl;
	}

	public boolean isComputeUiFields()
	{
		return computeUiFields;
	}

	public void setComputeUiFields(boolean computeUiFields)
	{
		this.computeUiFields = computeUiFields;
	}
}

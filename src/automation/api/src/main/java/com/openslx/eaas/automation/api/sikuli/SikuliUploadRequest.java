package com.openslx.eaas.automation.api.sikuli;

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

	@JsonProperty("objectId")
	private String objectId;

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

	public String getObjectId()
	{
		return objectId;
	}

	public void setObjectId(String objectId)
	{
		this.objectId = objectId;
	}
}

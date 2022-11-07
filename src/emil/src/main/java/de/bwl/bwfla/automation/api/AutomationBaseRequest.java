package de.bwl.bwfla.automation.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.bwl.bwfla.emil.datatypes.rest.*;

import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;


@XmlSeeAlso({AutomationSikuliRequest.class,
		AutomationTemplateRequest.class})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AutomationBaseRequest
{
	@JsonProperty("name")
	protected String name;

	@JsonProperty("environmentId")
	protected String environmentId;

	@JsonProperty("componentId")
	protected String componentId;

	@JsonProperty("outputType") // Files or Environment
	protected String outputType;

	// if output = environment, store with name
	@JsonProperty("environmentName")
	protected String environmentName;

	@JsonProperty("timeout")
	protected int timeout;

	public AutomationBaseRequest()
	{
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getEnvironmentId()
	{
		return environmentId;
	}

	public void setEnvironmentId(String environmentId)
	{
		this.environmentId = environmentId;
	}

	public String getComponentId()
	{
		return componentId;
	}

	public void setComponentId(String componentId)
	{
		this.componentId = componentId;
	}

	public String getOutputType()
	{
		return outputType;
	}

	public void setOutputType(String outputType)
	{
		this.outputType = outputType;
	}

	public String getEnvironmentName()
	{
		return environmentName;
	}

	public void setEnvironmentName(String environmentName)
	{
		this.environmentName = environmentName;
	}

	public int getTimeout()
	{
		return timeout;
	}

	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

}

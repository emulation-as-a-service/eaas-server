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

	@JsonProperty("targetId")
	protected String targetId;

	@JsonProperty(value = "targetIsComponent", defaultValue = "false")
	protected boolean targetIsComponent;

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

	public String getTargetId()
	{
		return targetId;
	}

	public void setTargetId(String targetId)
	{
		this.targetId = targetId;
	}

	public boolean isTargetIsComponent()
	{
		return targetIsComponent;
	}

	public void setTargetIsComponent(boolean targetIsComponent)
	{
		this.targetIsComponent = targetIsComponent;
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

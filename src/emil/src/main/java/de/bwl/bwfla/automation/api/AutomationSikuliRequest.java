package de.bwl.bwfla.automation.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;


@JsonIgnoreProperties(ignoreUnknown = true)
@XmlType(name = "AutomationSikuliRequest")
public class AutomationSikuliRequest extends AutomationBaseRequest
{

	@JsonProperty("sikuliUrl")
	protected String sikuliUrl;

	//sikuli only
	@JsonProperty("customOutputs")
	protected ArrayList<String> customOutputs;



	@JsonProperty("sikuliParams")
	private ArrayList<String> sikuliParams;

	@JsonProperty("resolutionX")
	protected int resolutionX;

	@JsonProperty("resolutionY")
	protected int resolutionY;

	public AutomationSikuliRequest()
	{
	}

	public String getSikuliUrl()
	{
		return sikuliUrl;
	}

	public void setSikuliUrl(String sikuliUrl)
	{
		this.sikuliUrl = sikuliUrl;
	}

	public ArrayList<String> getCustomOutputs()
	{
		return customOutputs;
	}

	public void setCustomOutputs(ArrayList<String> customOutputs)
	{
		this.customOutputs = customOutputs;
	}

	public int getResolutionX()
	{
		return resolutionX;
	}

	public void setResolutionX(int resolutionX)
	{
		this.resolutionX = resolutionX;
	}

	public int getResolutionY()
	{
		return resolutionY;
	}

	public void setResolutionY(int resolutionY)
	{
		this.resolutionY = resolutionY;
	}

	public ArrayList<String> getSikuliParams()
	{
		return sikuliParams;
	}

	public void setSikuliParams(ArrayList<String> sikuliParams)
	{
		this.sikuliParams = sikuliParams;
	}
}

package com.openslx.automation.api.sikuli;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class SikuliLogResponse
{
	@JsonProperty("lines")
	ArrayList<String> lines;

	public ArrayList<String> getLines()
	{
		return lines;
	}

	public void setLines(ArrayList<String> lines)
	{
		this.lines = lines;
	}
}

package de.bwl.bwfla.automation.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;


public class AllAutomationsResponse
{
	@JsonProperty("results")
	private ArrayList<AutomationResult> results;


	public static class PythonResultFile
	{
		@JsonProperty("automationType")
		private String automationType;
		@JsonProperty("result")

		private String result;

		public PythonResultFile()
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

		public String getResult()
		{
			return result;
		}

		public void setResult(String result)
		{
			this.result = result;
		}
	}


	public static class AutomationResult
	{

		@JsonProperty("automationType")
		private String automationType;

		@JsonProperty("result")
		private String result;

		@JsonProperty("id")
		private String id;

		@JsonProperty("isDone")
		private boolean isDone;

		@JsonProperty("originalEnvId")
		private String originalEnvId;

		@JsonProperty("executable")
		private String executable;

		@JsonProperty("status")
		private String status;

		@JsonProperty("error")
		private boolean error;

		public AutomationResult()
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

		public String getResult()
		{
			return result;
		}

		public void setResult(String result)
		{
			this.result = result;
		}

		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public boolean isDone()
		{
			return isDone;
		}

		public void setIsDone(boolean done)
		{
			isDone = done;
		}

		public String getOriginalEnvId()
		{
			return originalEnvId;
		}

		public void setOriginalEnvId(String originalEnvId)
		{
			this.originalEnvId = originalEnvId;
		}

		public String getExecutable()
		{
			return executable;
		}

		public void setExecutable(String executable)
		{
			this.executable = executable;
		}

		public String getStatus()
		{
			return status;
		}

		public void setStatus(String status)
		{
			this.status = status;
		}

		public boolean isError()
		{
			return error;
		}

		public void setError(boolean error)
		{
			this.error = error;
		}
	}

	public AllAutomationsResponse()
	{
	}

	public ArrayList<AutomationResult> getResults()
	{
		return results;
	}

	public void setResults(ArrayList<AutomationResult> results)
	{
		this.results = results;
	}
}

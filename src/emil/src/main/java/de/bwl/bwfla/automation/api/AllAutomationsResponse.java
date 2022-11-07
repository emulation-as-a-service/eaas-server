package de.bwl.bwfla.automation.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;


@JsonIgnoreProperties(ignoreUnknown = true)
public class AllAutomationsResponse
{
	@JsonProperty("results")
	private ArrayList<AutomationResult> results;


	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PythonResultFile
	{
		@JsonProperty("automationType")
		private String automationType;

		@JsonProperty("result")
		private String result;

		@JsonProperty("sikuliTaskId")
		private String sikuliTaskId;

		@JsonProperty("hasError")
		private boolean hasError;

		@JsonProperty("componentId")
		private String componentId;

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

		public String getSikuliTaskId()
		{
			return sikuliTaskId;
		}

		public void setSikuliTaskId(String sikuliTaskId)
		{
			this.sikuliTaskId = sikuliTaskId;
		}

		public boolean isHasError()
		{
			return hasError;
		}

		public void setHasError(boolean hasError)
		{
			this.hasError = hasError;
		}

		public String getComponentId()
		{
			return componentId;
		}

		public void setComponentId(String componentId)
		{
			this.componentId = componentId;
		}
	}


	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class AutomationResult
	{

		@JsonProperty("name")
		private String name;

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

		@JsonProperty("componentId")
		private String componentId;

		@JsonProperty("executable")
		private String executable;

		@JsonProperty("status")
		private String status;

		@JsonProperty("error")
		private boolean error;

		@JsonProperty("sikuliTaskId")
		private String sikuliTaskId;

		@JsonProperty("logs")
		private ArrayList<String> logs;

		@JsonProperty("startedAt")
		private String startedAt;

		@JsonProperty("finishedAt")
		private String finishedAt;

		@JsonProperty("duration")
		private String duration;

		public AutomationResult()
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

		public void setDone(boolean done)
		{
			isDone = done;
		}

		public String getSikuliTaskId()
		{
			return sikuliTaskId;
		}

		public void setSikuliTaskId(String sikuliTaskId)
		{
			this.sikuliTaskId = sikuliTaskId;
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

		public String getComponentId()
		{
			return componentId;
		}

		public void setComponentId(String componentId)
		{
			this.componentId = componentId;
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

		public ArrayList<String> getLogs()
		{
			return logs;
		}

		public void setLogs(ArrayList<String> logs)
		{
			this.logs = logs;
		}

		public String getStartedAt()
		{
			return startedAt;
		}

		public void setStartedAt(String startedAt)
		{
			this.startedAt = startedAt;
		}

		public String getFinishedAt()
		{
			return finishedAt;
		}

		public void setFinishedAt(String finishedAt)
		{
			this.finishedAt = finishedAt;
		}

		public String getDuration()
		{
			return duration;
		}

		public void setDuration(String duration)
		{
			this.duration = duration;
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

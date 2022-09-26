package de.bwl.bwfla.common.utils.apiutils;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WaitQueueCreatedResponse
{
    @JsonProperty("taskId")
    private String taskId;

    @JsonProperty("waitQueueUrl")
    private String waitQueueUrl;

    public WaitQueueCreatedResponse() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getWaitQueueUrl() {
        return waitQueueUrl;
    }

    public void setWaitQueueUrl(String waitQueueUrl) {
        this.waitQueueUrl = waitQueueUrl;
    }
}

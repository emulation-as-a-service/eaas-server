package de.bwl.bwfla.sikuli.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SikuliDownloadRequest {
    public SikuliDownloadRequest() {
    }

    @JsonProperty("componentId")
    private String componentId;

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

}

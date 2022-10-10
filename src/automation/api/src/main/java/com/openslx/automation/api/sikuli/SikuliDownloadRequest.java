package com.openslx.automation.api.sikuli;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SikuliDownloadRequest implements SikuliRequest{
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

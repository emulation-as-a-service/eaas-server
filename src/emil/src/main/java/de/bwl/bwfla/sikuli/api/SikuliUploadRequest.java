package de.bwl.bwfla.sikuli.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SikuliUploadRequest {
    public SikuliUploadRequest() {
    }

    @JsonProperty("componentId")
    private String componentId;

    @JsonProperty("blobStoreUrl")
    private String blobStoreUrl;

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getBlobStoreUrl() {
        return blobStoreUrl;
    }

    public void setBlobStoreUrl(String blobStoreUrl) {
        this.blobStoreUrl = blobStoreUrl;
    }
}

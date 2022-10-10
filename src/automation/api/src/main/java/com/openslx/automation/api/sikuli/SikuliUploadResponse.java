package com.openslx.automation.api.sikuli;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SikuliUploadResponse {

    @JsonProperty("images")
    private Map<String, Object> images;

    @JsonProperty("elements")
    private List<Object> elements;

    public Map<String, Object> getImages() {
        return images;
    }

    public void setImages(Map<String, Object> images) {
        this.images = images;
    }

    public List<Object> getElements() {
        return elements;
    }

    public void setElements(List<Object> elements) {
        this.elements = elements;
    }

    public SikuliUploadResponse() {
    }

}

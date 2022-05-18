package de.bwl.bwfla.sikuli.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SikuliCreateScriptRequest {

    @JsonProperty("componentId")
    private String componentId;

    @JsonProperty("entries")
    private ScriptEntries entries;

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public ScriptEntries getEntries() {
        return entries;
    }

    public void setEntries(ScriptEntries entries) {
        this.entries = entries;
    }

    public SikuliCreateScriptRequest() {
    }

    public class ScriptEntries {
        @JsonProperty("images")
        private Map<String, Object> images;

        @JsonProperty("elements")
        private Map<String, Object> elements;

        public Map<String, Object> getImages() {
            return images;
        }

        public void setImages(Map<String, Object> images) {
            this.images = images;
        }


        public Map<String, Object> getElements() {
            return elements;
        }

        public void setElements(Map<String, Object> elements) {
            this.elements = elements;
        }

        public ScriptEntries() {
        }
    }
}
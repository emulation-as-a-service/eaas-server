package de.bwl.bwfla.sikuli.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SikuliExecutionRequest {
    public SikuliExecutionRequest() {
    }

    @JsonProperty("componentId")
    private String componentId;

    @JsonProperty(value = "headless", defaultValue = "false")
    private boolean headless;

    @JsonProperty("resolution")
    private Resolution resolution;

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    public class Resolution {
        @JsonProperty("x")
        private int x;

        @JsonProperty("y")
        private int y;

        public Resolution() {
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }
}

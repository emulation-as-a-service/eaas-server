package de.bwl.bwfla.emucomp.components.emulators;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openslx.eaas.common.databind.DataUtils;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmulatorRuntimeConfiguration {

    private List<HardwareComponent> hardwareComponents;
    private List<FrameworkComponent> frameworkComponents;
    private List<List<String>> nativeConfig;
    private String machine;

    @JsonGetter(Fields.HARDWARE_COMPONENTS)
    public List<HardwareComponent> getHardwareComponents() {
        if(hardwareComponents == null)
            hardwareComponents = new ArrayList<>();
        return hardwareComponents;
    }

    @JsonGetter(Fields.FRAMEWORK_COMPONENTS)
    public List<FrameworkComponent> getFrameworkComponents() {
        if(frameworkComponents == null)
            frameworkComponents = new ArrayList<>();
        return frameworkComponents;
    }

    @JsonGetter(Fields.NATIVE_CONFIG)
    public List<List<String>> getNativeConfig() {
        if(nativeConfig == null)
            nativeConfig = new ArrayList<>();
        return nativeConfig;
    }

    @JsonGetter(Fields.MACHINE)
    public String getMachine() {
        return machine;
    }

    @JsonSetter(Fields.MACHINE)
    public EmulatorRuntimeConfiguration setMachine(String machine) {
        this.machine = machine;
        return this;
    }

    @JsonInclude
    abstract static class Component {
        private String component;
        private String path;
        private List<String> nativeConfig;

        @JsonGetter(Fields.COMPONENT)
        public String getComponent() {
            return component;
        }

        @JsonSetter(Fields.COMPONENT)
        public void setComponent(String component) {
            this.component = component;
        }

        @JsonGetter(Fields.PATH)
        public String getPath() {
            return path;
        }

        @JsonSetter(Fields.PATH)
        public void setPath(String path) {
            this.path = path;
        }

        @JsonGetter(Fields.NATIVE_CONFIG)
        public List<String> getNativeConfig() {
            return nativeConfig;
        }

        @JsonSetter(Fields.NATIVE_CONFIG)
        public void setNativeConfig(List<String> nativeConfig) {
            this.nativeConfig = nativeConfig;
        }
    }

    @JsonInclude
    public static class HardwareComponent extends Component {
        private long index;

        @JsonGetter(Fields.INDEX)
        public long getIndex() {
            return index;
        }

        @JsonSetter(Fields.INDEX)
        public void setIndex(long index) {
            this.index = index;
        }
    }

    @JsonInclude
    public static class FrameworkComponent extends Component {
    }

    private static final class Fields
	{
		public static final String MACHINE          = "machine";
		public static final String HARDWARE_COMPONENTS = "hardwareComponents";
		public static final String FRAMEWORK_COMPONENTS = "frameworkComponents";
		public static final String NATIVE_CONFIG = "nativeConfig";
		public static final String COMPONENT = "component";
		public static final String PATH = "path";

		public static final String INDEX = "index";
	}

	@Override
    public String toString()
    {
        try {
            System.out.println("machine " + this.machine);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (Exception error) {
            throw new RuntimeException("Serializing user-info failed!", error);
        }
    }
}

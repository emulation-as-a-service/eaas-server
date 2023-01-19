package de.bwl.bwfla.emil.datatypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkEnvironmentElement {

    @XmlElement(required = true)
    private String envId;

    @XmlElement(required = false)
    private String macAddress;

    @XmlElement(required = false)
    private List<Short> serverPorts;

    @XmlElement(required = false)
    private String serverIp;

    @XmlElement(required = false)
    private boolean wildcard;

    @XmlElement(required = false)
    private boolean hasOutput;

    @XmlElement(required = true)
    private String label;

    @XmlElement(required = false)
    private String title;

    @XmlElement
    private String fqdn;

    public boolean isHasOutput() {
        return hasOutput;
    }

    public void setHasOutput(boolean hasOutput) {
        this.hasOutput = hasOutput;
    }

    public String getEnvId() {
        return envId;
    }

    public void setEnvId(String envId) {
        this.envId = envId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Short> getServerPorts() {
        return serverPorts;
    }

    public void setServerPorts(List<Short> serverPorts) {
        this.serverPorts = serverPorts;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    @Deprecated
    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public void setWildcard(boolean wildcard) {
        this.wildcard = wildcard;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String[] getFqdnList() {
        if(fqdn == null)
            return null;
        String[] fqdns = fqdn.split(":");
        if(fqdns.length == 0)
            return null;
        return fqdns;
    }
}

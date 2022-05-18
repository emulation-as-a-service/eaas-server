package de.bwl.bwfla.sikuli.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RuncListInformation {

    @JsonProperty("ociVersion")
    private String ociVersion;

    @JsonProperty("id")
    private String id;

    @JsonProperty("pid")
    private String pid;

    @JsonProperty("status")
    private String status;

    @JsonProperty("bundle")
    private String bundle;

    @JsonProperty("rootfs")
    private String rootfs;

    @JsonProperty("created")
    private String created;

    @JsonProperty("owner")
    private String owner;

    public RuncListInformation() {
    }

    public String getOciVersion() {
        return ociVersion;
    }

    public void setOciVersion(String ociVersion) {
        this.ociVersion = ociVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getRootfs() {
        return rootfs;
    }

    public void setRootfs(String rootfs) {
        this.rootfs = rootfs;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}

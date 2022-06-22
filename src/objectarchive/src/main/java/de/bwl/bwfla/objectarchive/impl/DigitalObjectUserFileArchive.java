package de.bwl.bwfla.objectarchive.impl;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import org.apache.tamaya.inject.ConfigurationInjection;
import org.apache.tamaya.inject.api.Config;

import javax.inject.Inject;
import java.io.File;

public class DigitalObjectUserFileArchive extends DigitalObjectFileArchive {

    @Inject
    @Config(value="objectarchive.userarchive")
    public String userArchiveBase;


    public DigitalObjectUserFileArchive(String name) throws BWFLAException {
        ConfigurationInjection.getConfigurationInjector().configure(this);
        File userPath = new File(userArchiveBase, name);
        if(!userPath.exists())
        {
            if(!userPath.mkdirs())
                throw new BWFLAException("can not create user object dir:" + userPath.getAbsolutePath());
        }
        init(name, userPath.getAbsolutePath(), false);
    }
}

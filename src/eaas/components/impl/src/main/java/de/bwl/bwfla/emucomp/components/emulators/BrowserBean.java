package de.bwl.bwfla.emucomp.components.emulators;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.common.utils.NetworkUtils;
import de.bwl.bwfla.emucomp.api.Drive;
import de.bwl.bwfla.emucomp.api.EmulatorUtils;
import de.bwl.bwfla.emucomp.api.MachineConfiguration;
import de.bwl.bwfla.emucomp.api.Nic;

public class BrowserBean extends EmulatorBean {

    private DeprecatedProcessRunner proxyRunner;
    private String nic;

    @Override
    protected void prepareEmulatorRunner() throws BWFLAException {


        // disable fake clock, otherwise it will cause rendering issues
        this.disableFakeClock = true;

        emuRunner.setCommand("/usr/local/bin/eaas-browser");
        emuRunner.addEnvVariable("MAC", NetworkUtils.getRandomHWAddress());

        String config = this.getNativeConfig();
        if (config != null && !config.isEmpty()) {
            String[] tokens = config.trim().split("\\s+");
            for (String token : tokens)
            {
                if(token.isEmpty())
                    continue;

                emuRunner.addArgument(token.trim());
            }
        }
    }

    @Override
    protected String getEmuContainerName(MachineConfiguration env)
    {
        return "browser";
    }

    @Override
    protected boolean addDrive(Drive drive) {
        return false;
    }

    @Override
    protected boolean connectDrive(Drive drive, boolean attach) {
        return false;
    }

    @Override
    protected boolean addNic(Nic nic) {
        if (nic == null) {
            LOG.warning("NIC is null, attach canceled.");
            return false;
        }

        this.nic = nic.getHwaddress();
        emuRunner.addEnvVariable("NIC",  nic.getHwaddress());
        return true;
    }

    @Override
    protected void stopInternal()
    {
        if(proxyRunner != null && proxyRunner.isProcessRunning())
            stopProcessRunner(proxyRunner);

        super.stopInternal();
    }
}

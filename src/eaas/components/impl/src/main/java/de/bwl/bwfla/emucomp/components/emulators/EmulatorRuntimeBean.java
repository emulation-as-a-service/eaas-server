package de.bwl.bwfla.emucomp.components.emulators;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.emucomp.api.Drive;
import de.bwl.bwfla.emucomp.api.MachineConfiguration;
import de.bwl.bwfla.emucomp.api.Nic;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmulatorRuntimeBean extends EmulatorBean
{
	private final EmulatorRuntimeConfiguration config = new EmulatorRuntimeConfiguration();
	private String machine = null;
	private long componentIndex = 0;

    @Override
	protected void setRuntimeConfiguration(MachineConfiguration environment) throws BWFLAException
	{
		String machineName = LegacyBean2Machine.emulatorMachineMap.get(environment.getEmulator().getBean());
		String PREFIX = "emulators:";
		String emulatorPrefix = PREFIX + machineName;
    	this.machine = emulatorPrefix + "#" + (environment.getArch() != null ? environment.getArch() : "default");

		super.setRuntimeConfiguration(environment);
        config.setMachine(machine);
	}

	@Override
	protected String getEmuContainerName(MachineConfiguration machineConfiguration)
	{
		return LegacyBean2Machine.emulatorContainerMap.get(machineConfiguration.getEmulator().getBean());
	}

	@Override
	public void prepareEmulatorRunner() throws BWFLAException
	{
        if(emuEnvironment.getNativeConfig() != null && emuEnvironment.getNativeConfig().getValue() != null ) {
            String nativeConfig = emuEnvironment.getNativeConfig().getValue();
            String[] tokens = nativeConfig.split("\n");
        	for (String token : tokens) {
				String[] elements = token.trim().split("\\s+");
				List<String> subElements = new ArrayList<>(List.of(elements));
				config.getNativeConfig().add(subElements);
			}
        }
        this.isEmuconInitDisabled = true;
		emuRunner.setCommand("/init");
	}

	@Override
	public boolean addDrive(Drive drive) {
		if (drive == null || (drive.getData() == null)) {
			LOG.warning("Drive doesn't contain an image, attach canceled.");
			return false;
		}

		EmulatorRuntimeConfiguration.HardwareComponent component = new EmulatorRuntimeConfiguration.HardwareComponent();
		component.setComponent(this.machine + "-" + drive.getType().toString().toLowerCase(Locale.ROOT));
		component.setIndex(componentIndex++);
		Path imagePath = null;
		try {
			imagePath = Paths.get(this.lookupResource(drive.getData(), this.getImageFormatForDriveType(drive.getType())));
		} catch (Exception e) {
			LOG.warning("Drive doesn't reference a valid binding, attach canceled.");
			e.printStackTrace();
			return false;
		}
		component.setPath(imagePath.toAbsolutePath().toString());
		config.getHardwareComponents().add(component);
		return true;
	}

	@Override
	protected boolean connectDrive(Drive drive, boolean attach) throws BWFLAException {
		return false;
	}

	protected boolean addNic(Nic nic) {
		if (nic == null) {
			LOG.warning("NIC is null, attach canceled.");
			return false;
		}

		EmulatorRuntimeConfiguration.HardwareComponent component = new EmulatorRuntimeConfiguration.HardwareComponent();
		component.setComponent(machine + "-" + "nic");

		final String nicPath = this.getNetworksDir().resolve("nic_" + nic.getHwaddress()).toString();
		component.setPath(nicPath);
		config.getHardwareComponents().add(component);
		return true;
	}

	private String fmtDate(long epoch)
	{
		Date d = new Date(epoch);
		DateFormat format = new SimpleDateFormat("YYYY-MM-dd'T'hh:mm:ss"); // 2006-06-17T16:01:21
		String formatted = format.format(d);
		return formatted;
	}

	protected void setEmulatorTime(long epoch)
	{
		// config.setStartTime(fmtDate(epoch));
	}

	@Override
	public void start() throws BWFLAException {
		emuRunner.addEnvVariable("EAAS_CONFIG", config.toString());
		super.start();
	}

	@Override
	public String stop() throws BWFLAException {
		return super.stop();
	}

	@Override
	public void destroy(){
		super.destroy();
	}
}
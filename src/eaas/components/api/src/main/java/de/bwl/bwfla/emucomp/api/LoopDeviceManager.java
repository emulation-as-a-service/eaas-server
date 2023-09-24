package de.bwl.bwfla.emucomp.api;


import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.ProcessRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class LoopDeviceManager {
	protected static final Logger log = Logger.getLogger("EmulatorUtils");

	private static HashMap<String, Boolean> loopDevicesMap = new HashMap<String, Boolean>() {
		{
			for (int i = 0; i <= 7; i++) {
				put("/dev/loop" + i, Boolean.TRUE);
			}
		}
	};


	public synchronized static String getLoopDevice() throws BWFLAException {
		String loopNumber = (String) getKeyFromValue(loopDevicesMap, Boolean.TRUE);
		if(loopNumber == null){
			throw  new BWFLAException("out of loopdevices");
		}
		loopDevicesMap.put(loopNumber, Boolean.FALSE);
		return loopNumber;
	}

	public synchronized static void detachLoop(String loopDev) throws BWFLAException, IOException {
		loopDevicesMap.put(loopDev, Boolean.TRUE);
		detach(loopDev);
	}

	private static void detach(String dev) throws BWFLAException, IOException {
		ProcessRunner process = new ProcessRunner();
		process.setLogger(log);
		process.setCommand("losetup");
		process.addArguments("-d", dev);
		process.redirectStdErrToStdOut(false);
		if (!process.execute())
			throw new BWFLAException("Detaching loop-device failed!");
	}

	public static Object getKeyFromValue(Map hm, Object value) {
		for (Object o : hm.keySet()) {
			if (hm.get(o).equals(value)) {
				return o;
			}
		}
		return null;
	}
	}

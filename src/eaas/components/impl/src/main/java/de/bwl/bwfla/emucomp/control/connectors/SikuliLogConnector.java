package de.bwl.bwfla.emucomp.control.connectors;

import java.net.URI;
import java.nio.file.Path;

public class SikuliLogConnector extends LogConnector {
	public final static String PROTOCOL = "sikulilog";
	public SikuliLogConnector(Path logPath)
	{
		super(logPath);
	}

	@Override
	public URI getControlPath(URI componentResource) {
		return componentResource.resolve(PROTOCOL);
	}

	@Override
	public String getProtocol() {
		return PROTOCOL;
	}
}

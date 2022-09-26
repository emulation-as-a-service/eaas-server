package de.bwl.bwfla.emucomp.control.connectors;

import java.net.URI;


public class SikuliConnector implements IConnector
{
	public final static String PROTOCOL = "sikuli";


	@Override
	public URI getControlPath(URI componentResource)
	{
		return componentResource.resolve(SikuliConnector.PROTOCOL);
	}

	@Override
	public String getProtocol()
	{
		return SikuliConnector.PROTOCOL;
	}
}

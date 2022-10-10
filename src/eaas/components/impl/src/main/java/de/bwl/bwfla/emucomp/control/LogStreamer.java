package de.bwl.bwfla.emucomp.control;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.emucomp.NodeManager;
import de.bwl.bwfla.emucomp.components.AbstractEaasComponent;
import de.bwl.bwfla.emucomp.components.Tail;
import de.bwl.bwfla.emucomp.components.emulators.EmulatorBean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;


@ApplicationScoped
@Path("/components")
public class LogStreamer
{

	@Inject
	private NodeManager nodemgr = null;

	@GET
	@Path("/{componentId}/stdout")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getStdout(@PathParam("componentId") String componentId)
	{
		try {
			final AbstractEaasComponent component = nodemgr.getComponentById(componentId);
			if (!(component instanceof EmulatorBean))
				return Response.status(Response.Status.NOT_FOUND).build();

			EmulatorBean bean = (EmulatorBean) component;
			final Tail stdout = bean.getEmulatorStdOut();
			return streamResponse(stdout);
		}
		catch (BWFLAException error) {
			throw new NotFoundException("Component not found: " + componentId);
		}
	}


	@GET
	@Path("/{componentId}/stderr")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getStderr(@PathParam("componentId") String componentId)
	{
		try {
			final AbstractEaasComponent component = nodemgr.getComponentById(componentId);
			if (!(component instanceof EmulatorBean))
				return Response.status(Response.Status.NOT_FOUND).build();

			EmulatorBean bean = (EmulatorBean) component;
			Tail stderr = bean.getEmulatorStdErr();
			return streamResponse(stderr);
		}
		catch (BWFLAException error) {
			throw new NotFoundException("Component not found: " + componentId);
		}
	}

	@GET
	@Path("/{componentId}/sikulilog")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getSikuliLog(@PathParam("componentId") String componentId)
	{
		try {
			final AbstractEaasComponent component = nodemgr.getComponentById(componentId);
			if (!(component instanceof EmulatorBean))
				return Response.status(Response.Status.NOT_FOUND).build();

			EmulatorBean bean = (EmulatorBean) component;
			Tail sikuliLogs = bean.getSikuliLogs(componentId);
			return streamResponse(sikuliLogs);
		}
		catch (BWFLAException error) {
			throw new NotFoundException("Component not found: " + componentId);
		}
	}

	private Response streamResponse(Tail tail)
	{
		if (tail == null)
			return Response.status(Response.Status.NOT_FOUND).build();

		StreamingOutput stream = out -> {
			final var buffer = new byte[8 * 1024];
			final var source = tail.getStream();
			try {
				while (source.read(buffer, 0, 1) > 0) {
					final int explen = Math.min(source.available(), buffer.length - 1);
					final int curlen = 1 + source.read(buffer, 1, explen);
					out.write(buffer, 0, curlen);
					System.out.println(" --- Curlen is " + curlen);
					for (int i = 0; i < curlen; i++) {
						System.out.println(" Writing: " + buffer[i]);
					}

					out.flush();
				}
			}
			finally {
				System.out.println("Closing!");
				out.close();
				tail.cleanup();
			}
		};
		return Response.status(Response.Status.OK)
				.entity(stream).build();
	}
}

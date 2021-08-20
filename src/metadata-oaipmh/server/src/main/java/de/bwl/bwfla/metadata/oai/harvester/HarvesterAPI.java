/*
 * This file is part of the Emulation-as-a-Service framework.
 *
 * The Emulation-as-a-Service framework is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * The Emulation-as-a-Service framework is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Emulation-as-a-Software framework.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package de.bwl.bwfla.metadata.oai.harvester;


import com.webcohesion.enunciate.metadata.rs.TypeHint;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import de.bwl.bwfla.common.services.rest.ResponseUtils;
import de.bwl.bwfla.metadata.oai.harvester.config.BackendConfig;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
@Path("/harvesters")
public class HarvesterAPI
{
	private final Logger log = Logger.getLogger(HarvesterAPI.class.getSimpleName());

	@Resource(lookup = "java:jboss/ee/concurrency/executor/io")
	private Executor executor = null;

	@Inject
	private HarvesterRegistry harvesters = null;


	// ========== Admin API ==============================

	/** List all registered harvesters */
	@GET
	@Secured(roles={Role.ADMIN})
	@Produces(MediaType.APPLICATION_JSON)
	@TypeHint(String[].class)
	public Response listHarvesters()
	{
		final Collection<String> ids = harvesters.list();
		return Response.ok(ids, MediaType.APPLICATION_JSON_TYPE)
				.build();
	}

	/** Register a new harvester */
	@POST
	@Secured(roles={Role.ADMIN})
	@Consumes(MediaType.APPLICATION_JSON)
	@TypeHint(TypeHint.NO_CONTENT.class)
	public Response register(BackendConfig config)
	{
		try {
			harvesters.add(config);
		}
		catch (Exception error) {
			throw new BadRequestException(error);
		}

		return Response.ok()
				.build();
	}

	/** Delete an existing harvester */
	@DELETE
	@Secured(roles={Role.ADMIN})
	@Path("/{name}")
	@TypeHint(TypeHint.NO_CONTENT.class)
	public Response unregister(@PathParam("name") String name)
	{
		if (!harvesters.remove(name))
			throw new NotFoundException("Harvester not found: " + name);

		return Response.ok()
				.build();
	}

	/** Look up harvester's config */
	@GET
	@Path("/{name}")
	@Secured(roles={Role.ADMIN})
	@Produces(MediaType.APPLICATION_JSON)
	@TypeHint(BackendConfig.class)
	public Response fetch(@PathParam("name") String name)
	{
		final HarvesterBackend harvester = this.lookup(name);
		return Response.ok(harvester.getConfig(), MediaType.APPLICATION_JSON_TYPE)
				.build();
	}

	/** Update harvester's config */
	@PUT
	@Path("/{name}")
	@Secured(roles={Role.ADMIN})
	@Consumes(MediaType.APPLICATION_JSON)
	@TypeHint(TypeHint.NO_CONTENT.class)
	public Response update(@PathParam("name") String name, BackendConfig config)
	{
		if (!name.contentEquals(config.getName()))
			throw new BadRequestException("Invalid harvester name!");

		return this.register(config);
	}

	/** Execute named harvester */
	@POST
	@Secured(roles={Role.ADMIN})
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@TypeHint(HarvestingResult.class)
	public CompletionStage<Response> harvest(@PathParam("name") String name, @Context HttpServletRequest request)
	{
		final HarvesterBackend harvester = this.lookup(name);
		final Instant fromts = HarvesterAPI.getFromTimestamp(request);
		final Instant untilts = HarvesterAPI.getUntilTimestamp(request);

		final Supplier<Response> responder = () -> {
			try {
				final HarvestingResult result = harvester.execute(fromts, untilts);
				return Response.ok(result, MediaType.APPLICATION_JSON_TYPE)
						.build();
			}
			catch (Exception error) {
				final String message = "Harvesting failed!";
				log.log(Level.WARNING, message, error);
				return ResponseUtils.newInternalError(message, error);
			}
		};

		return CompletableFuture.supplyAsync(responder, executor);
	}

	/** Get harvester's status */
	@GET
	@Secured(roles={Role.ADMIN})
	@Path("/{name}/status")
	@Produces(MediaType.APPLICATION_JSON)
	@TypeHint(HarvesterStatus.class)
	public Response status(@PathParam("name") String name)
	{
		final HarvesterBackend harvester = this.lookup(name);
		return Response.ok(harvester.getStatus(), MediaType.APPLICATION_JSON_TYPE)
				.build();
	}


	// ========== Internal Helpers ==============================

	private static Instant getTimestampParam(HttpServletRequest request, String name, Instant defvalue)
	{
		final String value = request.getParameter(name);
		if (value == null || value.isEmpty())
			return defvalue;

		return Instant.parse(value);
	}

	private static Instant getFromTimestamp(HttpServletRequest request)
	{
		return HarvesterAPI.getTimestampParam(request, "from", null);
	}

	private static Instant getUntilTimestamp(HttpServletRequest request)
	{
		return HarvesterAPI.getTimestampParam(request, "until", null);
	}

	private HarvesterBackend lookup(String name) throws NotFoundException
	{
		final HarvesterBackend harvester = harvesters.lookup(name);
		if (harvester == null)
			throw new NotFoundException("Harvester not found: " + name);

		return harvester;
	}
}

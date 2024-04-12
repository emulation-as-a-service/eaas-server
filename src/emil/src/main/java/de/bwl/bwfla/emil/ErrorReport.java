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

package de.bwl.bwfla.emil;

import de.bwl.bwfla.common.utils.EaasBuildInfo;
import de.bwl.bwfla.common.utils.ProcessRunner;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
@Path("/error-report")
public class ErrorReport
{
	protected static final Logger LOG = Logger.getLogger("eaas/error-report");


	/* ============================= API =============================== */

	@GET
	@Secured(roles = {Role.PUBLIC})
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getErrorReport()
	{
		java.nio.file.Path outpath = null;
		try {
			LOG.info("Error-report requested on eaas-server version " + EaasBuildInfo.getVersion());
			outpath = Files.createTempFile("eaas-er-", ".gpg");

			final ProcessRunner runner = new ProcessRunner();
			runner.setCommand("/libexec/generate-error-report");
			runner.addArgument(outpath.toString());
			runner.setLogger(LOG);
			if (!runner.execute())
				throw new InternalServerErrorException("Generating error-report failed!");

			final LocalDateTime timestamp = LocalDateTime.now();
			final String name = "eaas-error-report-" + timestamp.get(ChronoField.DAY_OF_YEAR)
					 + "-" + timestamp.get(ChronoField.MINUTE_OF_DAY) + ".gpg";

			// Set response headers...
			return Response.status(Response.Status.OK)
					.header("Access-Control-Allow-Origin", "*")
					.header("Content-Disposition", "attachment; filename=" + name)
					.entity(Files.readAllBytes(outpath))
					.build();
		}
		catch (IOException exception) {
			return Response.status(Response.Status.NO_CONTENT)
					.build();
		}
		finally {
			if (outpath != null) {
				try {
					Files.deleteIfExists(outpath);
				}
				catch (IOException error) {
					LOG.log(Level.WARNING, "Deleting error-report failed!", error);
				}
			}
		}
	}
}

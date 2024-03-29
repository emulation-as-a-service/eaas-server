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

import com.openslx.eaas.common.concurrent.ParallelProcessors;
import com.openslx.eaas.common.databind.DataUtils;
import com.openslx.eaas.common.event.MigrationFinishedEvent;
import com.openslx.eaas.migration.IMigratable;
import com.openslx.eaas.migration.MigrationRegistry;
import com.openslx.eaas.migration.MigrationUtils;
import com.openslx.eaas.migration.config.MigrationConfig;
import de.bwl.bwfla.common.datatypes.DigitalObjectMetadata;
import de.bwl.bwfla.common.datatypes.SoftwareDescription;
import de.bwl.bwfla.common.datatypes.SoftwarePackage;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.emil.datatypes.EaasiSoftwareObject;
import de.bwl.bwfla.emil.datatypes.EmilSoftwareObject;
import de.bwl.bwfla.emil.datatypes.SoftwareCollection;
import de.bwl.bwfla.common.services.security.AuthenticatedUser;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import de.bwl.bwfla.common.services.security.UserContext;
import de.bwl.bwfla.emil.utils.ImportCounts;
import de.bwl.bwfla.imageproposer.client.ImageProposer;
import de.bwl.bwfla.objectarchive.util.ObjectArchiveHelper;
import de.bwl.bwfla.softwarearchive.util.SoftwareArchiveHelper;
import gov.loc.mets.FileType;
import gov.loc.mets.Mets;
import org.apache.commons.lang3.StringUtils;
import org.apache.tamaya.inject.api.Config;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;


@ApplicationScoped
@Path("/software-repository")
public class SoftwareRepository extends EmilRest
		implements IMigratable
{
    private SoftwareArchiveHelper swHelper;
    private ObjectArchiveHelper objHelper;

    @Inject
    @Config(value = "ws.softwarearchive")
    private String softwareArchive = null;

    @Inject
    @Config(value = "ws.objectarchive")
    private String objectArchive = null;

	@Inject
	@AuthenticatedUser
	private UserContext userctx = null;

	private ImageProposer imageProposer;

	@Inject
	@Config(value = "emil.imageproposerservice")
	private String imageProposerService = null;

	@Resource(lookup = "java:jboss/ee/concurrency/executor/io")
	private ExecutorService executor;

    @PostConstruct
    private void initialize()
	{
		try {
			swHelper = new SoftwareArchiveHelper(softwareArchive);
			objHelper = new ObjectArchiveHelper(objectArchive);
			imageProposer = new ImageProposer(imageProposerService + "/imageproposer");
		}
		catch (IllegalArgumentException error) {
			LOG.log(Level.WARNING, "Initializing software-repository failed!", error);
		}
	}

	private void movePublishedSoftwareToZeroConfArchive(MigrationConfig mc) throws Exception
	{
		final var counter = ImportCounts.counter();

		LOG.info("Checking if archive is properly set for public software...");
		final Stream<SoftwarePackage> packages = swHelper.getSoftwarePackages();

		packages.filter(SoftwarePackage::isPublic)
				.filter(sw -> !sw.getArchive().equals("zero conf"))
				.forEach(sw -> {
					LOG.info("Found software that needs to be migrated: " + sw.getId());
					var emilSoftwareObject = new EmilSoftwareObject();
					emilSoftwareObject.setIsPublic(true);
					emilSoftwareObject.setArchiveId("zero conf");
					emilSoftwareObject.setId(sw.getId());
					emilSoftwareObject.setLabel(sw.getName());
					emilSoftwareObject.setAllowedInstances(sw.getNumSeats());
					emilSoftwareObject.setObjectId(sw.getObjectId());
					emilSoftwareObject.setQID(sw.getQID());
					emilSoftwareObject.setLicenseInformation(sw.getLicence());
					emilSoftwareObject.setIsOperatingSystem(sw.getIsOperatingSystem());
					emilSoftwareObject.setNativeFMTs(sw.getSupportedFileFormats());

					try (final var response = this.packages().create(emilSoftwareObject)) {
						if (response.getStatus() == Status.OK.getStatusCode()) {
							counter.increment(ImportCounts.IMPORTED);
						}
						else {
							counter.increment(ImportCounts.FAILED);

							var message = "Migrating software '" + sw.getId() + "' failed!";
							if (response.hasEntity()) {
								message += " Cause: " + response.getEntity().toString();
							}

							LOG.warning(message);
						}
					}
				});

		final var numMigrated = counter.get(ImportCounts.IMPORTED);
		final var numFailed = counter.get(ImportCounts.FAILED);
		final var maxFailureRate = MigrationUtils.getFailureRate(mc);
		LOG.info("Migrated " + numMigrated + " software-package(s), failed " + numFailed);
		if (!MigrationUtils.acceptable(numMigrated + numFailed, numFailed, maxFailureRate))
			throw new BWFLAException("Moving published software to zero-conf archive failed!");
	}

	public SoftwareCollection getSoftwareCollection()
	{
		return new SoftwareCollection(objHelper, swHelper);
	}

	public void importSoftware(EaasiSoftwareObject swo) throws BWFLAException
	{
		SoftwarePackage softwarePackage = swo.getSoftwarePackage();
		softwarePackage.setArchive("Remote Objects");
		softwarePackage.setPublic(true);
		try {
			SoftwarePackage software = swHelper.getSoftwarePackageById(softwarePackage.getObjectId());
//			if(software != null) {
//				LOG.warning("software with id " + softwarePackage.getObjectId() + " present. skipping...");
//				return;
//			}
		}
		catch (BWFLAException error) {
			LOG.log(Level.WARNING, "Importing software-package failed!", error);
			return;
		}

		objHelper.importFromMetadata("Remote Objects", swo.getMetsData());
		swHelper.addSoftwarePackage(softwarePackage);
	}


	// ========== Public API ==============================

	@Path("/actions")
	public Actions actions()
	{
		return new Actions();
	}

	@Path("packages")
	public SoftwarePackages packages()
	{
		return new SoftwarePackages();
	}

	@Path("descriptions")
	public SoftwareDescriptions descriptions()
	{
		return new SoftwareDescriptions();
	}


	// ========== Subresources ==============================

	public class Actions
	{
		@POST
		@Path("/sync")
		@Secured(roles = {Role.RESTRICTED})
		@Produces(MediaType.APPLICATION_JSON)
		public synchronized Response sync()
		{
			try {
				final var oldEntries = new HashMap<String, SoftwareDescription>();
				swHelper.getSoftwareDescriptions()
						.forEach((desc) -> oldEntries.put(desc.getSoftwareId(), desc));

				swHelper.sync();

				final var newEntries = new HashMap<String, SoftwareDescription>();
				swHelper.getSoftwareDescriptions()
						.forEach((desc) -> {
							if (oldEntries.remove(desc.getSoftwareId()) == null)
								newEntries.put(desc.getSoftwareId(), desc);
						});

				final var swRepo = SoftwareRepository.this;
				swRepo.markObjectsAsSoftware(newEntries.values().stream(), true);
				swRepo.markObjectsAsSoftware(oldEntries.values().stream(), false);
			}
			catch (Exception error) {
				return Emil.internalErrorResponse(error);
			}

			return Emil.successMessageResponse("Software packages were synced successfully!");
		}
	}

	public class SoftwarePackages
	{
		/**
		 * Looks up and returns all software packages.
		 * @return A JSON response containing a list of software packages or an error message.
		 */
		@GET
		@Secured(roles = {Role.PUBLIC})
		@Produces(MediaType.APPLICATION_JSON)
		public Response list()
		{
			LOG.info("Listing all software-packages...");

			try {
				final Stream<SoftwarePackage> packages = swHelper.getSoftwarePackages();
				if (packages == null)
					throw new NotFoundException();

				// Construct response (in streaming-mode)
				final StreamingOutput output = (ostream) -> {
					final var jsonfactory = DataUtils.json()
							.mapper()
							.getFactory();

					try (com.fasterxml.jackson.core.JsonGenerator json = jsonfactory.createGenerator(ostream)) {
						final var writer = DataUtils.json()
								.writer();

						json.writeStartObject();
						json.writeStringField("status", "0");
						json.writeArrayFieldStart("packages");
						packages.forEach((pkg) -> {
							try {
								writer.writeValue(json, SoftwareRepository.toEmilSoftwareObject(pkg));
							}
							catch (Exception error) {
								LOG.log(Level.WARNING, "Serializing software-package failed!", error);
								throw new RuntimeException(error);
							}
						});
						json.writeEndArray();
						json.writeEndObject();
						json.flush();
					}
					finally {
						packages.close();
					}
				};

				return SoftwareRepository.createResponse(Status.OK, output);
			}
			catch (Throwable error) {
				LOG.log(Level.WARNING, "Listing software-packages failed!", error);
				return Emil.internalErrorResponse(error);
			}
		}

		@GET
		@Path("/{softwareId}")
		@Secured(roles = {Role.PUBLIC})
		@Produces(MediaType.APPLICATION_JSON)
		public Response get(@PathParam("softwareId") String softwareId)
		{
			LOG.info("Looking up software-package '" + softwareId + "'...");

			try {
				SoftwarePackage software = swHelper.getSoftwarePackageById(softwareId);
				if (software == null)
					throw new NotFoundException();

				EmilSoftwareObject swo = SoftwareRepository.toEmilSoftwareObject(software);
				return SoftwareRepository.createResponse(Status.OK, swo);
			}
			catch(Throwable error) {
				LOG.log(Level.WARNING, "Looking up software-package failed!", error);
				return Emil.internalErrorResponse(error);
			}
		}

		@DELETE
		@Path("/{softwareId}")
		@Secured(roles = {Role.RESTRICTED})
		@Produces(MediaType.APPLICATION_JSON)
		public Response deleteSoftware(@PathParam("softwareId") String softwareId)
		{
			try{
				swHelper.deleteSoftware(softwareId);
				return Response.status(Status.OK)
				.build();
			}
			catch (BWFLAException e)
			{
				return Emil.internalErrorResponse(e);
			}
		}

		/**
		 * Save or update software object meta data.
		 * expects a JSON object:
		 * <pre>
		 * {"objectId":"id","licenseInformation":"","allowedInstances":1,"nativeFMTs":[],"importFMTs":[],"exportFMTs":[]}
		 * </pre>
		 * @param swo EmilSoftwareObject as JSON string
		 * @return JSON response (error) message
		 */
		@POST
		@Secured(roles = {Role.RESTRICTED})
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces(MediaType.APPLICATION_JSON)
		public Response create(EmilSoftwareObject swo)
		{
			LOG.info("Creating/Changing software-package...");

			try {

				var software = new SoftwarePackage();

				String archiveName = swo.getArchiveId();
				LOG.info("Got archive name from request: " + archiveName);

				if (archiveName == null) {
					if (userctx != null && userctx.getUserId() != null) {
						LOG.info("Using user context: " + userctx.getUserId());
						archiveName = "user-" + userctx.getUserId(); //TODO can "user-" be used here generally? Potentially migration needed
						LOG.info("Setting archive name to: " + archiveName);
					}
				}

				//object needs to be reimported to 'zero conf' archive if:
				//1. archive is null
				//2. software is from a user object and needs to be published
				// Software that is from a user object but is private, does NOT need to be reimported!

				if (archiveName == null || (archiveName.startsWith("user") && swo.getIsPublic())) {

					LOG.info("Object needs to be reimported to the default archive, so that it is able to be published as software...");
					DigitalObjectMetadata md = objHelper.getObjectMetadataUnresolved(archiveName, swo.getObjectId());

					if (md == null) {
						LOG.severe("Getting object metadata failed!");
						return Emil.errorMessageResponse("Failed to access object!");
					}

					// modify metadata to use for import
					// necessary as we need presigned URLS for the actual files and not packed-iso
					Mets m = Mets.fromValue(md.getMetsData(), Mets.class);
					var fs = m.getFileSec();
					var fgrp = fs.getFileGrp();
					for (var f : fgrp){
						var files = f.getFile();
						for (FileType actFile : files){

							//to properly reimport an object to zero conf we need to
							//1. get the internal representation of the objects resources (presigned minio URLs)
							//2. set the id to the original file name which can be retrieved through the original href of the file
							//a file with id: 'FID-12345...' and href: 'file/myfile.txt'
							//will then be imported with id: 'myfile.txt' and href: 'http://minio:9000/object-archive...'

							var resolved = objHelper.resolveObjectResourceInternally(archiveName, swo.getObjectId(), actFile.getID(), "GET");
							FileType.FLocat location = actFile.getFLocat().get(0);
							var filename = StringUtils.substringAfterLast(location.getHref(), "/");
							location.setTitle(filename);
							location.setHref(resolved);
						}
					}

					objHelper.importFromMetadata("zero conf", m.toString());

					if(archiveName.startsWith("user")){
						objHelper.delete(archiveName, swo.getObjectId());
					}
					archiveName = "zero conf";
				}


				if(software.isPublic() && !swo.getIsPublic()){
					LOG.warning("Trying to set public software to back to private. This is prohibited! SW will stay public.");
				}
				else{
					software.setPublic(swo.getIsPublic());
					LOG.info("Setting software-package's visibility to: " + ((software.isPublic()) ? "public" : "private"));
				}


				software.setObjectId(swo.getObjectId());
				software.setArchive(archiveName);
				software.setName(swo.getLabel());
				software.setDeleted(false);
				software.setNumSeats(swo.getAllowedInstances());
				software.setLicence(swo.getLicenseInformation());
				software.setIsOperatingSystem(swo.getIsOperatingSystem());
				software.setSupportedFileFormats(swo.getNativeFMTs());
				if (swo.getImportFMTs() != null)
					software.getSupportedFileFormats().addAll(swo.getImportFMTs());

				if (swo.getImportFMTs() != null)
					software.getSupportedFileFormats().addAll(swo.getExportFMTs());

				software.setQID(swo.getQID());

				swHelper.addSoftwarePackage(software);
				objHelper.markAsSoftware(software.getArchive(), software.getObjectId(), true);
				imageProposer.refreshIndex();
			}
			catch(Throwable error) {
				LOG.log(Level.WARNING, "Creating new software-package failed!", error);
				return SoftwareRepository.internalErrorResponse(error);
			}

			String message = "Successfully imported software object " + swo.getObjectId();
			return SoftwareRepository.successMessageResponse(message);
		}
	}


	public class SoftwareDescriptions
	{
		/**
		 * Looks up and returns the descriptions for all software packages.
		 * A JSON response will be returned, containing:
		 * <pre>
		 * {
		 *      "status": "0",
		 *      "descriptions": [
		 *          { "id": &ltSoftwarePackage's ID&gt, "label": "Short description" },
		 *          ...
		 *      ]
		 * }
		 * </pre>
		 *
		 * When an internal error occurs, a JSON response containing
		 * the corresponding message will be returned:
		 * <pre>
		 * {
		 *      "status": "2",
		 *      "message": "Error message."
		 * }
		 * </pre>
		 *
		 * @return A JSON response containing a list of descriptions
		 *         for all software packages or an error message.
		 */
		@GET
		@Secured(roles = {Role.PUBLIC})
		@Produces(MediaType.APPLICATION_JSON)
		public Response list()
		{
			LOG.info("Listing all software-package descriptions...");

			String loggedInUserArchiveName;

			if (userctx != null && userctx.getUserId() != null) {
				loggedInUserArchiveName = "user-" + userctx.getUserId();
				LOG.info("Listing software that is accessible to user: " + userctx.getUserId());
			}
			else {
				loggedInUserArchiveName = null;
			}

			try {
				final Stream<SoftwareDescription> descriptions = swHelper.getSoftwareDescriptions();
				if (descriptions == null) {
					// TODO: throw NotFoundException here!
					return SoftwareRepository.errorMessageResponse("Software archive could not be read!");
				}

				// Construct response (in streaming-mode)
				final StreamingOutput output = (ostream) -> {
					try (final JsonGenerator json = Json.createGenerator(ostream)) {
						json.writeStartObject();
						json.write("status", "0");
						json.writeStartArray("descriptions");
						descriptions.forEach((desc) -> {

							var archiveId = (desc.getArchiveId() != null) ? desc.getArchiveId() : "default";

							if (archiveId.startsWith("user")) {
								//this is reached if the object archive for the software is a (private) user-archive

								if(loggedInUserArchiveName == null){
									//if no user context is provided, only show default/public software
									return;
								}

								else if(!archiveId.equals(loggedInUserArchiveName)){
									//only show private software where the corresponding archive belongs to the user
									return;
								}
							}

							json.writeStartObject();
							json.write("id", desc.getSoftwareId());
							json.write("label", desc.getLabel());
							json.write("isPublic", desc.isPublic());
							json.write("archiveId", archiveId);
							json.write("isOperatingSystem", desc.getIsOperatingSystem());
							json.writeEnd();
						});

						json.writeEnd();
						json.writeEnd();
						json.flush();
					}
					finally {
						descriptions.close();
					}
				};

				return SoftwareRepository.createResponse(Status.OK, output);
			}
			catch (Throwable error) {
				LOG.log(Level.WARNING, "Listing software-package descriptions failed!", error);
				return Emil.internalErrorResponse(error);
			}
		}

		/**
		 * Looks up and returns the description for a specified software package.
		 * When the software package is found, a JSON response will be returned, containing:
		 * <pre>
		 * {
		 *      "status": "0",
		 *      "id": &ltSoftwarePackage's ID&gt,
		 *      "label": "Short description"
		 * }
		 * </pre>
		 *
		 * When an internal error occurs, a JSON response containing
		 * the corresponding message will be returned:
		 * <pre>
		 * {
		 *      "status": "1",
		 *      "message": "Error message."
		 * }
		 * </pre>
		 *
		 * @param softwareId The software package's ID to look up.
		 * @return A JSON response containing software package's description when found,
		 *         else an error message.
		 */
		@GET
		@Path("/{softwareId}")
		@Secured(roles = {Role.PUBLIC})
		@Produces(MediaType.APPLICATION_JSON)
		public Response get(@PathParam("softwareId") String softwareId)
		{
			LOG.info("Looking up description of software-package '" + softwareId + "'...");

			try {
				SoftwareDescription desc = swHelper.getSoftwareDescriptionById(softwareId);
				if (desc == null) {
					// TODO: throw NotFoundException here!
					return Emil.errorMessageResponse("Software with ID '" + softwareId + "' was not found!");
				}

				// Construct response
				final JsonObject json = Json.createObjectBuilder()
						.add("status", "0")
						.add("id", desc.getSoftwareId())
						.add("label", desc.getLabel())
						.add("isPublic", desc.isPublic())
						.add("archiveId", (desc.getArchiveId() != null) ? desc.getArchiveId() : "default")
						.add("isOperatingSystem", desc.getIsOperatingSystem())
						.build();


				return SoftwareRepository.createResponse(Status.OK, json.toString());
			}
			catch (Throwable error) {
				LOG.log(Level.WARNING, "Looking up description of software-package failed!", error);
				return Emil.internalErrorResponse(error);
			}
		}
	}

	private static EmilSoftwareObject toEmilSoftwareObject(SoftwarePackage software)
	{
		EmilSoftwareObject swo = new EmilSoftwareObject();
		swo.setId(software.getObjectId());
		swo.setLabel(software.getName());
		swo.setIsPublic(software.isPublic());
		swo.setObjectId(software.getObjectId());
		swo.setArchiveId(software.getArchive());
		swo.setAllowedInstances(software.getNumSeats());
		List<String> fmts = software.getSupportedFileFormats();
		if (fmts == null)
			fmts = new ArrayList<String>();

		swo.setNativeFMTs(fmts);
		swo.setExportFMTs(new ArrayList<String>());
		swo.setImportFMTs(new ArrayList<String>());
		swo.setLicenseInformation(software.getLicence());
		swo.setIsOperatingSystem(software.getIsOperatingSystem());
		swo.setQID(software.getQID());
		return swo;
	}

	private void markObjectsAsSoftware() throws Exception
	{
		try (final var descriptions = swHelper.getSoftwareDescriptions()) {
			this.markObjectsAsSoftware(descriptions, true);
		}
	}

	private void markObjectsAsSoftware(Stream<SoftwareDescription> entries, boolean isSoftware)
			throws Exception
	{
		final var counter = ImportCounts.counter();
		final var whatmsg = (isSoftware) ? "Marking" : "Unmarking";

		final Consumer<SoftwareDescription> marker = (desc) -> {
			try {
				objHelper.markAsSoftware(desc.getArchiveId(), desc.getSoftwareId(), isSoftware);
				counter.increment(ImportCounts.IMPORTED);
			}
			catch (Exception error) {
				LOG.log(Level.WARNING, whatmsg + " object '" + desc.getSoftwareId() + "' as software failed!", error);
				counter.increment(ImportCounts.FAILED);
			}
		};

		LOG.info(whatmsg + " objects as software...");
		ParallelProcessors.consumer(marker)
				.consume(entries, executor);

		final var numMarked = counter.get(ImportCounts.IMPORTED);
		final var numFailed = counter.get(ImportCounts.FAILED);
		LOG.info(((isSoftware) ? "Marked " : "Unmarked ") + numMarked + " object(s) as software, failed " + numFailed);
	}

	@Override
	public void register(@Observes MigrationRegistry migrations) throws Exception
	{
		migrations.register("move-published-software-to-zeroconf-archive", this::movePublishedSoftwareToZeroConfArchive);
	}

	public void handle(@Observes MigrationFinishedEvent event) throws Exception
	{
		this.markObjectsAsSoftware();
	}
}

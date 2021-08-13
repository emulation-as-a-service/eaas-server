package de.bwl.bwfla.objectarchive;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jws.WebService;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.ws.soap.MTOM;

import de.bwl.bwfla.common.datatypes.GenericId;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.jaxb.JaxbCollectionWriter;
import de.bwl.bwfla.common.utils.jaxb.JaxbNames;
import de.bwl.bwfla.emucomp.api.FileCollection;
import de.bwl.bwfla.objectarchive.api.SeatDescription;
import de.bwl.bwfla.objectarchive.conf.ObjectArchiveSingleton;
import de.bwl.bwfla.objectarchive.datatypes.DigitalObjectArchive;

import de.bwl.bwfla.common.datatypes.DigitalObjectMetadata;
import de.bwl.bwfla.common.taskmanager.TaskState;
import de.bwl.bwfla.objectarchive.impl.DigitalObjectUserArchive;
import org.apache.tamaya.inject.ConfigurationInjection;
import org.apache.tamaya.inject.api.Config;

import static de.bwl.bwfla.objectarchive.conf.ObjectArchiveSingleton.tmpArchiveName;

@Stateless
@MTOM
@WebService(targetNamespace = "http://bwfla.bwl.de/api/objectarchive")
public class ObjectArchiveFacadeWS 
{
	protected static final Logger LOG = Logger.getLogger(ObjectArchiveFacadeWS.class.getName());

	@Resource(lookup = "java:jboss/ee/concurrency/executor/io")
	private Executor executor = null;

	@Inject
	@Config(value="objectarchive.default_archive")
	private String defaultArchive;

	@Inject
	@Config(value="objectarchive.user_archive_prefix")
	private String USERARCHIVEPRIFIX;

	@Inject
	@Config(value="objectarchive.user_archive_enabled")
	private boolean userArchiveEnabled;

	@Inject
	private SeatManager seatmgr;
	
	@PostConstruct
	private void initialize()
	{
		ConfigurationInjection.getConfigurationInjector().configure(this);
	}
	
	/**
	 * @return list of object IDs
	 */

	private DigitalObjectArchive getArchive(String archive) throws BWFLAException
	{
		if(!ObjectArchiveSingleton.confValid)
		{
			final String message = "Object archive '" + archive + "' not configured!";
			LOG.severe(message);
			throw new BWFLAException(message);
		}

		if(archive == null)
			archive = defaultArchive;

		DigitalObjectArchive a = ObjectArchiveSingleton.archiveMap.get(archive);
		if(a != null)
			return a;

		if(userArchiveEnabled && !archive.startsWith(USERARCHIVEPRIFIX))
		{
			LOG.warning("trying harder: " + archive);
			archive = USERARCHIVEPRIFIX + archive;
			return getArchive(archive);
		}
		throw new BWFLAException("Object archive " + archive + " not found");
	}

	public @XmlMimeType("application/xml") DataHandler getObjectIds(String archive) throws BWFLAException {
		DigitalObjectArchive a = getArchive(archive);
//		return a.getObjectList();
		final Stream<String> ids = a.getObjectIds();
		return this.toDataHandler(ids.map(GenericId::new), GenericId.class, JaxbNames.DIGITAL_OBJECT_IDS);
	}

	/**
	 * @param id object-id
	 * @return object reference as PID / PURL
	 */
	public String getObjectReference(String archive, String id) throws BWFLAException {
		DigitalObjectArchive a = getArchive(archive);
		if(id == null)
		{
			throw new BWFLAException("request for object with invalid objectId " + id);
		}

		try {
			FileCollection fc = a.getObjectReference(id);
			if(fc == null)
				throw new BWFLAException("could not find object");
			return fc.value();
		} catch (JAXBException e) {
			throw new BWFLAException(e);
		}
	}

	public void importObjectFromMetadata(String archive, String metadata) throws BWFLAException {
		DigitalObjectArchive a = getArchive(archive);
		a.importObject(metadata);
	}

	public void delete(String archive, String id) throws BWFLAException {
		DigitalObjectArchive a = getArchive(archive);
		a.delete(id);
	}

	public DigitalObjectMetadata getObjectMetadata(String archive, String id) throws BWFLAException {
		DigitalObjectArchive a = getArchive(archive);
		return a.getMetadata(id);
	}

	public @XmlMimeType("application/xml") DataHandler getObjectMetadataCollection(String archive) throws BWFLAException {
		final DigitalObjectArchive a = getArchive(archive);
		final Stream<DigitalObjectMetadata> objects = a.getObjectMetadata();
		return this.toDataHandler(objects, DigitalObjectMetadata.class, JaxbNames.DIGITAL_OBJECTS);
	}

	public int getNumObjectSeats(String archive, String id) throws BWFLAException {
		DigitalObjectArchive a = getArchive(archive);
		return a.getNumObjectSeats(id);
	}

	public int getNumObjectSeatsForTenant(String archive, String id, String tenant) throws BWFLAException
	{
		int seats = -1;

		if (tenant != null)
			seats = seatmgr.getNumSeats(tenant, archive, id);

		if (seats < 0) {
			// No tenant specific limits defined!
			seats = this.getNumObjectSeats(archive, id);
		}

		return seats;
	}

	public void setNumObjectSeatsForTenant(String archive, String id, String tenant, int seats) throws BWFLAException
	{
		seatmgr.setNumSeats(tenant, archive, id, seats);
	}

	public void setNumObjectSeatsForTenantBatched(String archive, List<SeatDescription> resources, String tenant) throws BWFLAException
	{
		seatmgr.setNumSeats(tenant, archive, resources);
	}

	public void resetNumObjectSeatsForTenant(String archive, String id, String tenant) throws BWFLAException
	{
		seatmgr.resetNumSeats(tenant, archive, id);
	}

	public void resetNumObjectSeatsForTenantBatched(String archive, List<String> ids, String tenant) throws BWFLAException
	{
		seatmgr.resetNumSeats(tenant, archive, ids);
	}

	public void resetAllObjectSeatsForTenant(String tenant) throws BWFLAException
	{
		seatmgr.resetNumSeats(tenant);
	}

	public TaskState getTaskState(String id)
	{
		return ObjectArchiveSingleton.getState(id);
	}

	public TaskState syncObjects(String _archive, List<String> objectIDs) throws BWFLAException {
		DigitalObjectArchive a = getArchive(_archive);
		return a.sync(objectIDs);
	}

	public void sync(String _archive)
	{
		if(!ObjectArchiveSingleton.confValid)
		{
			LOG.severe("ObjectArchive not configured");
			return;
		}
		
		DigitalObjectArchive a = ObjectArchiveSingleton.archiveMap.get(_archive);
		if(a == null)
			return;
		
		a.sync();
	}
	
	public void syncAll()
	{
		if(!ObjectArchiveSingleton.confValid)
		{
			LOG.severe("ObjectArchive not configured");
			return;
		}
		
		for(DigitalObjectArchive a : ObjectArchiveSingleton.archiveMap.values())
		{
			a.sync();
		}
	}

	public List<String> getArchives() {
		if (!ObjectArchiveSingleton.confValid) {
			LOG.severe("ObjectArchive not configured");
			return null;
		}

		Set<String> keys = ObjectArchiveSingleton.archiveMap.keySet();
		ArrayList<String> result = new ArrayList<>();
		for (String key : keys)
		{
			if(key.equals(tmpArchiveName))
				continue;

			result.add(key);
		}
		return result;
	}

	public void registerUserArchive(String userId) throws BWFLAException {
		ObjectArchiveSingleton.archiveMap.put(userId, new DigitalObjectUserArchive(userId));
	}

	private <T> DataHandler toDataHandler(Stream<T> source, Class<T> klass, String name)
	{
		try {
			final String mimetype = "application/xml";
			final JaxbCollectionWriter<T> pipe = new JaxbCollectionWriter<>(source, klass, name, mimetype, LOG);
			executor.execute(pipe);
			return pipe.getDataHandler();
		}
		catch (Exception error) {
			LOG.log(Level.WARNING, "Returning data-handler for '" + name + "' failed!", error);
			source.close();
			return null;
		}
	}
}

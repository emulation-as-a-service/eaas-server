package de.bwl.bwfla.objectarchive.util;

import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;

import de.bwl.bwfla.api.objectarchive.ObjectArchiveFacadeWS;
import de.bwl.bwfla.api.objectarchive.ObjectArchiveFacadeWSService;
import de.bwl.bwfla.api.objectarchive.TaskState;
import de.bwl.bwfla.common.datatypes.DigitalObjectMetadata;
import de.bwl.bwfla.common.datatypes.GenericId;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.jaxb.JaxbCollectionReader;
import de.bwl.bwfla.common.utils.jaxb.JaxbNames;
import de.bwl.bwfla.emucomp.api.FileCollection;
import de.bwl.bwfla.objectarchive.api.SeatDescription;


public class ObjectArchiveHelper {

	protected final Logger	log	= Logger.getLogger(this.getClass().getName());
	
	private ObjectArchiveFacadeWS archive = null;
	private final String wsHost;
	
	public ObjectArchiveHelper(String wsHost)
	{
		this.wsHost = wsHost;
	}
	
	private static ObjectArchiveFacadeWS getImageArchiveCon(String host)
	{
		URL wsdl;
		ObjectArchiveFacadeWS archive;
		try 
		{
			wsdl = new URL(host + "/objectarchive/ObjectArchiveFacadeWS?wsdl");
			ObjectArchiveFacadeWSService service = new ObjectArchiveFacadeWSService(wsdl);
			archive = service.getObjectArchiveFacadeWSPort();
		} 
		catch (Throwable t) 
		{
			// TODO Auto-generated catch block
			Logger.getLogger(ObjectArchiveFacadeWS.class.getName()).info("Can not initialize wsdl from " + host + "/objectarchive/ObjectArchiveFacadeWS?wsdl");
			return null;
		}

		BindingProvider bp = (BindingProvider)archive;
		SOAPBinding binding = (SOAPBinding) bp.getBinding();
		binding.setMTOMEnabled(true);
		bp.getRequestContext().put("javax.xml.ws.client.receiveTimeout", "0");
		bp.getRequestContext().put("javax.xml.ws.client.connectionTimeout", "0");
		bp.getRequestContext().put("com.sun.xml.internal.ws.transport.http.client.streaming.chunk.size", 8192);

		// make this threadsafe
		// https://stackoverflow.com/questions/10599959/is-this-jax-ws-client-call-thread-safe
		bp.getRequestContext().put("thread.local.request.context", "true");
		bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, host + "/objectarchive/ObjectArchiveFacadeWS?wsdl");

		return archive;
	}
	
	private void connectArchive() throws BWFLAException
	{
		if(archive != null)
			return;
		
		archive = getImageArchiveCon(wsHost);
		if(archive == null)
			throw new BWFLAException("could not connect to object archive @ " + wsHost);	
	}

	public String resolveObjectResource(String _archive, String objectId, String resourceId, String method) throws BWFLAException
	{
		connectArchive();
		return archive.resolveObjectResource(_archive, objectId, resourceId, method);
	}

	public String resolveObjectResourceInternally(String _archive, String objectId, String resourceId, String method) throws BWFLAException
	{
		connectArchive();
		return archive.resolveObjectResourceInternally(_archive, objectId, resourceId, method);
	}

	public DigitalObjectMetadata getObjectMetadata(String _archive, String id) throws BWFLAException
	{
		connectArchive();
		return archive.getObjectMetadata(_archive, id);
	}

	public void updateObjectLabel(String _archive, String id, String newLabel) throws BWFLAException
	{
		connectArchive();
		archive.updateLabel(_archive, id, newLabel);
	}

	public Stream<DigitalObjectMetadata> getObjectMetadata(String _archive) throws BWFLAException
	{
		connectArchive();

		final Source source = archive.getObjectMetadataCollection(_archive);
		try {
			final String name = JaxbNames.DIGITAL_OBJECTS;
			return new JaxbCollectionReader<>(source, DigitalObjectMetadata.class, name, log)
					.stream();
		}
		catch (Exception error) {
			throw new BWFLAException("Parsing objects failed!", error);
		}
	}

	public void importFromMetadata(String _archive, String metadata) throws BWFLAException
	{
		connectArchive();
		archive.importObjectFromMetadata(_archive, metadata);
	}
	
	public Stream<String> getObjectIds(String _archive) throws BWFLAException
	{
		connectArchive();

		final Source source = archive.getObjectIds(_archive);
		try {
			final String name = JaxbNames.DIGITAL_OBJECT_IDS;
			return new JaxbCollectionReader<>(source, GenericId.class, name, log)
					.stream()
					.map(GenericId::get);
		}
		catch (Exception error) {
			throw new BWFLAException("Parsing object IDs failed!", error);
		}

		
//		log.info(_archive + ": found " + objs.size() + " objects");
//		List<String> uniqueList = new ArrayList<String>(
//				new HashSet<String>(objs));
//		java.util.Collections.sort(uniqueList);
//		return uniqueList;
	}

	public FileCollection getObjectReference(String _archive, String id) throws BWFLAException
	{
		connectArchive();
		String colStr = archive.getObjectReference(_archive, id);
		if(colStr == null)
		{
			log.warning("could not get metadata for ID: " + id);
			return null;
		}
		
		FileCollection fc = null;
		try {
			fc = FileCollection.fromValue(colStr);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (fc == null || fc.id == null)
			return null;
		
		if(fc.files.size() == 0)
			return null;
		
		return fc;
	}
	
	public void sync(String _archive) throws BWFLAException
	{
		connectArchive();
		archive.sync(_archive);
	}

	public TaskState sync(String _archive, List<String> objectIDs) throws BWFLAException
	{
		connectArchive();
		return archive.syncObjects(_archive, objectIDs);
	}

	public TaskState getTaskState(String taskId) throws BWFLAException {
		connectArchive();
		return archive.getTaskState(taskId);
	}
	
	public void sync() throws BWFLAException
	{
		connectArchive();
		archive.syncAll();
	}

	public int getNumObjectSeats(String _archive, String objectId) throws BWFLAException {
		connectArchive();
		return archive.getNumObjectSeats(_archive, objectId);
	}

	public int getNumObjectSeatsForTenant(String _archive, String object, String tenant) throws BWFLAException {
		connectArchive();
		return archive.getNumObjectSeatsForTenant(_archive, object, tenant);
	}

	public void setNumObjectSeatsForTenant(String _archive, String object, String tenant, int seats) throws BWFLAException {
		connectArchive();
		archive.setNumObjectSeatsForTenant(_archive, object, tenant, seats);
	}

	public void setNumObjectSeatsForTenant(String _archive, List<SeatDescription> objects, String tenant) throws BWFLAException {
		connectArchive();
		archive.setNumObjectSeatsForTenantBatched(_archive, objects, tenant);
	}

	public void resetNumObjectSeatsForTenant(String _archive, String object, String tenant) throws BWFLAException {
		connectArchive();
		archive.resetNumObjectSeatsForTenant(_archive, object, tenant);
	}

	public void resetNumObjectSeatsForTenant(String _archive, List<String> objects, String tenant) throws BWFLAException {
		connectArchive();
		archive.resetNumObjectSeatsForTenantBatched(_archive, objects, tenant);
	}

	public void resetAllObjectSeatsForTenant(String tenant) throws BWFLAException {
		connectArchive();
		archive.resetAllObjectSeatsForTenant(tenant);
	}

	public List<String> getArchives() throws BWFLAException {
		connectArchive();
		return archive.getArchives();
	}

	public String getHost()
	{
		return wsHost;
	}

	public void registerUserArchive(String userId) throws BWFLAException {
		connectArchive();
		archive.registerUserArchive(userId);
	}

	public void delete(String _archive, String objectId) throws BWFLAException {
		connectArchive();
		archive.delete(_archive, objectId);
	}
}

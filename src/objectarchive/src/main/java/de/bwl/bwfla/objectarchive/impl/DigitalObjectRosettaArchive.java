package de.bwl.bwfla.objectarchive.impl;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bwl.bwfla.common.taskmanager.TaskState;
import de.bwl.bwfla.objectarchive.datatypes.bsb.BsbFileCollection;
import de.bwl.bwfla.objectarchive.datatypes.bsb.BsbFileCollectionEntry;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import org.apache.commons.io.IOUtils;

import de.bwl.bwfla.emucomp.api.FileCollection;
import de.bwl.bwfla.emucomp.api.FileCollectionEntry;
import de.bwl.bwfla.objectarchive.datatypes.DigitalObjectArchive;
import de.bwl.bwfla.common.datatypes.DigitalObjectMetadata;

public class DigitalObjectRosettaArchive implements Serializable, DigitalObjectArchive
{
	private String url;
	private boolean isDefaultArchive;

	public DigitalObjectRosettaArchive(String url, boolean isDefaultArchive) {
		this.url = url;
		this.isDefaultArchive = isDefaultArchive;
	}

	@Override
	public Stream<String> getObjectIds() {
		return Stream.empty();
	}

	@Override
	public FileCollection getObjectReference(String objectId) {
		InputStream in = null;
		try {
			in = new URL( url + objectId ).openStream();
			
			String json = IOUtils.toString( in );
			System.out.println("got json: " + json );
			final ObjectMapper mapper = new ObjectMapper();
			BsbFileCollection bsbFiles = mapper.readValue(json , BsbFileCollection.class);
			if(bsbFiles == null)
			{
				System.out.println("failed json");
				return null;
			}
			FileCollection fc = new FileCollection(bsbFiles.id);
			for(BsbFileCollectionEntry e : bsbFiles.files)
			{
				FileCollectionEntry fce = new FileCollectionEntry();
				fce.setId(e.getFileId());
				fce.setType(e.getType());
				fce.setUrl(e.getUrl());
				fc.files.add(fce);
			}
			return fc;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally { IOUtils.closeQuietly(in);
		}
		return null;
	}

	@Override
	public FileCollection getInternalReference(String objectId) throws BWFLAException
	{
		return getObjectReference(objectId);
	}

	@Override
	public void importObject(String metsdata) throws BWFLAException {

	}

	@Override
	public String getName() {
		return "emil-rosetta";
	}

	@Override
	public Path getLocalPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalObjectMetadata getMetadata(String objectId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<DigitalObjectMetadata> getObjectMetadata() {

		return this.getObjectIds()
				.map(this::getMetadata)
				.filter(Objects::nonNull);
	}

	@Override
	public void sync() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TaskState sync(List<String> objectId) {
		return null;
	}


	@Override
	public boolean isDefaultArchive() {
		return isDefaultArchive;
	}

	@Override
	public int getNumObjectSeats(String id) {
		return -1;
	}

	@Override
	public void delete(String id) throws BWFLAException {
		throw new BWFLAException("not supported");
	}
}

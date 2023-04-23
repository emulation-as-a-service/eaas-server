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

package de.bwl.bwfla.softwarearchive.impl;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.bwl.bwfla.common.datatypes.SoftwarePackage;
import de.bwl.bwfla.common.datatypes.SoftwareDescription;
import de.bwl.bwfla.softwarearchive.ISoftwareArchive;

import javax.xml.bind.JAXBException;


public class SoftwareFileArchive implements Serializable, ISoftwareArchive
{
	private static final long serialVersionUID = -8250444788579220131L;

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private final String name;
	private final Path archivePath;

	
	/**
	 * File-based SoftwareArchive, with SoftwarePackages stored as XML files:
	 * <i>path/ID</i>
	 */
	public SoftwareFileArchive(String name, String path)
	{
		this.name = name;
		this.archivePath = Paths.get(path);
	}

	@Override
	public boolean hasSoftwarePackage(String id)
	{
		SoftwarePackage swp = getSoftwarePackageById(id);
		if(swp == null)
			return false;
		return !swp.isDeleted();
	}

	@Override
	public boolean changeSoftwareLabel(String objectId, String newLabel)
	{
		log.info("Changing software label in SW Archive...");
		final Path path = archivePath.resolve(objectId);

		try {
			SoftwarePackage swPackage = this.getSoftwarePackageByPath(path);
			swPackage.setName(newLabel);
			Files.deleteIfExists(path); //necessary, as new label can be shorter than old one -> faulty XML file
			Files.write( path, swPackage.value(true).getBytes("UTF-8"), StandardOpenOption.CREATE);

		}
		catch (IOException | JAXBException e) {
			log.warning("Updating Software Label failed!" + e);
			return false;
		}

		return true;
	}

	@Override
	public synchronized boolean addSoftwarePackage(SoftwarePackage software)
	{
		final String id = software.getObjectId();
		final Path path = archivePath.resolve(id);
		if (Files.exists(path)) {
			log.info("Software package with ID " + id + " already exists! Replacing it...");
			try {
				Files.deleteIfExists(path);
			} catch (IOException exception) {
				log.warning("Deleting software package with ID " + id + " failed!");
				log.log(Level.SEVERE, exception.getMessage(), exception);
			}
		}
		try {
			Files.write( path, software.value(true).getBytes("UTF-8"), StandardOpenOption.CREATE);
		} catch (IOException | JAXBException e) {
			e.printStackTrace();
			log.warning("Writing software package '" + path.toString() + "' failed!");
			return false;
		}
		return true;
	}
	
	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public int getNumSoftwareSeatsById(String id)
	{
		SoftwarePackage software = this.getSoftwarePackageById(id);
		return (software != null) ? software.getNumSeats() : -1;
	}

	@Override
	public void deleteSoftware(String id) {
		SoftwarePackage swp = getSoftwarePackageById(id);
		if(swp == null)
			return;

		swp.setDeleted(true);
		addSoftwarePackage(swp);
	}

	@Override
	public SoftwarePackage getSoftwarePackageById(String id)
	{
		final Path path = archivePath.resolve(id);
		if (Files.notExists(path)) {
			// log.warning("Software package with ID " + id + " does not exist!");
			return null;
		}
		// check if QID is set
		// if QID => get support file formats *read and write
		// add PUIDs to array
		SoftwarePackage sp = this.getSoftwarePackageByPath(path);
		if(sp == null)
			return null;

		String QID =  sp.getQID();
		List<String> formats = sp.getSupportedFileFormats();
		if (formats == null) {
			formats = new ArrayList<String>();
		}

		// if(QID  != null)
		//	QIDsFinder.extendSupportedFormats(QID, formats);
		return sp;
	}

	@Override
	public Stream<String> getSoftwarePackageIds()
	{
		try {
			final DirectoryStream<Path> files = Files.newDirectoryStream(archivePath);
			return StreamSupport.stream(files.spliterator(), false)
					.filter((path) -> {
						SoftwarePackage swp = getSoftwarePackageByPath(path);
						return swp != null && !swp.isDeleted();
					}).map((path) -> path.getFileName().toString()).onClose(() -> {
						try {
							files.close();
						}
						catch (Exception error) {
							log.log(Level.WARNING, "Closing directory-stream failed!", error);
						}
					});
		}
		catch (Exception exception) {
			log.log(Level.SEVERE, "Reading software package directory failed!", exception);
			return Stream.empty();
		}
	}

	@Override
	public Stream<SoftwarePackage> getSoftwarePackages()
	{
		return this.getSoftwarePackageIds()
				.map(this::getSoftwarePackageById);
	}
	
	@Override
	public SoftwareDescription getSoftwareDescriptionById(String id)
	{
		SoftwarePackage software = this.getSoftwarePackageById(id);
		if (software == null)
			return null;
		
		SoftwareDescription result =  new SoftwareDescription(id, software.getName(), software.getIsOperatingSystem(), software.getArchive());
		result.setPublic(software.isPublic());
		return result;
	}
	
	@Override
	public Stream<SoftwareDescription> getSoftwareDescriptions()
	{
		return this.getSoftwarePackageIds()
				.map(this::getSoftwareDescriptionById);
	}
	

	/* =============== Internal Methods =============== */
	
	private synchronized SoftwarePackage getSoftwarePackageByPath(Path path)
	{
		try {
			byte[] encoded = Files.readAllBytes(path);
			return SoftwarePackage.fromValue(new String(encoded, StandardCharsets.UTF_8), SoftwarePackage.class);
		}
		catch (Exception exception) {
			log.warning("Reading software package '" + path.toString() + "' failed: " +  exception);
			return null;
		}
	}
}

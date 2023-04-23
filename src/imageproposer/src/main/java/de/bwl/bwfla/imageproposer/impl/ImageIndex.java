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

package de.bwl.bwfla.imageproposer.impl;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.openslx.eaas.common.databind.DataUtils;
import de.bwl.bwfla.common.datatypes.identification.OperatingSystemInformation;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;


public class ImageIndex
{
	private final Map<String, Set<String>> entriesByPUID;
	private final Map<String, Set<String>> entriesByExt;
	private final Map<String, Set<String>> operatingSystemsPUIDInv;
	private final Map<String, Set<String>> operatingSystemsExtInv;
	private final Map<String, OperatingSystemInformation> operatingSystems;
	private final Map<String, OperatingSystemInformation> operatingSystemExtMap;

	public ImageIndex(Map<String, OperatingSystemInformation> operatingSystems)
	{
		this.operatingSystems = operatingSystems;
		this.entriesByPUID = new HashMap<String, Set<String>>();
		this.entriesByExt = new HashMap<String, Set<String>>();
		this.operatingSystemsPUIDInv = new HashMap<String, Set<String>>();
		this.operatingSystemsExtInv = new HashMap<String, Set<String>>();
		this.operatingSystemExtMap = new HashMap<>();

		if(operatingSystems == null)
			return;

		for(String os : operatingSystems.keySet())
		{
			OperatingSystemInformation operatingSystemInformation = operatingSystems.get(os);

			if(operatingSystemInformation.getPuids() != null) {
				for (String puid : operatingSystemInformation.getPuids()) {
					Set<String> osSet = operatingSystemsPUIDInv.get(puid);
					if (osSet == null) {
						osSet = new HashSet<String>();
						operatingSystemsPUIDInv.put(puid, osSet);
					}

					osSet.add(os);
				}
			}

			if(operatingSystemInformation.getExtensions() != null) {
				for (String ext : operatingSystemInformation.getExtensions()) {
					Set<String> osSet = operatingSystemsExtInv.get(ext);
					if(osSet == null)
					{
						osSet = new HashSet<>();
						operatingSystemsExtInv.put(ext, osSet);
					}
					osSet.add(os);
				}
			}
		}

	}


	private void printEntry(String description, Object map){
		try {
			System.out.println(description + ":\n" + DataUtils.json().writer(true).writeValueAsString(map));
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public void printMaps(){

		System.out.println("---------------------------------");
		System.out.println("---------------------------------");
		System.out.println("---------------------------------");
		System.out.println("---------------------------------");
		printEntry("operatingSystems", this.operatingSystems);
		printEntry("entriesByExt", this.entriesByExt);
		printEntry("entriesByPUID", this.entriesByPUID);
		printEntry("operatingSystemsExtInv", this.operatingSystemsExtInv);
		printEntry("operatingSystemsPUIDInv", this.operatingSystemsPUIDInv);
		printEntry("operatingSystemExtMap", this.operatingSystemExtMap);


	}

	public OperatingSystemInformation getOperatingSystemInfo(String osId)
	{
		return operatingSystems.get(osId);
	}

	public Set<String> getOsRequirementByPUID(String format)
	{
		if(!operatingSystemsPUIDInv.containsKey(format))
			return null;
		return new HashSet<String>(operatingSystemsPUIDInv.get(format));
	}

	public OperatingSystemInformation getOperatingSystemByExt(String osId)
	{
		return operatingSystemExtMap.get(osId);
	}

	public Set<String> getOsRequirementByExt(String format)
	{
		if(!operatingSystemsExtInv.containsKey(format))
			return null;
		return new HashSet<String>(operatingSystemsExtInv.get(format));
	}
	
	public Set<String> getEnvironmentsByPUID(String format)
	{
		return entriesByPUID.get(format);
	}

	public Set<String> getEnvironmentsByExt(String ext)
	{
		return entriesByExt.get(ext);
	}
	
	public void addEnvironmentWithPUID(String format, String image)
	{
		Set<String> entry = entriesByPUID.get(format);
		if (entry == null) {
			entry = new HashSet<String>();
			entriesByPUID.put(format, entry);
		}
		
		entry.add(image);
	}

	public void addEnvironmentWithExt(String ext, String image) {
		Set<String> entry = entriesByExt.get(ext);
		if (entry == null) {
			entry = new HashSet<String>();
			entriesByExt.put(ext, entry);
		}

		entry.add(image);
	}

	public Set<String> put(String format, Set<String> images)
	{
		return entriesByPUID.put(format, images);
	}
	
	public void clear()
	{
		entriesByPUID.clear();
	}
	
	public int size()
	{
		return entriesByPUID.size();
	}

	//TODO unused, can be removed?
	public static ImageIndex createFromJson(Path path)
	{
		final Logger log = Logger.getLogger(ImageIndex.class.getName());
		
		if (!Files.exists(path) || Files.isDirectory(path)) {
			log.severe("Invalid path for image index description specified: " + path.toString());
			return null;
		}
		
		ImageIndex index = null;
		try {
			index = ImageIndex.parseJson(path);
		} catch (Exception exception) {
			log.warning("Parsing image index's description failed: " + path);
			log.log(Level.WARNING, exception.getMessage(), exception);
		}
		
		return index;
	}

	//TODO unused, can be removed?
	private static ImageIndex parseJson(Path input) throws IOException
	{
		final Logger log = Logger.getLogger(ImageIndex.class.getName());
		JsonObject object = null;
		
		try (Reader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
			 JsonReader json = Json.createReader(reader)) {
			object = json.readObject();
		}

		final JsonValue entries = object.get("entriesByPUID");
		if (entries == null || (entries.getValueType() != JsonValue.ValueType.ARRAY)) {
			log.warning("Image index description is invalid: " + input.toString());
			return null;
		}

		final ImageIndex index = new ImageIndex(null);
		for (JsonValue element : entries.asJsonArray()) {
			final JsonObject entry = element.asJsonObject();
			final String format = entry.getString("format");
			final JsonArray images = entry.getJsonArray("images");
			if (format == null || images == null)
				continue;

			for (int i = 0; i < images.size(); ++i)
				index.addEnvironmentWithPUID(format, images.getString(i));
		}
		
		return index;
	}


}

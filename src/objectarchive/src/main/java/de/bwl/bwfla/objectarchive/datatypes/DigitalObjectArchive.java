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

package de.bwl.bwfla.objectarchive.datatypes;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.openslx.eaas.migration.MigrationRegistry;
import de.bwl.bwfla.common.datatypes.DigitalObjectMetadata;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.guacplay.util.NotImplementedException;
import de.bwl.bwfla.common.taskmanager.TaskState;
import de.bwl.bwfla.emucomp.api.FileCollection;


/**
 *
 * @author klaus
 * This interface is the internal view on object archive impl. Currently it 
 * is in sync with ObjectArchiveFacadeWSRemote, however, the implementation 
 * is not bound to the WS interface. The WS facade is in charge of translating 
 * between DigitalObjectArchive and the Facade. 
 */
public interface DigitalObjectArchive
{
	Stream<String> getObjectIds();
	FileCollection getObjectReference(String objectId) throws BWFLAException;
	FileCollection getInternalReference(String objectId) throws BWFLAException;
	void importObject(String metsdata) throws BWFLAException;
	String getName();
	Path getLocalPath();
	DigitalObjectMetadata getMetadata(String objectId) throws BWFLAException;
	DigitalObjectMetadata getUnresolvedMetadata(String objectId) throws BWFLAException;

	Stream<DigitalObjectMetadata> getObjectMetadata();
	boolean isDefaultArchive();

    int getNumObjectSeats(String id);

	void sync();

    TaskState sync(List<String> objectId);

    void delete(String id) throws BWFLAException;

	default String resolveObjectResource(String objectId, String resourceId, String method) throws BWFLAException
	{
		final var object = this.getObjectReference(objectId);
		return resolveHelper(resourceId, object);
	}

	default String resolveObjectResourceInternally(String objectId, String resourceId, String method) throws BWFLAException
	{
		final var object = this.getInternalReference(objectId);
		return resolveHelper(resourceId, object);
	}

	default void register(MigrationRegistry migrations) throws Exception
	{
		// Empty!
	}

	default void updateLabel(String objectId, String newLabel) throws BWFLAException
	{
		throw new NotImplementedException("This archive does not support updating the label.");
	}

	default void markAsSoftware(String objectId, boolean isSoftware) throws BWFLAException
	{
		throw new NotImplementedException("This archive does not support updating metadata.");
	}

	private static String resolveHelper(String resourceId, FileCollection object)
	{
		if (object == null)
			return null;

		final var resource = object.find(resourceId);
		return (resource != null) ? resource.getUrl() : null;
	}
}

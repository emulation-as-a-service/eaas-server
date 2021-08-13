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

package com.openslx.eaas.imagearchive.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.openslx.eaas.imagearchive.ArchiveBackend;
import com.openslx.eaas.imagearchive.BlobKind;
import com.openslx.eaas.imagearchive.indexing.impl.MetaDataIndex;
import com.openslx.eaas.imagearchive.service.DataService;
import com.openslx.eaas.imagearchive.storage.StorageRegistry;


public class MetaDataService extends DataService<JsonNode, MetaDataIndex.Record>
{
	public static MetaDataService create(BlobKind kind, ArchiveBackend backend)
	{
		final var index = backend.indexes()
				.lookup(kind);

		return new MetaDataService(backend.storage(), (MetaDataIndex) index);
	}


	// ===== Internal Helpers ==============================

	private MetaDataService(StorageRegistry storage, MetaDataIndex index)
	{
		super(storage, index, MetaDataIndex.Record::filter, MetaDataIndex.Record::filter);
	}
}

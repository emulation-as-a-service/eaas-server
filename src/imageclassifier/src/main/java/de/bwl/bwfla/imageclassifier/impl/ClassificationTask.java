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

package de.bwl.bwfla.imageclassifier.impl;

import java.util.*;
import java.util.concurrent.ExecutorService;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.services.security.UserContext;
import de.bwl.bwfla.emucomp.api.FileCollection;
import de.bwl.bwfla.emucomp.api.FileCollectionEntry;
import de.bwl.bwfla.imageclassifier.client.ClassificationEntry;
import de.bwl.bwfla.imageclassifier.client.IdentificationRequest;
import de.bwl.bwfla.imageclassifier.client.Identification;
import de.bwl.bwfla.imageclassifier.datatypes.FileIdentificationResult;
import de.bwl.bwfla.imageclassifier.datatypes.IdentificationData;
import de.bwl.bwfla.imageclassifier.datatypes.IdentificationResult;
import de.bwl.bwfla.imageclassifier.datatypes.IdentificationResultContainer;

public class ClassificationTask extends BaseTask
{
	public ClassificationTask(IdentificationRequest request, UserContext userctx, ExecutorService executor) {
		super(request, userctx, executor);
	}

	@Override
	public Object execute() throws Exception {
		log.info("Starting classification task...");

		final IdentificationResultContainer<?> container = super.identify();

		Object iResult = container.getData();
		if (iResult instanceof IdentificationResult) {

			final IdentificationResult identification = (IdentificationResult) iResult;

			log.info("Constructing classification response...");

			final Map<String, String> policy = identification.getPolicy();
			HashMap<String, IdentificationData<?>> data = identification.getIdentificationData();
			FileCollection fc = identification.getFileCollection();

			HashMap<String, Identification.IdentificationDetails<ClassificationEntry>> resultHashMap = new HashMap<>();
			for (FileCollectionEntry fce : fc.files) {
				IdentificationData<?> idData = data.get(fce.getId());
				if (idData == null)
					continue;

				Identification.IdentificationDetails<ClassificationEntry> details = new Identification.IdentificationDetails<>();
				details.setDiskType(idData.getType());
				if (idData.getIndex() != null)
					details.setEntries(idData.getIndex().getClassifierList(policy));

				resultHashMap.put(fce.getId(), details);
			}

			log.info("Classification response constructed.");


			return new Identification<>(fc, resultHashMap);
		}
		else if(iResult instanceof FileIdentificationResult)
		{
			final FileIdentificationResult identification = (FileIdentificationResult)iResult;
			IdentificationData<?> idData = identification.getData();
			Identification.IdentificationDetails<ClassificationEntry> details = new Identification.IdentificationDetails<>();
			details.setDiskType(idData.getType());
			if (idData.getIndex() != null)
				details.setEntries(idData.getIndex().getClassifierList( new HashMap<String, String>()));

			HashMap<String, Identification.IdentificationDetails<ClassificationEntry>> resultHashMap = new HashMap<>();
			resultHashMap.put(identification.getFileName(), details);

			return new Identification<>(identification.getFileName(), identification.getUrl(), resultHashMap);
		}
		else throw new BWFLAException("unknown identification result");
	}
}

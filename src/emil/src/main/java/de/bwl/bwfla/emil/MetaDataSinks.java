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

import com.openslx.eaas.imagearchive.ImageArchiveClient;
import com.openslx.eaas.imagearchive.api.v2.common.ReplaceOptionsV2;
import com.openslx.eaas.imagearchive.client.endpoint.v2.EnvironmentsV2;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.emil.datatypes.EaasiSoftwareObject;
import de.bwl.bwfla.emil.datatypes.EmilEnvironment;
import de.bwl.bwfla.emucomp.api.Environment;
import de.bwl.bwfla.metadata.repository.api.ItemDescription;
import de.bwl.bwfla.metadata.repository.sink.ItemSink;
import de.bwl.bwfla.metadata.repository.sink.MetaDataSink;

import javax.xml.bind.JAXBException;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class MetaDataSinks
{
	public static MetaDataSink images(String archive, ImageArchiveClient imagearchive)
	{
		return new MetaDataSink()
				.set(new EnvironmentSink(archive, imagearchive));
	}

	public static MetaDataSink environments(EmilEnvironmentRepository environmentRepository)
	{
		return new MetaDataSink()
				.set(new EmilEnvironmentSink(environmentRepository));
	}

	public static MetaDataSink software(EmilSoftwareData swData)
	{
		return new MetaDataSink()
				.set(new SoftwareSink(swData));
	}


	// ========== MetaDataSink Implementations =========================

	private static class EnvironmentSink implements ItemSink
	{
		private final Logger log = Logger.getLogger(this.getClass().getName());
		private final EnvironmentsV2 environments;
		private final ReplaceOptionsV2 options;

		public EnvironmentSink(String archive, ImageArchiveClient imagearchive)
		{
			this.environments = imagearchive.api()
					.v2()
					.environments();

			this.options = new ReplaceOptionsV2()
				.setLocation(archive);
		}

		@Override
		public void insert(ItemDescription item) throws BWFLAException
		{
			try {
				final Environment environment = Environment.fromValue(item.getMetaData());
				if (environments.exists(environment.getId()))
					return;

				environments.replace(environment.getId(), environment, options);
			}
			catch (JAXBException error) {
				throw new BWFLAException(error);
			}
		}

		@Override
		public void insert(Stream<ItemDescription> items) throws BWFLAException
		{
			final Function<ItemDescription, Integer> inserter = (item) -> {
				try {
					this.insert(item);
				}
				catch (Exception error) {
					log.log(Level.WARNING, "Inserting item '" + item.getIdentifier().getId() + "' failed!", error);
					return 1;
				}

				return 0;
			};

			final Optional<Integer> numfailed = items.map(inserter)
					.reduce((i1, i2) -> i1 + i2);

			if (!numfailed.isPresent())
				return;   // Stream was empty, no items inserted!

			if (numfailed.get() > 0)
				throw new BWFLAException("Inserting " + numfailed.get() + " item(s) failed!");
		}
	}

	private static class EmilEnvironmentSink implements ItemSink
	{
		private final Logger log = Logger.getLogger(this.getClass().getName());
		private final EmilEnvironmentRepository environmentRepository;


		public EmilEnvironmentSink(EmilEnvironmentRepository environmentRepository)
		{
			this.environmentRepository = environmentRepository;
		}

		@Override
		public void insert(ItemDescription item) throws BWFLAException
		{
			try {
				final EmilEnvironment environment = EmilEnvironment.fromValue(item.getMetaData(), EmilEnvironment.class);
				if (environmentRepository.existsEmilEnvironment(environment.getEnvId()))
					return;

				environment.setArchive("remote");
				environmentRepository.save(environment, false);
			}
			catch (JAXBException error) {
				throw new BWFLAException(error);
			}
		}

		@Override
		public void insert(Stream<ItemDescription> items) throws BWFLAException
		{
			final Function<ItemDescription, Integer> inserter = (item) -> {
				try {
					this.insert(item);
				}
				catch (Exception error) {
					log.log(Level.WARNING, "Inserting item '" + item.getIdentifier().getId() + "' failed!", error);
					return 1;
				}

				return 0;
			};

			final Optional<Integer> numfailed = items.map(inserter)
					.reduce((i1, i2) -> i1 + i2);

			if (!numfailed.isPresent())
				return;

			if (numfailed.get() > 0)
				throw new BWFLAException("Inserting " + numfailed.get() + " item(s) failed!");
		}
	}

	private static class SoftwareSink implements ItemSink
	{
		private final Logger log = Logger.getLogger(this.getClass().getName());
		private final EmilSoftwareData softwareData;

		public SoftwareSink(EmilSoftwareData softwareData)
		{
			this.softwareData = softwareData;
		}

		@Override
		public void insert(ItemDescription item) throws BWFLAException
		{
			try {
				final EaasiSoftwareObject software = EaasiSoftwareObject.fromValue(item.getMetaData(), EaasiSoftwareObject.class);
				if(software.getMetsData() != null)
					softwareData.importSoftware(software);
			}
			catch (Exception error) {
				throw new BWFLAException(error);
			}
		}

		@Override
		public void insert(Stream<ItemDescription> items) throws BWFLAException
		{
			final Function<ItemDescription, Integer> inserter = (item) -> {
				try {
					this.insert(item);
				}
				catch (Exception error) {
					log.log(Level.WARNING, "Inserting item '" + item.getIdentifier().getId() + "' failed!", error);
					return 1;
				}

				return 0;
			};

			final Optional<Integer> numfailed = items.map(inserter)
					.reduce((i1, i2) -> i1 + i2);

			if (!numfailed.isPresent())
				return;   // Stream was empty, no items inserted!

			if (numfailed.get() > 0)
				throw new BWFLAException("Inserting " + numfailed.get() + " item(s) failed!");
		}
	}


	private MetaDataSinks()
	{
		// Empty!
	}
}

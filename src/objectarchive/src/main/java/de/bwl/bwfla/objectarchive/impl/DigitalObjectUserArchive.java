package de.bwl.bwfla.objectarchive.impl;

import com.openslx.eaas.common.concurrent.ParallelProcessors;
import com.openslx.eaas.migration.config.MigrationConfig;
import de.bwl.bwfla.blobstore.BlobStore;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.objectarchive.conf.ObjectArchiveSingleton;
import de.bwl.bwfla.objectarchive.datatypes.DigitalObjectS3ArchiveDescriptor;
import org.apache.tamaya.ConfigurationProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static de.bwl.bwfla.objectarchive.impl.DigitalObjectFileArchive.UpdateCounts;


public class DigitalObjectUserArchive extends DigitalObjectS3Archive
{
    public DigitalObjectUserArchive(DigitalObjectS3ArchiveDescriptor descriptor) throws BWFLAException
    {
        super(descriptor);
    }

    public static Stream<String> listArchiveNames(DigitalObjectS3ArchiveDescriptor descriptor) throws BWFLAException
    {
        final var userArchivePrefix = ConfigurationProvider.getConfiguration()
                .get("objectarchive.user_archive_prefix");

        final var log = Logger.getLogger(ObjectArchiveSingleton.LOGGER_NAME);
        final var blobstore = BlobStore.builder()
                .endpoint(descriptor.getEndpoint())
                .credentials(descriptor.getAccessKey(), descriptor.getSecretKey())
                .logger(log)
                .build();

        final var bucket = blobstore.bucket(descriptor.getBucket());
        try {
            final var prefix = descriptor.getPath();
            return bucket.list(prefix)
                    .map(new ObjectIdMapper(prefix))
                    .filter(Objects::nonNull)
                    .filter((archiveId) -> archiveId.startsWith(userArchivePrefix));
        }
        catch (Exception exception) {
            log.log(Level.SEVERE, "Listing object-archive names failed!", exception);
            return Stream.empty();
        }
    }

    public static void renameArchives(MigrationConfig mc) throws Exception
    {
        final var log = Logger.getLogger(DigitalObjectUserArchive.class.getName());
        final var config = ConfigurationProvider.getConfiguration();
        final var counter = UpdateCounts.counter();

        final var basedir = Path.of(config.get("objectarchive.userarchive"));
        final var newNamePrefix = config.get("objectarchive.user_archive_prefix");
        final var legacyNamePrefix = "user_archive";

        final Consumer<Path> renamer = (curpath) -> {
            final var curname = curpath.getFileName().toString();
            if (!curname.startsWith(legacyNamePrefix))
                return;

            final var newname = curname.replace(legacyNamePrefix, newNamePrefix);
            final var newpath = curpath.getParent()
                    .resolve(newname);

            try {
                Files.move(curpath, newpath);
                counter.increment(UpdateCounts.UPDATED);
                log.info("Renamed object-archive: " + curname + " -> " + newname);
            }
            catch (Exception error) {
                log.log(Level.WARNING, "Renaming object-archive '" + curname + "' failed!", error);
                counter.increment(UpdateCounts.FAILED);
            }
        };

        if (!Files.exists(basedir)) {
            log.info("No private object-archives found!");
            return;
        }

        log.info("Renaming private object-archives...");
        try (final var dirs = Files.list(basedir)) {
            ParallelProcessors.consumer(renamer)
                    .consume(dirs, ObjectArchiveSingleton.executor());
        }

        final var numRenamed = counter.get(UpdateCounts.UPDATED);
        final var numFailed = counter.get(UpdateCounts.FAILED);
        log.info("Renamed " + numRenamed + " private object-archive(s), failed " + numFailed);
    }
}

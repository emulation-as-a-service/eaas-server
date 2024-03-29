package de.bwl.bwfla.imagearchive.util;

import java.net.URL;

import de.bwl.bwfla.api.imagearchive.*;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.emucomp.api.*;


public class EnvironmentsAdapter extends ImageArchiveWSClient {

	public EnvironmentsAdapter(String wsHost) {
		super(wsHost);
	}

	@Deprecated
	private static final String EMULATOR_DEFAULT_ARCHIVE = "emulators";
	
	@Deprecated
	public ImportImageHandle importImage(String backend, URL ref, ImageArchiveMetadata iaMd, boolean deleteIfExists) throws BWFLAException {
		connectArchive();
		if (ref == null)
			throw new BWFLAException("URL was null");

		String sessionId = archive.importImageFromUrl(backend, ref.toString(), iaMd);
		return new ImportImageHandle(archive, backend, iaMd.getType(), sessionId);
	}

	@Deprecated
	public void deleteNameIndexesEntry(String id, String version) throws BWFLAException {
		this.deleteNameIndexesEntry(this.getDefaultBackendName(), id, version);
	}

	@Deprecated
	public void deleteNameIndexesEntry(String backend, String id, String version) throws BWFLAException {
		connectArchive();
		archive.deleteNameIndexesEntry(backend, id, version);
	}

	@Deprecated
	public class ImportImageHandle {
		private final String sessionId;
		private final ImageType type;
		private final ImageArchiveWS archive;
		private final String backend;

		ImportImageHandle(ImageArchiveWS archive, String backend, ImageType type, String sessionId) {
			this.sessionId = sessionId;
			this.type = type;
			this.archive = archive;
			this.backend = backend;
		}

		public ImageArchiveBinding getBinding() throws ImportNoFinishedException, BWFLAException {
			final ImageImportResult result = archive.getImageImportResult(backend, sessionId);
			if (result == null)
				throw new ImportNoFinishedException();

			return new ImageArchiveBinding(backend, result.getImageId(), type.value());
		}

		public ImageArchiveBinding getBinding(long timeout /* seconds */ ) throws BWFLAException {

			ImageArchiveBinding binding = null;

			while (binding == null) { // will throw a BWFLAException in case of an error
				try {
					if (timeout < 0)
						throw new BWFLAException("getBinding: timeout exceeded");
					binding = getBinding();
					timeout--;
				} catch (EnvironmentsAdapter.ImportNoFinishedException e) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						throw new BWFLAException(e1);
					}
				}
			}
			return binding;
		}
	}

	@Deprecated
	public EmulatorMetadata extractMetadata(String imageId) throws BWFLAException {
		connectArchive();
		return archive.extractMetadata(EMULATOR_DEFAULT_ARCHIVE, imageId);
	}

	@Deprecated
	public ImageNameIndex getNameIndexes() throws BWFLAException {
		connectArchive();
		return archive.getNameIndexes(getDefaultBackendName());
	}

	@Deprecated
	public ImageNameIndex getNameIndexes(String backend) throws BWFLAException {
		 connectArchive();
		 return archive.getNameIndexes(backend);
	}

	@Deprecated
	public String resolveEmulatorImage(String imgid) throws BWFLAException {
		return this.resolveImage(EMULATOR_DEFAULT_ARCHIVE, imgid);
	}

	@Deprecated
	public String resolveImage(String backend, String imgid) throws BWFLAException {
		connectArchive();
		return archive.resolveImage(backend, imgid);
	}

	public static class ImportNoFinishedException extends Exception {  }
}

package de.bwl.bwfla.imagearchive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.activation.DataHandler;

import com.openslx.eaas.imagearchive.ImageArchiveClient;
import com.openslx.eaas.resolver.DataResolvers;
import de.bwl.bwfla.common.services.guacplay.io.Metadata;
import de.bwl.bwfla.common.services.handle.HandleClient;
import de.bwl.bwfla.common.services.handle.HandleException;
import de.bwl.bwfla.common.taskmanager.TaskState;
import de.bwl.bwfla.common.utils.*;
import de.bwl.bwfla.common.utils.jaxb.JaxbType;
import de.bwl.bwfla.emucomp.api.*;
import de.bwl.bwfla.imagearchive.ImageIndex.Alias;
import de.bwl.bwfla.imagearchive.ImageIndex.ImageMetadata;
import de.bwl.bwfla.imagearchive.ImageIndex.ImageDescription;
import de.bwl.bwfla.imagearchive.ImageIndex.ImageNameIndex;
import de.bwl.bwfla.imagearchive.conf.ImageArchiveBackendConfig;
import de.bwl.bwfla.imagearchive.datatypes.EmulatorMetadata;
import de.bwl.bwfla.imagearchive.datatypes.ImageArchiveMetadata;
import de.bwl.bwfla.imagearchive.datatypes.ImageImportResult;
import de.bwl.bwfla.imagearchive.tasks.ImportImageTask;
import org.apache.commons.io.FileUtils;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.imagearchive.datatypes.ImageArchiveMetadata.ImageType;


public class ImageHandler
{
	protected final Logger log; // = Logger.getLogger(ImageHandler.class.getName());

	private HashMap<String, FutureTask<ImageLoaderResult>> importTasks = new HashMap<>();

	private final ImageArchiveBackendConfig iaConfig;
	private final ImageMetadataCache cache;
	private final HandleClient handleClient;
	private ImageNameIndex imageNameIndex;
	private final ExecutorService pool;



	/** Map containing lock-objects for images with in-progress operations */
	private final ConcurrentHashMap<String, ImageLock> locks;

	public ImageHandler(ImageArchiveBackendConfig config, ImageMetadataCache cache, Logger log) throws BWFLAException {
		this.log = log;
		this.iaConfig = config;
		this.cache = cache;
		this.locks = new ConcurrentHashMap<>();
		pool = Executors.newFixedThreadPool(20);

		// compatibility hack: keep old installations working
		String indexPath;
		if(config.getName().equals("emulators"))
		{
			indexPath = "/home/bwfla/server-data/nameindexes.dump";
		}
		else {
			// compat hack pt2: old configs pointed to a directory
			indexPath = config.getNameIndexConfigPath();
			Path p = Paths.get(indexPath);
			if(!Files.exists(p)) {
				try {
					Files.createDirectories(p);
				} catch (IOException e) {
					e.printStackTrace();
					throw new BWFLAException(e);
				}
			}
			if(Files.isDirectory(p)) {
				indexPath = p.resolve(config.getName() + ".yaml").toString();
			}
		}

		if (new File(indexPath).exists()) {
			this.imageNameIndex = ImageNameIndex.parse(indexPath);
		}
		else this.imageNameIndex = new ImageNameIndex(indexPath);

		this.handleClient = (config.isHandleConfigured()) ? new HandleClient() : null;

		cleanTmpFiles();
		//resolveLocalBackingFiles();
	}

	public void lock(String id)
	{
		locks.computeIfAbsent(id, (unused) -> new ImageLock())
				.acquire();
	}

	public void unlock(String id)
	{
		locks.computeIfPresent(id, (unused, lock) -> {
			// Remove lock object, when unused
			return (lock.release() > 0) ? lock : null;
		});
	}

	public ImageNameIndex getNameIndexes(){
		return imageNameIndex;
	}

	public void addNameIndexesEntry(ImageMetadata entry, Alias alias) throws BWFLAException {
		imageNameIndex.addNameIndexesEntry(entry, alias);

		// compat hack
		/*
		if(iaConfig.getName().equals("emulators"))
			createLocalEmulatorQcow(entry.getImage().getId());

		 */
	}

	public void deleteNameIndexesEntry(String id, String version) {
    	imageNameIndex.delete(id, version);
	}

	public void updateLatestEmulator(String emulator, String version) {
		imageNameIndex.updateLatest(emulator, version);
	}

	private String getArchivePrefix()
	{
		String prefix = iaConfig.getHttpPrefix();
		// make sure there's a trailing slash in the URL base
		if (!prefix.endsWith("/")) {
			prefix += "/";
		}

		return prefix;
	}

	public String getExportPrefix()
	{
		/*
		if (iaConfig.isHandleConfigured())
			return "http://hdl.handle.net/" + iaConfig.getHandlePrefix() + "/";
		*/

		return this.getArchivePrefix();
	}

//	private ImageExport.ImageFileInfo getDependency(ImageType parentType, String parentId) throws IOException, BWFLAException {
//
//		File f = new File(iaConfig.getImagePath() + "/" + parentType.name() + "/" + parentId);
//		if(!f.exists())
//			throw new BWFLAException("parent file not found. broken parameters");
//
//		ImageInformation info = new ImageInformation(f.getAbsolutePath(), log);
//		if (info.getBackingFile() == null)
//			return null;
//
//		String id = getBackingImageId(info.getBackingFile());
//		if (id == null)
//			return null;
//
//		File file = null;
//		ImageType type = null;
//		for(ImageType _type : ImageType.values()) {
//			File backing = new File(iaConfig.getImagePath() + "/" + _type.name() + "/" + id);
//
//			if(backing.exists()) {
//				file = backing;
//				type = _type;
//			}
//		}
//		if(file == null)
//			return null;
//
//		return new ImageExport.ImageFileInfo(getArchivePrefix(), id, type);
//	}

	public Path findImagePathById(String imageid)
	{
		final var basepath = iaConfig.getImagePath()
				.toPath()
				.toAbsolutePath();

		for (ImageType type : ImageType.values()) {
			final var imgpath = basepath.resolve(type.name())
					.resolve(imageid);

			if (Files.exists(imgpath))
				return imgpath;
		}

		return null;
	}

	public ImageInformation.QemuImageFormat findBackingFileFormat(String bfid, String bfurl) throws Exception
	{
		ImageInformation bfinfo;
		try {
			bfinfo = new ImageInformation(bfurl, log);
		}
		catch (Exception error) {
			log.log(Level.WARNING, "Looking up backing file via URL failed!", error);
			log.info("Searching backing file locally...");
			final var bfpath = this.findImagePathById(bfid);
			if (bfpath != null) {
				log.info("Found backing file locally at: " + bfpath);
				bfurl = bfpath.toString();
			}
			else {
				log.info("Backing file was not found locally!");
				log.info("Looking up backing file in new archive...");
				if (iaConfig.getName().equalsIgnoreCase("emulators")) {
					bfurl = DataResolvers.emulators()
							.resolve(bfid);
				}
				else {
					final var binding = new ImageArchiveBinding();
					binding.setImageId(bfid);
					bfurl = DataResolvers.images()
							.resolve(binding, null);
				}
			}

			bfinfo = new ImageInformation(bfurl, log);
		}

		return bfinfo.getFileFormat();
	}

	public String updateBackingFileUrl(Path image, ImageInformation info)
	{
		return this.updateBackingFileUrl(image.toFile(), info);
	}

	public String updateBackingFileUrl(File image)
	{
		return this.updateBackingFileUrl(image, null);
	}

	public String updateBackingFileUrl(File image, ImageInformation info)
	{
		try {
			log.info("Updating backing file for: " + image.getAbsolutePath());
			if (info == null)
				info = new ImageInformation(image.getAbsolutePath(), log);

			if (!info.hasBackingFile()) {
				log.info("No backing file defined!");
				return null;
			}

			String id = ImageInformation.getBackingImageId(info.getBackingFile());
			log.info("Image info: " + image.getAbsolutePath() + " --> " + info.getBackingFile() + " (ID = " + id + ")");

			if (id.equals(info.getBackingFile())) {
				log.info("Local backing file reference is up-to-date!");
			}
			else {
				var format = info.getBackingFileFormat();
				if (format == null)
					format = this.findBackingFileFormat(id, info.getBackingFile());

				log.info("Rebasing image: " + image.getAbsolutePath() + " --> " + id);
				EmulatorUtils.changeBackingFile(image.toPath(), id, format, log);
			}

			return id;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Updating backing file failed!", e);
			return null;
		}
	}

	private void resolveLocalBackingFiles()
	{
		for(ImageType type : ImageType.values()) {
			File dir = new File(iaConfig.getImagePath() + "/" + type.name());
			if(!dir.exists())
				continue;

			File[] files = dir.listFiles();
			if(files == null)
				continue;

			for (final File fileEntry : files) {
				if (fileEntry.isDirectory())
					continue;

					if (fileEntry.getName().startsWith(".fuse"))
					continue;

				this.updateBackingFileUrl(fileEntry);
				if (handleClient != null) {
					final String imgname = fileEntry.getName();
					try {
						this.createOrUpdateHandle(imgname);
					}
					catch (Exception error) {
						log.log(Level.WARNING, "Registering image '" + imgname + "' failed!", error);
					}
				}
			}
		}

		/*
		if(iaConfig.getName().equals("emulators"))
			createLocalEmulatorQcowFiles();

		 */
	}

	/*
	private void createLocalEmulatorQcow(String id) throws BWFLAException
	{
		File dir = new File(iaConfig.getImagePath() + "/base");
		File image = new File(dir, id);
		if(!image.exists())
			throw new BWFLAException("Emulator Image " + id + " not found");
		createLocalEmulatorQcow(image);
	}

	 */


	/*
	private void createLocalEmulatorQcow(File file) throws BWFLAException
	{
		File localCowDir = new File(iaConfig.getImagePath() + "/fakeqcow");
		if(!localCowDir.exists())
			localCowDir.mkdirs();

		String name = file.getName();
		QcowOptions options = new QcowOptions();
		options.setBackingFile(file.getAbsolutePath());
		File cow = new File(localCowDir, name);
		if(cow.exists())
			cow.delete();
		EmulatorUtils.createCowFile(cow.toPath(), options);
	}

	private void createLocalEmulatorQcowFiles()
	{
		File dir = new File(iaConfig.getImagePath() + "/base");
		if(!dir.exists())
			return;

		File[] files = dir.listFiles();
		if(files == null)
			return;

		for (final File fileEntry : files) {
			if (fileEntry.isDirectory())
				continue;

			if (fileEntry.getName().startsWith(".fuse"))
				continue;

			try {
				createLocalEmulatorQcow(fileEntry);
			} catch (BWFLAException e) {
				e.printStackTrace();
			}
		}
	}
*/
//	private List<ImageExport.ImageFileInfo> processBindingForExport(ImageArchiveBinding iab) throws BWFLAException, IOException {
//		List<ImageExport.ImageFileInfo> fileInfos = new ArrayList<>();
//
//		File target = getImageTargetPath(iab.getType());
//		if(target == null)
//			throw new BWFLAException("getImageExportData: inconsistent metadata: " + target.getAbsolutePath() + " type " + iab.getType());
//		File imageFile = new File(target, iab.getImageId());
//		if(!imageFile.exists())
//			throw new BWFLAException("getImageExportData: inconsistent metadata " + imageFile.getAbsolutePath() + " not found.");
//
//		ImageExport.ImageFileInfo info = new ImageExport.ImageFileInfo(getArchivePrefix(),
//				iab.getImageId(), ImageType.valueOf(iab.getType().toLowerCase()));
//
//		fileInfos.add(info);
//
//		ImageExport.ImageFileInfo parent = info;
//		while((parent = getDependency(parent.getType(), parent.getId())) != null)
//		{
//			fileInfos.add(parent);
//		}
//
//		return fileInfos;
//	}

//	ImageExport getImageExportData(String envId) throws BWFLAException, IOException {
//
//		Environment env = getEnvById(envId);
//		if(env == null)
//			return null;
//
//		MachineConfiguration mc = (MachineConfiguration) env;
//
//		ImageExport export = new ImageExport();
//		List<ImageExport.ImageFileInfo> fileInfos = new ArrayList<>();
//
//		ImageArchiveBinding iab = null;
//		for (AbstractDataResource b : mc.getAbstractDataResource()) {
//			if (b instanceof ImageArchiveBinding) {
//				iab = (ImageArchiveBinding) b;
//				fileInfos.addAll(processBindingForExport(iab));
//			}
//		}
//
//		export.setImageFiles(fileInfos);
//		return export;
//	}
	
	private static void deleteDirectory(File dir) throws IOException
	{
		if(!dir.exists())
			return;
		
		String[]entries = dir.list();
		for(String s: entries){
		    File currentFile = new File(dir.getPath(), s);
		    currentFile.delete();
		}
		Files.deleteIfExists(dir.toPath());
	}

	public void cleanTmpFiles()
	{
		File tmpEnvFile = new File(iaConfig.getMetaDataPath(), "tmp");
		try {
			deleteDirectory(tmpEnvFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		tmpEnvFile.mkdir();
		
		File tmpImgFile = new File(iaConfig.getImagePath(), "tmp");
		try {
			deleteDirectory(tmpImgFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		tmpImgFile.mkdir();
	
		ConcurrentHashMap<String, Environment> map = cache.get(ImageType.valueOf("tmp"));
		if(map == null)
			cache.put(ImageType.valueOf("tmp"), new ConcurrentHashMap<>());
		else
			map.clear();
	}
	
	protected File getImageTargetPath(String type) {
		if(type == null)
			return  null;
		
		File target = new File(iaConfig.getImagePath(), type);
		if (target.isDirectory())
			return target;
		else
			return null;
	}

	public File getMetaDataTargetPath(String type) {
		File target = new File(iaConfig.getMetaDataPath(), type);

		if (target.isDirectory())
			return target;
		else
			return null;
	}
	
	synchronized void removeCachedEnv(ImageType type, String id) throws BWFLAException
	{
		ConcurrentHashMap<String, Environment> map = cache.get(type);
		if(map == null)
			throw new BWFLAException("map for image type :" + type + " not found");
		
		map.remove(id);		
	}
	
	synchronized void addCachedEnvironment(ImageType type, String id, Environment env) throws BWFLAException
	{
		ConcurrentHashMap<String, Environment> map = cache.get(type);
		if(map == null)
		{
			throw new BWFLAException("map for image type : -" + type + "- not found");
		}
		map.put(id, env);		
	}

	@Deprecated
	String importImageUrl(URL url, ImageArchiveMetadata iaMd, boolean delete) throws BWFLAException, IOException {
		
		File target = getImageTargetPath(iaMd.getType().name());
		String importId;
		if(iaMd.getImageId() != null)
			importId = iaMd.getImageId();
		else
			importId = UUID.randomUUID().toString();

		File destImgFile = new File(target, importId);

		if (destImgFile.exists()) {
			if (!delete) {
				log.warning("the following file already exists, will not overwrite: " + destImgFile.getAbsolutePath());
			} else
				destImgFile.delete();
		}

		String uuid = UUID.randomUUID().toString();
		FutureTask<ImageLoaderResult> ft =  new FutureTask<ImageLoaderResult>(new ImageLoader(url, target, importId, this));
		importTasks.put(uuid, ft);
		pool.submit(ft);
		return uuid;
	}

	TaskState importImageUrlAsync(URL url, ImageArchiveMetadata iaMd, boolean delete) throws BWFLAException, IOException {

		File target = getImageTargetPath(iaMd.getType().name());
		String importId;
		if(iaMd.getImageId() != null)
			importId = iaMd.getImageId();
		else
			importId = UUID.randomUUID().toString();

		File destImgFile = new File(target, importId);

		if (destImgFile.exists()) {
			if (!delete) {
				log.warning("the following file already exists, will not overwrite: " + destImgFile.getAbsolutePath());
			} else
				destImgFile.delete();
		}

		return ImageArchiveRegistry.submitTask(new ImportImageTask(url, target, importId, this, log));
	}

	TaskState importImageStreamAsync(DataHandler image, ImageArchiveMetadata iaMd) throws BWFLAException {
		File target = getImageTargetPath(iaMd.getType().name());

		String importId;
		if(iaMd.getImageId() == null)
			importId = UUID.randomUUID().toString();
		else
			importId = iaMd.getImageId();

		File destImgFile = new File(target, importId);
		if (destImgFile.exists()) {
			if (!iaMd.isDeleteIfExists()) {
				log.warning("the following file already exists, will not overwrite: " + destImgFile.getAbsolutePath());
			} else
				destImgFile.delete();
		}

		try
		{
			InputStream inputStream = image.getInputStream();
			return ImageArchiveRegistry.submitTask(new ImportImageTask(inputStream, target, importId, this, log));
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new BWFLAException(" image getInputStream: " + e);
		}
	}

	@Deprecated
	String importImageStream(DataHandler image, ImageArchiveMetadata iaMd) throws BWFLAException {
		File target = getImageTargetPath(iaMd.getType().name());

		String importId;
		if(iaMd.getImageId() == null)
			importId = UUID.randomUUID().toString();
		else
			importId = iaMd.getImageId();

		File destImgFile = new File(target, importId);
		if (destImgFile.exists()) {
			if (!iaMd.isDeleteIfExists()) {
				log.warning("the following file already exists, will not overwrite: " + destImgFile.getAbsolutePath());
			} else
				destImgFile.delete();
		}

		String taskId = UUID.randomUUID().toString();
		try
		{
			InputStream inputStream = image.getInputStream();
			DataUtil.writeData(inputStream, destImgFile);
			this.updateBackingFileUrl(destImgFile);

			FutureTask<ImageLoaderResult> ft =  new FutureTask<ImageLoaderResult>(new ImageLoader(inputStream, target, importId, this));
			importTasks.put(taskId, ft);
			pool.submit(ft);
			return taskId;
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new BWFLAException(" image getInputStream: " + e);
		}
	}

	@Deprecated
	public ImageImportResult getImageImportResult(String session) throws BWFLAException {
		FutureTask<ImageLoaderResult> ft = importTasks.get(session);
		if(ft.isDone())
		{
			importTasks.remove(session);
			try {
				ImageLoaderResult res = ft.get();
				if(!res.success)
					throw new BWFLAException(res.message);
				return new ImageImportResult(this.getArchivePrefix(), res.id);
			} catch (InterruptedException|ExecutionException e) {
				throw new BWFLAException(e);
			}
		}
		return null;
	}

	boolean deleteImage(String imageId, String type)
	{
		File target = getImageTargetPath(type);
		File destImgFile = new File(target, imageId);
		if(!destImgFile.exists())
		{
			log.severe("image: " + destImgFile + " does not exist");
			return false;
		}
		return destImgFile.delete();
	}

	public boolean writeMetaData(String conf, String id, String type, boolean delete) {
		File metaDataDir = new File(iaConfig.getMetaDataPath(), type);
		if (!metaDataDir.isDirectory())
			return false;

		String confFullName = id + ".xml";

		File destConfFile = new File(metaDataDir + File.separator + confFullName);
		if (destConfFile.exists()) {
			if (!delete) {
				log.severe("the following file already exists, will not overwrite: " + destConfFile.getAbsolutePath());
				return false;
			} else
				destConfFile.delete();
		}
		return DataUtil.writeString(conf, destConfFile);
	}

	public boolean deleteMetaData(String id) throws BWFLAException
	{
		log.info("deleting: " + id);
		for(ImageType imageType : ImageType.values())
		{
			File path = getMetaDataTargetPath(imageType.toString());

			if (path == null || !path.exists() || !path.isDirectory())
				continue;

			for (final File fileEntry : path.listFiles()) {
				if (fileEntry.isDirectory())
					continue;
			
				String env = loadMetaDataFile(fileEntry);
				if(env == null)
					continue;
				
				Environment emuEnv = null;
				try {
					emuEnv = Environment.fromValue(env);
				} catch (Throwable t) {
					continue;
				}
				if(emuEnv.getId().equals(id))
				{
					boolean ret = fileEntry.delete();
					if(ret)
						removeCachedEnv(imageType, id);
					
					log.info("deleting envId success: " + ret);
					return ret;
				}
			}
		}
		return false;
	}

	public String loadMetaDataFile(File mf) {
		
		if(mf.getName().startsWith(".fuse"))
		{
			log.warning("found fuse hidden file, skipping");
			return null;
		}
		
		String env;
		try {
			env = FileUtils.readFileToString(mf, "UTF-8");
		} catch (IOException e) {
			log.info("failed loading " + mf + " - " + e.getMessage());
			return null;
		}

		return env;
	}

	protected final static String IA_URI_SCHEME = "imagearchive";

	private Environment migrateEnvironment(Environment env, ImageType t) {
		if (!(env instanceof MachineConfiguration))
			return env;

		MachineConfiguration conf = (MachineConfiguration) env;
		conf.setMetaDataVersion("1");

		ArrayList<AbstractDataResource> addList = new ArrayList<AbstractDataResource>();
		Iterator<AbstractDataResource> iter = conf.getAbstractDataResource().iterator();
		while (iter.hasNext()) {
			AbstractDataResource r = iter.next();
			
			if (!(r instanceof Binding))
				continue;

			Binding b = (Binding) r;
			URI uri = null;
			try {
				uri = new URI(b.getUrl());
			} catch (URISyntaxException e) {
				log.info("invalid uri: " + b.getUrl() + " " + e.getMessage());
				continue;
			}
	
			if (uri.isOpaque() && uri.getScheme().equalsIgnoreCase(IA_URI_SCHEME)) {
				ImageArchiveBinding iaBinding = new ImageArchiveBinding(iaConfig.getName(), uri.getSchemeSpecificPart(), t.toString());
				iaBinding.setId(b.getId());
				addList.add(iaBinding);
				iter.remove();	
			}
		}
		conf.getAbstractDataResource().addAll(addList);
	//	log.info(conf.toString());
		return conf;
	}

	public ConcurrentHashMap<String, Environment> loadMetaData(ImageArchiveMetadata.ImageType imageType) {

		File path = getMetaDataTargetPath(imageType.toString());
		// log.info("loading metadata for type: " + path + " " + imageType);
		ConcurrentHashMap<String, Environment> md = new ConcurrentHashMap<String, Environment>();
		if (path == null || !path.exists() || !path.isDirectory()) {
			log.info("path " + path + " not a valid meta-data directory");
			return md;
		}

		for (final File fileEntry : path.listFiles()) {
			if (fileEntry.isDirectory())
				continue;
			
			if(fileEntry.getName().startsWith(".fuse"))
				continue;
			
			// log.info("loading: " + fileEntry);

			String env;
			if ((env = loadMetaDataFile(fileEntry)) != null) {
				Environment emuEnv = null;
				try {
					emuEnv = Environment.fromValue(env);
				} catch (Throwable t) {
					log.info("loadMetadata: failed to parse environment: " + t.getMessage());
					log.log(Level.WARNING, t.getMessage(), t);
					continue;
				}
				if (emuEnv.getMetaDataVersion() == null) {
					emuEnv = migrateEnvironment(emuEnv, imageType);
					emuEnv.setMetaDataVersion("1");
					fileEntry.delete();
					writeMetaData(emuEnv.toString(), emuEnv.getId(), imageType.toString(), true);
				}
				md.put(emuEnv.getId(), emuEnv);
			}
		}
		return md;
	}

	protected Environment getEnvById(String id) {
		if(id == null)
			return null;
		for (ImageType t : ImageType.values()) {
			ConcurrentHashMap<String, Environment> map = cache.get(t);
			if (map == null)
				continue;
			// log.info("map " + t.toString() + " entries # " + map.size());
			// log.info(map.keySet().toString());
			Environment env = map.get(id);
			if (env != null)
			{
				return env;
			}
		}
		return null;
	}

	public ImageType getImageType(String id)
	{
		if(id == null)
			return null;
		for (ImageType t : ImageType.values()) {
			ConcurrentHashMap<String, Environment> map = cache.get(t);
			if (map == null)
				continue;

			if(map.get(id) != null)
				return t;
		}
		return null;
	}

	public ImageArchiveBinding getImageBinding(String name)
	{
		return this.getImageBinding(name, null);
	}

	public ImageArchiveBinding getImageBinding(String name, String version)
	{
		if (name == null || name.isEmpty())
			return null;

		final ImageMetadata entry = imageNameIndex.get(name, version);
		if (entry == null)
			return null;

		final ImageDescription image = entry.getImage();
		final ImageArchiveBinding binding = new ImageArchiveBinding();
		binding.setBackendName(iaConfig.getName());
		binding.setAccess(Binding.AccessType.COW);
		binding.setFileSystemType(image.fstype());
		binding.setType(image.type());
		binding.setImageId(image.id());
		binding.setUrl("");
		return binding;
	}

	private MachineConfiguration getEnvByImageId(ImageType t, String id)
	{
		ConcurrentHashMap<String, Environment> map = cache.get(t);
		for(String key : map.keySet()) {
			Environment env = map.get(key);
			if (!(env instanceof MachineConfiguration))
				continue;

			MachineConfiguration mc = (MachineConfiguration) env;

			for (AbstractDataResource b : mc.getAbstractDataResource()) {
				if (b instanceof ImageArchiveBinding && b.getId().equals("main_hdd")) {
					ImageArchiveBinding iab = (ImageArchiveBinding) b;
					if (iab.getImageId().equals(id))
						return mc;
				}
			}
		}
		return null;
	}

	public ConcurrentHashMap<String, Environment> getImages(String type) {
		try {
			ImageType t = ImageType.valueOf(type);
			File dir = new File(iaConfig.getMetaDataPath(), t.name());
			if (!dir.exists()) {
				log.info("dir not found: " + dir);
				return new ConcurrentHashMap<String, Environment>();
			}
			return loadMetaData(t);
		} catch (IllegalArgumentException e) {
			log.warning("unknown ImageType " + e.getMessage());
			return new ConcurrentHashMap<String, Environment>();
		}
	}

	public EmulatorMetadata extractMetadata(String imageId) throws BWFLAException {
		File srcDir = getImageTargetPath("base");
		File imgFile = new File(srcDir, imageId);
		if(!imgFile.exists())
			throw new BWFLAException("can't find emulator image " + imgFile);

		try (final ImageMounter mounter = new ImageMounter(log)) {
			final Path workdir = ImageMounter.createWorkingDirectory();
			mounter.addWorkingDirectory(workdir);

			ImageMounter.Mount rawmnt = mounter.mount(imgFile.toPath(), workdir.resolve(imgFile.toPath().getFileName() + ".dd"));
			ImageMounter.Mount fsmnt = mounter.mount(rawmnt, workdir.resolve("fs"), FileSystemType.EXT4);
			log.info("after fsmount");
			Path fsDir = fsmnt.getMountPoint();
			if (!Files.exists(fsDir)) {
				throw new BWFLAException("can't find filesystem - not a valid emulator image.");
			}

			Path metadata = fsDir.resolve("metadata");
			if (!Files.exists(metadata)) {
				log.severe("no metadata directory found");
				return null;
			}

			try (final var archive = ImageArchiveClient.create()) {
				this.copyEnvironments(metadata.resolve("environments"), archive);
				this.copyTemplates(metadata.resolve("templates"), archive);
			}
			catch (Exception error) {
				log.log(Level.WARNING, "Importing emulator's metadata failed!", error);
			}

			EmulatorMetadata md;
			Path metadataFilePath = metadata.resolve("metadata.json");
			try {
				md = EmulatorMetadata.fromJsonValueWithoutRoot(new String(Files.readAllBytes(metadataFilePath)), EmulatorMetadata.class);
			} catch (IOException e) {
				log.info("metadata ref not available");
				return null;
			}

			if (md.getContainerDigest() == null) {
				Path hashRefPath = metadata.resolve("repo-digest");
				try {
					md.setContainerDigest(new String(Files.readAllBytes(hashRefPath)));
				} catch (IOException e) {
					log.info("container ref not available");
					return null;
				}
			}

			log.info("completing unmount");
			return md;
		}
	}

	private void copyEnvironments(Path srcdir, ImageArchiveClient archive) throws IOException
	{
		if (!Files.exists(srcdir))
			return;  // nothing to copy!

		final var machines = archive.api()
				.v2()
				.machines();

		final Consumer<Path> uploader = (path) -> {
			if (Files.isDirectory(path))
				return;

			try (final var input = Files.newInputStream(path)) {
				final var machine = JaxbType.from(input, MachineConfiguration.class);
				machines.replace(machine.getId(), machine);
				log.info("Imported machine '" + machine.getId() + "'");
			}
			catch (Exception error) {
				log.log(Level.WARNING, "Importing machine failed!", error);
			}
		};

		try (final var files = Files.list(srcdir)) {
			files.forEach(uploader);
		}
	}

	private void copyTemplates(Path srcdir, ImageArchiveClient archive) throws IOException
	{
		if (!Files.exists(srcdir))
			return;  // nothing to copy!

		final var templates = archive.api()
				.v2()
				.templates();

		final Consumer<Path> uploader = (path) -> {
			if (Files.isDirectory(path))
				return;

			try (final var input = Files.newInputStream(path)) {
				final var template = JaxbType.from(input, MachineConfigurationTemplate.class);
				templates.replace(template.getId(), template);
				log.info("Imported machine-template '" + template.getId() + "'");
			}
			catch (Exception error) {
				log.log(Level.WARNING, "Importing machine-template failed!", error);
			}
		};

		try (final var files = Files.list(srcdir)) {
			files.forEach(uploader);
		}
	}


	/**
	 * Asynchronously replicates specified images by importing them into this image archive.
	 * @param images A list of source URLs for images to import.
	 * @return A list of import-task IDs, one for each image to import.
	 * @see #getImageImportResult(String)
	 */
	@Deprecated
	public List<String> replicateImages(List<String> images) {
		final List<String> taskids = new ArrayList<String>(images.size());
		images.forEach((urlstr) -> {
			try {
				log.severe("replicating " + urlstr);
				final URL url = new URL(urlstr);
				final String urlpath = url.getPath();
				final String imageid = urlpath.substring(urlpath.lastIndexOf("/") + 1);
				final ImageArchiveMetadata metadata = new ImageArchiveMetadata(ImageType.base);
				metadata.setDeleteIfExists(true);
				metadata.setImageId(imageid);
				taskids.add(this.importImageUrl(url, metadata, false));
			}
			catch (Exception error) {
				log.log(Level.WARNING, "Preparing image-import from URL failed!", error);
			}
		});

		return taskids;
	}

	/**
	 * Asynchronously replicates specified images by importing them into this image archive.
	 * @param images A list of source URLs for images to import.
	 * @return A list of import-task IDs, one for each image to import.
	 * @see ImageArchiveWS#getTaskState(String)
	 */
	public List<TaskState> replicateImagesAsync(List<String> images) {
		final List<TaskState> taskids = new ArrayList<>(images.size());
		images.forEach((urlstr) -> {
			try {
				log.severe("replicating " + urlstr);
				final URL url = new URL(urlstr);
				final String urlpath = url.getPath();
				final String imageid = urlpath.substring(urlpath.lastIndexOf("/") + 1);
				final ImageArchiveMetadata metadata = new ImageArchiveMetadata(ImageType.base);
				metadata.setDeleteIfExists(true);
				metadata.setImageId(imageid);
				taskids.add(this.importImageUrlAsync(url, metadata, false));
			}
			catch (Exception error) {
				log.log(Level.WARNING, "Preparing image-import from URL failed!", error);
			}
		});

		return taskids;
	}

	public void createOrUpdateHandle(String imageId) throws BWFLAException
	{
		if(handleClient == null)
			return;

		final String url = this.getArchivePrefix() + imageId;
		try {
			log.info("Trying to create new handle for image '" + imageId + "'...");
			handleClient.create(imageId, url);
			log.info("New handle for image '" + imageId + "' created");
			return;
		}
		catch (HandleException error) {
			if (error.getErrorCode() != HandleException.ErrorCode.HANDLE_ALREADY_EXISTS) {
				log.log(Level.WARNING, "Creating new handle for image '" + imageId + "' failed! ", error);
				throw error;
			}
		}

		try {
			log.info("Handle already exists! Trying to update previous URL for image '" + imageId + "'...");
			handleClient.update(imageId, url);
			log.info("URL updated in existing handle for image '" + imageId + "'");
			return;
		}
		catch (HandleException error) {
			if (error.getErrorCode() != HandleException.ErrorCode.INVALID_VALUE) {
				log.log(Level.WARNING, "Updating URL in exisiting handle for image '" + imageId + "' failed!", error);
				throw error;
			}
		}

		log.info("Trying to add new URL for image '" + imageId + "'...");
		handleClient.add(imageId, url);
		log.info("URL added to existing handle for image '" + imageId + "'");
	}

	public String getHandleUrl(String id)
	{
		if(handleClient == null)
			return null;
		return "http://hdl.handle.net/" + handleClient.toHandle(id);
	}

	private static class ImageLoaderResult
	{
		boolean success;
		String message;
		String id;

		ImageLoaderResult(String id)
		{
			this.success = true;
			this.message = "success";
			this.id = id;
		}

		ImageLoaderResult(boolean success, String message)
		{
			this.success = success;
			this.message = message;
		}
	}

	private static class ImageLoader implements Callable<ImageLoaderResult>
	{
		private File destImgFile;
		private URL url;
		private File target;
		private String importId;
		private ImportType type;
		private InputStream inputStream;
		private ImageType imageType = ImageType.user;

		private final ImageHandler imageHandler;
		private final Logger log;

		private enum ImportType {
			URL,
			STREAM
		};

		public ImageLoader(InputStream inputStream, File target, String importId, ImageHandler imageHandler)  {

			this.imageHandler = imageHandler;
			this.inputStream = inputStream;
			this.log = imageHandler.log;

			this.importId = importId;
			this.target = target;
			destImgFile = new File(target, importId);

			type = ImportType.STREAM;
		}

		public ImageLoader(URL url, File target, String importId, ImageHandler imageHandler)
		{
			type = ImportType.URL;

			this.url = url;
			this.imageHandler = imageHandler;
			this.log = imageHandler.log;
			this.target = target;
			this.importId = importId;
			destImgFile = new File(target, importId);
		}

		public void setImageType(ImageType type)
		{
			this.imageType = type;
		}

		private ImageLoaderResult fromStream()
		{
			if (imageHandler.handleClient != null) {
				try {
					imageHandler.createOrUpdateHandle(importId);
				}
				catch (BWFLAException error) {
					return new ImageLoaderResult(false, error.getMessage());
				}
			}

			return new ImageLoaderResult(importId);
		}

		private void downloadDependencies(String url) throws BWFLAException {
			final String imageid = url.substring(url.lastIndexOf("/") + 1);
			Binding b = new Binding();
			b.setUrl(url.toString());
			File dst = new File(target, imageid);

			if(dst.exists()) {
				log.warning("downloading dependencies: skip " + dst.getAbsolutePath());
			}
			else {
				try {
					EmulatorUtils.copyRemoteUrl(b, dst.toPath(), null);
				} catch (BWFLAException e) {
					b.setUrl("http://hdl.handle.net/" + imageHandler.handleClient.toHandle(imageid));
					log.severe("retrying handle... " + b.getUrl());
					EmulatorUtils.copyRemoteUrl(b, dst.toPath(), null);
				}
			}
			String result = imageHandler.updateBackingFileUrl(dst);
			if (imageHandler.handleClient != null)
				imageHandler.createOrUpdateHandle(imageid);

			if(result != null)
				downloadDependencies(result);
		}

		private ImageLoaderResult fromUrl()
		{
			try {
				Binding b = new Binding();
				b.setUrl(url.toString());

				// XmountOptions options = new XmountOptions();
				// EmulatorUtils.copyRemoteUrl(b, destImgFile.toPath(), options);

				if(!destImgFile.exists()) {
					EmulatorUtils.copyRemoteUrl(b, destImgFile.toPath());
				}
				ImageInformation info = new ImageInformation(destImgFile.toPath().toString(), log);
				ImageInformation.QemuImageFormat fmt = info.getFileFormat();
				if (fmt == null) {
					throw new BWFLAException("could not determine file fmt");
				}
				if(imageType.equals(ImageType.user))
					return new ImageLoaderResult(importId);

				switch (fmt) {
					case VMDK:
					case VHD:
					case VHDX:
						File convertedImgFile = new File(target, UUID.randomUUID().toString());
						destImgFile.renameTo(convertedImgFile);
						File outFile = new File(target, importId);
						EmulatorUtils.convertImage(convertedImgFile.toPath(), outFile.toPath(), ImageInformation.QemuImageFormat.QCOW2, log);
						convertedImgFile.delete();
					default:
						String result = imageHandler.updateBackingFileUrl(destImgFile);
						if (imageHandler.handleClient != null)
							imageHandler.createOrUpdateHandle(importId);

						if(result != null)
							downloadDependencies(result);

						return new ImageLoaderResult(importId);
				}
			} catch (Exception e1) {
				log.log(Level.WARNING, e1.getMessage(), e1);
				return new ImageLoaderResult(false, "failed moving incoming image to " + destImgFile + " reason " + e1.getMessage());
			}
		}

		@Override
		public ImageLoaderResult call() {
			switch(type) {
				case URL:
					return fromUrl();
				case STREAM:
					return fromStream();
				default:
					return new ImageLoaderResult(false, "");
			}
		}
	}

	private static class ImageLock
	{
		private int counter;


		public ImageLock()
		{
			this.counter = 0;
		}

		public synchronized void acquire()
		{
			++counter;
			while (counter > 1)
				this.await();
		}

		public synchronized int release()
		{
			--counter;
			this.notify();
			return counter;
		}

		private void await()
		{
			try {
				this.wait();
			}
			catch (Exception error) {
				// Ignore it!
			}
		}
	}
}

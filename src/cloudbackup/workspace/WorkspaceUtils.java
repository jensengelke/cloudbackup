package cloudbackup.workspace;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;

import cloudbackup.config.CloudBackupBackupOptions;
import cloudbackup.exception.CloudBackupException;
import cloudbackup.exception.FileSystemAccessException;
import cloudbackup.model.LocalFile;
import cloudbackup.model.RemoteFile;

public class WorkspaceUtils {

	private static final String sourceClass = WorkspaceUtils.class.getName();
	private static final Logger log = Logger.getLogger(sourceClass);

	private static final String LOCAL_FILES_CACHE_FILENAME = "localFiles.json";
	private static final String REMOTE_FILES_CACHE_FILENAME = "remoteFiles.json";

	public static File getOrCreateWorkspace() throws CloudBackupException {
		final String sourceMethod = "getOrCreateWorkspace";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		String backupName = CloudBackupBackupOptions.getInstance().getBackupName();
		File workspaceDir = FileUtils.getFile("workspaces/" + backupName);

		if (!workspaceDir.exists() && !workspaceDir.mkdirs()) {
			throw new FileSystemAccessException("Unable to create parent directory");
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, workspaceDir);
		}
		return workspaceDir;
	}

	public static Map<String, RemoteFile> getRemoteFiles() throws CloudBackupException {
		final String sourceMethod = "getRemoteFiles";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}

		Map<String, RemoteFile> fileMap = null;
		File workspaceDir = getOrCreateWorkspace();
		File remoteFilesCache = FileUtils.getFile(workspaceDir, REMOTE_FILES_CACHE_FILENAME);

		if (null != remoteFilesCache && remoteFilesCache.exists()) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				fileMap = new HashMap<String, RemoteFile>();
				RemoteFileCache cache = mapper.readValue(remoteFilesCache, RemoteFileCache.class);
				fileMap = cache.getRemoteFiles();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, fileMap);
		}
		return fileMap;
	}

	public static Map<String, LocalFile> getLocalFiles() throws CloudBackupException {
		final String sourceMethod = "getLocalFiles";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		Map<String, LocalFile> fileMap = new HashMap<String, LocalFile>();
		File workspaceDir = getOrCreateWorkspace();
		File localFilesCache = FileUtils.getFile(workspaceDir, LOCAL_FILES_CACHE_FILENAME);

		if (null != localFilesCache && localFilesCache.exists()) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				LocalFileCache cache = mapper.readValue(localFilesCache, LocalFileCache.class);
				fileMap = cache.getLocalFiles();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, fileMap);
		}
		return fileMap;
	}

	public static void writeLocalFiles(Map<String, LocalFile> cachedLocalFiles) throws CloudBackupException {
		final String sourceMethod = "writeLocalFiles";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { cachedLocalFiles });
		}
		File workspaceDir = getOrCreateWorkspace();
		File localFilesCache = FileUtils.getFile(workspaceDir, LOCAL_FILES_CACHE_FILENAME);

		if (null != localFilesCache && !localFilesCache.exists()) {
			try {
				localFilesCache.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			LocalFileCache cache = new LocalFileCache();
			cache.setLocalFiles(cachedLocalFiles);
			mapper.writeValue(localFilesCache, cache);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}

	public static void writeRemoteFiles(Map<String, RemoteFile> cachedRemoteFiles) throws CloudBackupException {
		final String sourceMethod = "writeRemoteFiles";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { cachedRemoteFiles });
		}
		File workspaceDir = getOrCreateWorkspace();
		File remoteFilesCache = FileUtils.getFile(workspaceDir, REMOTE_FILES_CACHE_FILENAME);
		if (null != remoteFilesCache && !remoteFilesCache.exists()) {
			try {
				remoteFilesCache.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			RemoteFileCache cache = new RemoteFileCache();
			cache.setRemoteFiles(cachedRemoteFiles);
			mapper.writeValue(remoteFilesCache, cache);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}

	}

	public static void updateRemoteFilesCache(Map<String, RemoteFile> remoteFileUpdates) throws CloudBackupException {
		final String sourceMethod = "writeRemoteFiles";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { remoteFileUpdates });
		}
		
		Map<String, RemoteFile> currentCache = getRemoteFiles(); 
		
		if (null == currentCache) {
			writeRemoteFiles(remoteFileUpdates);
		} else {
			currentCache.putAll(remoteFileUpdates);
			writeRemoteFiles(currentCache);
		}
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}

	public static void clean() throws CloudBackupException {
		final String sourceMethod = "clean";

		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}

		try {
			File workspaceDir = getOrCreateWorkspace();
			FileUtils.deleteDirectory(workspaceDir);
		} catch (FileSystemAccessException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}

	public static void cleanLocalFileCache() throws CloudBackupException {
		final String sourceMethod = "cleanLocalFileCache";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}

		try {
			File workspaceDir = getOrCreateWorkspace();
			FileUtils.forceDelete(FileUtils.getFile(workspaceDir, LOCAL_FILES_CACHE_FILENAME));
		} catch (java.io.FileNotFoundException fne) {
			//continue if there is nothing to delete
		} catch (FileSystemAccessException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}

}

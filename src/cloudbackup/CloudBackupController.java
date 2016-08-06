package cloudbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import cloudbackup.config.CloudBackupBackupOptions;
import cloudbackup.config.CloudBackupRunOptions;
import cloudbackup.config.CloudBackupSystemOptions;
import cloudbackup.datapipe.DataPipe;
import cloudbackup.datapipe.DataPipeContext;
import cloudbackup.exception.CloudBackupException;
import cloudbackup.exception.FileSystemAccessException;
import cloudbackup.local.LocalFileSystemConnector;
import cloudbackup.model.LocalFile;
import cloudbackup.model.RemoteFile;
import cloudbackup.remote.googledrive.GoogleDriveConnector;
import cloudbackup.workspace.WorkspaceUtils;

public class CloudBackupController {
	
	private static final String sourceClass = CloudBackupController.class.getName();
	private static Logger log;	


	public static void main(String[] args) {
		CloudBackupRunOptions runOptions = null;
		CloudBackupBackupOptions backupOptions = null;
		CloudBackupSystemOptions systemOptions = null;
		
		if (null != args[0]) {
			runOptions = CloudBackupRunOptions.getInstance(); 
			runOptions.setMode(args[0]);
		}
		
		try {
			if (null != args[1]) {
				backupOptions = CloudBackupBackupOptions.initialize(args[1]);
			} else {
				backupOptions = CloudBackupBackupOptions.initialize("config/defaultBackup.json");
			}			
			if (null != args[2]) {
				systemOptions = CloudBackupSystemOptions.initialize(args[2]);
			} else {
				systemOptions = CloudBackupSystemOptions.initialize("config/cloudBackup.json");
			}
		} catch (CloudBackupException e) {
			System.err.println(e.getLocalizedMessage());
		}
		
		if (null == runOptions || null == backupOptions || null == systemOptions) {
			System.err.println("Cannot start ...");
			printUsage();
			System.exit(0);
		}
		
		try {
			String loogingPropertiesFile = CloudBackupSystemOptions.getInstance().getLoggingProperties();
			if (null!=loogingPropertiesFile) {
				final InputStream inputStream = new FileInputStream(new File(loogingPropertiesFile));
			    LogManager.getLogManager().readConfiguration(inputStream);
			    log = Logger.getLogger(sourceClass);
			} else {
				System.out.println("no logging ... silent mode ;-)");
			}
			
		} catch (final IOException | CloudBackupException e) {
		    Logger.getAnonymousLogger().severe("Could not load default logging.properties file");
		    Logger.getAnonymousLogger().severe(e.getMessage());
		}
		
		if (log.isLoggable(Level.FINE))  {
			try {
				log.fine("system config: " + CloudBackupSystemOptions.getInstance().toString());
				log.fine("backup config: " + CloudBackupBackupOptions.getInstance().toString());
				log.fine("run options  : " + runOptions.toString());
			} catch (CloudBackupException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		
		Thread preventSleepMode = new Thread(new WinUtils());
		preventSleepMode.start();
		
		if ("indexLocalFileSystem".equals(runOptions.getMode())) {
			try {
				indexLocalFileSystem();
			} catch (CloudBackupException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if ("clean".equals(runOptions.getMode())) {
			try {
				WorkspaceUtils.clean();
			} catch (CloudBackupException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if ("reindexLocalFileSystem".equals(runOptions.getMode())) {
			reindexLocalFileSystem();
		}
		
		//build local cache from google drive
		if ("buildLocalCacheFromBackup".equals(runOptions.getMode())) {
			try {
				buildLocalCacheFromBackup();
			} catch (CloudBackupException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		//build local cache from google drive dry run 
		
		//backup
		if ("backup".equals(runOptions.getMode())) {
			try {
				backup();
			} catch (CloudBackupException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//restore latest
		if ("restore".equals(runOptions.getMode())) {
			try {
				restore();
			} catch (CloudBackupException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		CloudBackupRunOptions.getInstance().setRunning(false);
	
	}
	
	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("CloudBackupController <mode> [path to backup config] [path to system config]");
	}
	
	
	/*
	 * initial indexing
	 */
	private static void indexLocalFileSystem() throws CloudBackupException {
		try {
			Map<String, LocalFile> cachedFileSet = WorkspaceUtils.getLocalFiles();
			
			if (null != cachedFileSet && !cachedFileSet.isEmpty()) {
				System.out.println("There is an existing local file cache in workspace " + WorkspaceUtils.getOrCreateWorkspace());
				System.out.println("Use 'clean' mode to remove workspace cache files.");
				System.out.println("Use 'reindexLocalFileSystem' to overwrite local file cache only");
				System.exit(0);
			}
							
			Set<LocalFile> fileSet = LocalFileSystemConnector.getInstance().indexLocalFileSystem(CloudBackupBackupOptions.getInstance().getBackupDir(), false);
			
			Map<String, LocalFile> fileMap = new HashMap<>(fileSet.size());
			
			for (LocalFile localFile : fileSet) {
				fileMap.put(localFile.getFileId(), localFile);
			}
			WorkspaceUtils.writeLocalFiles(fileMap);				
		} catch (FileSystemAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private static void buildLocalCacheFromBackup() throws CloudBackupException {
		
		Set<RemoteFile> remoteFileSet = GoogleDriveConnector.getInstance().getAllDriveFilesForBackup(CloudBackupBackupOptions.getInstance().getGoogleDriveBackupDirectory());
		Map<String, RemoteFile> remoteFileMap = new HashMap<>(remoteFileSet.size());
		
		for (RemoteFile remoteFile : remoteFileSet) {
			remoteFileMap.put(remoteFile.getBackupId(), remoteFile);
		}
		WorkspaceUtils.writeRemoteFiles(remoteFileMap);
	}
	
	private static void backup() throws CloudBackupException {
		Map<String, RemoteFile> remoteFiles = WorkspaceUtils.getRemoteFiles();
		Map<String, RemoteFile> remoteFilesUpdates = new HashMap<String, RemoteFile>();
		
		if (null == remoteFiles) {
			buildLocalCacheFromBackup();
			remoteFiles = WorkspaceUtils.getRemoteFiles();
		}
		if (null == remoteFiles) {
			remoteFiles = new HashMap<String, RemoteFile>();
		}
		reindexLocalFileSystem();
		Map<String, LocalFile> localFiles = WorkspaceUtils.getLocalFiles();
		
		Set<String> backupIdsFoundInThisBackup = new HashSet<String>();
		
		
		Iterator<String> localFileIterator = localFiles.keySet().iterator();
		
		PathMatcher fileInlcuder = FileSystems.getDefault().getPathMatcher("glob:" + CloudBackupBackupOptions.getInstance().getBackupDir() + "/" + CloudBackupBackupOptions.getInstance().getIncludeFiles());
		PathMatcher fileExlcuder = FileSystems.getDefault().getPathMatcher("glob:" + CloudBackupBackupOptions.getInstance().getBackupDir() + "/" + CloudBackupBackupOptions.getInstance().getExcludeFiles());
		
		ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 15, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(localFiles.size()));
		Collection<Future<DataPipeContext>> futures = new LinkedList<Future<DataPipeContext>>();
		
		while (localFileIterator.hasNext()) {
			String backupId = localFileIterator.next();
			backupIdsFoundInThisBackup.add(backupId);	
			
			LocalFile localFile = localFiles.get(backupId);
			
			Path filePath = Paths.get(CloudBackupBackupOptions.getInstance().getBackupDir() + File.separator + localFile.getRelativePath());
			if (!fileExlcuder.matches(filePath) ||
					fileInlcuder.matches(filePath)) {
				if (remoteFiles.keySet().contains(backupId)) {
					
					
					RemoteFile remoteFile = remoteFiles.get(backupId);
					
					if (localFile.getVersions().get(0).getModifiedTimestamp() != remoteFile.getLastRevisionTimestamp()) {
						
						log.info("local file " + localFile.getRelativePath() + " has changed: modified timestamp does not match backup");
						//update
						DataPipeContext context = new DataPipeContext();
						context.setLocalFile(localFile);
						context.setRemoteFile(remoteFile);
						context.setProps(GoogleDriveConnector.getInstance().getPropertiesFromFile(remoteFile.getFileId()));
						
						
						DataPipe pipe = new DataPipe(CloudBackupBackupOptions.getInstance().getBackupPipe(), context); 
//						pipe.run();
						futures.add(executor.submit(pipe));
						
						
					}
					
				} else {
					DataPipeContext context = new DataPipeContext();
					context.setLocalFile(localFile);
					
					DataPipe pipe = new DataPipe(CloudBackupBackupOptions.getInstance().getBackupPipe(), context); 
//					pipe.run();
					futures.add(executor.submit(pipe));
					
				}
			} else {
				log.info("file skipped due to filter configuration: " + filePath);
			}
			
		}
		
		
		executor.shutdown();
		
		for (Future<DataPipeContext> future: futures) {
			DataPipeContext context;
			try {
				context = future.get();
				remoteFilesUpdates.put(context.getRemoteFile().getBackupId(), context.getRemoteFile());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
		
		//update files that are deleted on local system
		
		Set<String> remoteFilesBackupIds = remoteFiles.keySet();
		for (String remoteFileBackupId : remoteFilesBackupIds) {
			if (!backupIdsFoundInThisBackup.contains(remoteFileBackupId)) {
				// a file that exists in the backup does not exist on the local file system
				if (!remoteFiles.get(remoteFileBackupId).isDeleted()) {
					// the file has not been marked deleted previously
					
					RemoteFile remoteFile = remoteFiles.get(remoteFileBackupId);
					remoteFile.setDeleted(true);
					remoteFile.setDeletionTimestamp(CloudBackupRunOptions.getInstance().getBackupTimestamp());
					
					try {
						if (log.isLoggable(Level.FINE)) {
							log.fine("marking file as deleted: " + remoteFile);
						}
						GoogleDriveConnector.getInstance().markDeleted(remoteFile);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					remoteFilesUpdates.put(remoteFileBackupId, remoteFile);
				}
			}
		}
		
		WorkspaceUtils.updateRemoteFilesCache(remoteFilesUpdates);		
	}
	
	private static void reindexLocalFileSystem() {
		final String sourceMethod = "reindexLocalFileSystem";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		try {
			WorkspaceUtils.cleanLocalFileCache();
			indexLocalFileSystem();
		} catch (CloudBackupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);;
		}
	}
	
	private static void restore() throws CloudBackupException{
		final String sourceMethod = "restore";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		
		String restoreDirName = CloudBackupBackupOptions.getInstance().getRestoreDir();
		File restoreDir = FileUtils.getFile(restoreDirName);

		if (!restoreDir.exists() && !restoreDir.mkdirs()) {
			throw new FileSystemAccessException("Unable to create parent directory");
		}
		
		Map<String, RemoteFile> remoteFiles = WorkspaceUtils.getRemoteFiles();
		if (null == remoteFiles) {
			buildLocalCacheFromBackup();
			remoteFiles = WorkspaceUtils.getRemoteFiles();
		}
		
		if (null == remoteFiles) {
			throw new CloudBackupException("No Files in backup on Drive...");
		}

		ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 15, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
		Set<String> keys = remoteFiles.keySet();
		for (String key : keys) {
			boolean skip = false;
			RemoteFile remoteFile = remoteFiles.get(key);
			if (remoteFile.isDeleted()) {
				if (remoteFile.getDeletionTimestamp() < CloudBackupBackupOptions.getInstance().getRestoreTimestamp()) {
					log.info("Skipping file that was deleted prior to restore timestamp: " + remoteFile.toString());
					skip = true;				
				}
			}
			
			if (!skip) {
				DataPipeContext context = new DataPipeContext();
				context.setRemoteFile(remoteFile);
				context.setProps(GoogleDriveConnector.getInstance().getPropertiesFromFile(remoteFile.getFileId()));
				DataPipe pipe = new DataPipe(CloudBackupBackupOptions.getInstance().getRestorePipe(), context);
				executor.submit(pipe);
				
			}
		}
		executor.shutdown();
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);;
		}
	}
	
	
}

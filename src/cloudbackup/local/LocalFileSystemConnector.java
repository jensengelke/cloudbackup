package cloudbackup.local;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import cloudbackup.exception.CloudBackupException;
import cloudbackup.exception.FileSystemAccessException;
import cloudbackup.exception.LocalFileOrDirNotFoundException;
import cloudbackup.model.LocalFile;
import cloudbackup.model.LocalFileVersion;
import cloudbackup.utils.CryptoUtils;

public class LocalFileSystemConnector {
	
	private static final LocalFileSystemConnector INSTANCE = new LocalFileSystemConnector();
	
	private static final String sourceClass = LocalFileSystemConnector.class.getName();
	private static final Logger log = Logger.getLogger(sourceClass);	
	private LocalFileSystemConnector(){
		
	}
	
	public static LocalFileSystemConnector getInstance() {
		return INSTANCE;
	}
	
	public Set<LocalFile> indexLocalFileSystem(String backupPath, boolean includeContentHashes) throws FileSystemAccessException {
		final String sourceMethod = "indexLocalFileSystem"; 
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] {backupPath, includeContentHashes});
		}

		Set<LocalFile> fileSet = new HashSet<LocalFile>();
		File backupDir = new File(backupPath);
		
		if (!backupDir.exists()) {
			throw new LocalFileOrDirNotFoundException("Backup path does not point to an existing directory: " + backupPath);
		}
		
		if (!backupDir.isDirectory()) {
			throw new FileSystemAccessException("Backup path points to a file, not a directory: " + backupPath);
		}		
		Iterator<File> fileIt = FileUtils.iterateFiles(backupDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		
		
		while (fileIt.hasNext()) {
			File currentFile = fileIt.next();
			
			
				LocalFile localFile = new LocalFile(getRelativePath(currentFile, backupDir));
				LocalFileVersion currentVersion = new LocalFileVersion(currentFile.length(), currentFile.lastModified());
				if (includeContentHashes) {
					try {
						currentVersion.setContentHash(CryptoUtils.hashFile(currentFile));
					} catch (CloudBackupException e) {
						//continue without content hash
						log.severe("Cannot calculate file content hash for " + currentFile.getAbsolutePath());
						e.printStackTrace();
					}
				}				
				localFile.addVersion(currentVersion);		
				fileSet.add(localFile);
		}		
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, fileSet.size());
		}
		return fileSet;
	}

	
	private String getRelativePath(File fullFileName, File rootDir) {
		final String sourceMethod = "getRelativePath"; 
		if (log.isLoggable(Level.FINER)) {
			log.entering(sourceClass, sourceMethod, new Object[] {fullFileName.getAbsolutePath(), rootDir.getAbsolutePath()});
		}
		
		String rootPath = rootDir.getAbsolutePath();
		String filePath = fullFileName.getAbsolutePath();
		
		String relativePath = filePath.substring(rootPath.length() + 1);

		if (log.isLoggable(Level.FINER)) {
			log.exiting(sourceClass, sourceMethod, relativePath);
		}
		
		return relativePath; 
	}
		
}

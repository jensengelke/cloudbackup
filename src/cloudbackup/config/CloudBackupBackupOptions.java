package cloudbackup.config;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import cloudbackup.exception.CloudBackupConfigException;
import cloudbackup.exception.CloudBackupException;
import cloudbackup.exception.FileSystemAccessException;

public class CloudBackupBackupOptions {
	
	private static CloudBackupBackupOptions INSTANCE = null;
	private static final String sourceClass = CloudBackupBackupOptions.class.getName();
	private static final Logger log = Logger.getLogger(sourceClass);	
	
	private String backupName;
	private String backupDirectory;
	private String restoreDirectory;
	private String googleDriveBackupDirectory;
	private String includeFiles;
	private String excludeFiles;
	private long restoreTimestamp;
	private DataPipeConfig backupPipe;
	private DataPipeConfig restorePipe;
	
	private static boolean initialized = false;
	
	public DataPipeConfig getBackupPipe() {
		return backupPipe;
	}

	public DataPipeConfig getRestorePipe() {
		return restorePipe;
	}
	
	public String getBackupName() {
		return backupName;
	}

	public String getBackupDir() {
		return backupDirectory;
	}

	public String getRestoreDir() {
		return restoreDirectory;
	}

	public String getGoogleDriveBackupDirectory() {
		return googleDriveBackupDirectory;
	}

	public String getIncludeFiles() {
		return includeFiles;
	}

	public String getExcludeFiles() {
		return excludeFiles;
	}

	public long getRestoreTimestamp() {
		return restoreTimestamp;
	}

	private CloudBackupBackupOptions() {
	}
	
	public static CloudBackupBackupOptions getInstance() throws CloudBackupException {
		if (!initialized) {
			throw new CloudBackupException("Configuration is not yet initialized");
		} 
		return INSTANCE;
	}
	
	public static CloudBackupBackupOptions initialize(String configFilePath) throws CloudBackupException {
		final String sourceMethod = "initialize"; 
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[]{configFilePath});
		}
		
		File configFile = new File(configFilePath);
		if (!configFile.exists() || configFile.isDirectory()) {
			throw new FileSystemAccessException("Cannot access config file: " + configFilePath);
		}
		
		ObjectMapper mapper = new ObjectMapper();
		CloudBackupBackupOptionsBean configBean = null;
		try {
			configBean = mapper.readValue(configFile, CloudBackupBackupOptionsBean.class);
		} catch (JsonParseException e) {
			throw new CloudBackupConfigException("Cannot parse JSON in System Config file: " + e.getLocalizedMessage(), e);
		} catch (JsonMappingException e) {
			throw new CloudBackupConfigException("Cannot map JSON in System Config file to properties: " + e.getLocalizedMessage(), e);			
		} catch (IOException e) {
			throw new CloudBackupConfigException("Cannot access System config file: " + e.getLocalizedMessage(), e);
		}
		
		if (null != configBean) {
			INSTANCE = new CloudBackupBackupOptions();
			if (null == configBean.getBackupDataPipe()) {
				throw new CloudBackupConfigException("Missing Backup Config property: backupDataPipe");
			} else {
				INSTANCE.backupPipe = configBean.getBackupDataPipe();
			}
			
			if (null == configBean.getBackupDirectory()) {
				throw new CloudBackupConfigException("Missing Backup Config property: backupDirectory");
			} else {
				INSTANCE.backupDirectory = configBean.getBackupDirectory();
			}
			
			if (null == configBean.getBackupName()) {
				throw new CloudBackupConfigException("Missing Backup Config property: backupName");
			} else {
				INSTANCE.backupName = configBean.getBackupName();
			}
			
			if (null == configBean.getExcludeFiles()) {
				System.out.println("WARNING: no exclude files configured...");
			} else {
				INSTANCE.excludeFiles = configBean.getExcludeFiles();
			}
			
			if (null == configBean.getGoogleDriveBackupDirectory()) {
				throw new CloudBackupConfigException("Missing Backup Config property: googleDriveBackupDirectory");
			} else {
				INSTANCE.googleDriveBackupDirectory = configBean.getGoogleDriveBackupDirectory();
			}
			
			if (null == configBean.getIncludeFiles()) {
				System.out.println("WARNING: no include files configured...");
			} else {
				INSTANCE.includeFiles = configBean.getIncludeFiles();
			}
			
			if (null == configBean.getRestoreDataPipe()) {
				throw new CloudBackupConfigException("Missing Backup Config property: restoreDataPipe");
			} else {
				INSTANCE.restorePipe = configBean.getRestoreDataPipe();
			}
			
			if (null == configBean.getRestoreDirectory()) {
				throw new CloudBackupConfigException("Missing Backup Config property: restoreDirectory");
			} else {
				INSTANCE.restoreDirectory = configBean.getRestoreDirectory();
			}
			
			if (null == configBean.getRestoreTimestamp()) {
				System.out.println("WARNING: no restore timestamp configured");
				INSTANCE.restoreTimestamp = -1;
			} else {
				//"2026-04-17 11:14:00"
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					df.parse(configBean.getRestoreTimestamp());
				} catch (ParseException e) {
					throw new CloudBackupConfigException("Cannot parse restore timestamp: " + e.getMessage());
				}
			}
			
			initialized = true;
			
		} else {
			throw new CloudBackupConfigException("Unexpected error while reading System Config");
		}
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, INSTANCE);
		}
		return INSTANCE;
	}

	

	@Override
	public String toString() {	
		StringBuilder sb =	new StringBuilder("CloudBackupSystemOptions: ");
		sb.append(" backupDir=" + backupDirectory);
		sb.append(", backupName=" + backupName);
		sb.append(", backupPipe=" + backupPipe);
		sb.append(", excludeFiles=" + excludeFiles);
		sb.append(", googleDriveBackupDir=" + googleDriveBackupDirectory);
		sb.append(", includeFiles=" + includeFiles);
		sb.append(", restoreDir=" + restoreDirectory);
		sb.append(", restorePipe=" + restorePipe);
		sb.append(", restoreTimestamp=" + restoreTimestamp);
		return sb.toString();		 
	}

}

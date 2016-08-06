package cloudbackup.config;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import cloudbackup.exception.CloudBackupConfigException;
import cloudbackup.exception.CloudBackupException;
import cloudbackup.exception.FileSystemAccessException;

public class CloudBackupSystemOptions {
	
	private static CloudBackupSystemOptions INSTANCE = null;
	private static final String sourceClass = CloudBackupSystemOptions.class.getName();
	private static final Logger log = Logger.getLogger(sourceClass);	
	
	private String hashAlgorithm = null;
	private String masterPassword = null;
	private String loggingProperties = null;
	private String encryptionAlgorithm = null;
	
	private static boolean initialized = false;
	
	private CloudBackupSystemOptions() {
	}
	
	public static CloudBackupSystemOptions getInstance() throws CloudBackupException {
		if (!initialized) {
			throw new CloudBackupException("Configuration is not yet initialized");
		} 
		return INSTANCE;
	}
	
	public static CloudBackupSystemOptions initialize(String configFilePath) throws CloudBackupException {
		final String sourceMethod = "initialize"; 
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[]{configFilePath});
		}
		
		File configFile = new File(configFilePath);
		if (!configFile.exists() || configFile.isDirectory()) {
			throw new FileSystemAccessException("Cannot access config file: " + configFilePath);
		}
		
		ObjectMapper mapper = new ObjectMapper();
		CloudBackupSystemOptionsBean configBean = null;
		try {
			configBean = mapper.readValue(configFile, CloudBackupSystemOptionsBean.class);
		} catch (JsonParseException e) {
			throw new CloudBackupConfigException("Cannot parse JSON in System Config file: " + e.getLocalizedMessage(), e);
		} catch (JsonMappingException e) {
			throw new CloudBackupConfigException("Cannot map JSON in System Config file to properties: " + e.getLocalizedMessage(), e);			
		} catch (IOException e) {
			throw new CloudBackupConfigException("Cannot access System config file: " + e.getLocalizedMessage(), e);
		}
		
		if (null != configBean) {
			INSTANCE = new CloudBackupSystemOptions();
			if (null == configBean.getHashAlgorithm()) {
				throw new CloudBackupConfigException("Missing System Config property: hashAlgorithm");
			} else {
				INSTANCE.hashAlgorithm = configBean.getHashAlgorithm();
			}
			
			if (null == configBean.getMasterPassword()) {
				throw new CloudBackupConfigException("Missing System Config property: masterPassword");
			} else {
				INSTANCE.masterPassword = configBean.getMasterPassword();
			}
			
			if (null != configBean.getLoggingProperties()) {
				INSTANCE.loggingProperties = configBean.getLoggingProperties();
			} else {
				System.out.println("WARNING: no logging parameters configured...");
			}
			
			if (null != configBean.getEncryptionAlgorithm()) {
				INSTANCE.encryptionAlgorithm = configBean.getEncryptionAlgorithm();
			} else {
				System.out.println("WARNING: no encryption parameters configured...");
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

	public String getHashAlgorithm() {
		return hashAlgorithm;
	}

	public String getMasterPassword() {
		return masterPassword;
	}
	
	public String getEncryptionAlgorithm() {
		return encryptionAlgorithm;
	}

	public String getLoggingProperties() {
		return loggingProperties;
	}

	public void setLoggingProperties(String loggingProperties) {
		this.loggingProperties = loggingProperties;
	}

	@Override
	public String toString() {	
		StringBuilder sb =	new StringBuilder("CloudBackupSystemOptions: ");
		sb.append(" masterPassword=***");
		sb.append(", hashAlgorithm=" + hashAlgorithm);
		sb.append(", encryptionAlgorithm=" + encryptionAlgorithm);
		sb.append(", loggingProperties=" + loggingProperties);
		
		return sb.toString();		 
	}

}

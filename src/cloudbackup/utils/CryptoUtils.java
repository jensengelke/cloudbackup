package cloudbackup.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cloudbackup.config.CloudBackupSystemOptions;
import cloudbackup.exception.CloudBackupException;

public class CryptoUtils {

	private static final String sourceClass = CryptoUtils.class.getName();
	private static final Logger log = Logger.getLogger(sourceClass);	

	
	public static byte[] hashString(String message) throws CloudBackupException {
		final String sourceMethod = "hashString"; 
		if (log.isLoggable(Level.FINER)) {
			log.entering(sourceClass, sourceMethod, new Object[] {message});
		}
		String algorithm = CloudBackupSystemOptions.getInstance().getHashAlgorithm();
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));
			if (log.isLoggable(Level.FINER)) {
				log.exiting(sourceClass, sourceMethod, hashedBytes);
			}
			return hashedBytes;
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
			throw new CloudBackupException("Could not generate hash from String", ex);
		}
	}
	
	public static byte[] hashByteArray(byte[] message)throws CloudBackupException {
		final String sourceMethod = "hashByteArray"; 
		if (log.isLoggable(Level.FINER)) {
			log.entering(sourceClass, sourceMethod, new Object[] {message});
		}
		String algorithm = CloudBackupSystemOptions.getInstance().getHashAlgorithm();
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			byte[] hashedBytes = digest.digest(message);
			if (log.isLoggable(Level.FINER)) {
				log.exiting(sourceClass, sourceMethod, hashedBytes);
			}
			return hashedBytes;
		} catch (NoSuchAlgorithmException ex) {
			throw new CloudBackupException("Could not generate hash from String", ex);
		}
	}
	
	public static String hashStringToHex(String message) throws CloudBackupException {
		final String sourceMethod = "hashString"; 
		if (log.isLoggable(Level.FINER)) {
			log.entering(sourceClass, sourceMethod, new Object[] {message});
		}
		String algorithm = CloudBackupSystemOptions.getInstance().getHashAlgorithm();
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));
			String hash = convertByteArrayToHexString(hashedBytes); 
			if (log.isLoggable(Level.FINER)) {
				log.exiting(sourceClass, sourceMethod, hash);
			}
			return hash;
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
			throw new CloudBackupException("Could not generate hash from String", ex);
		}
	}
	
	public static String hashFile(File file) throws CloudBackupException {
		final String sourceMethod = "hashFile"; 
		if (log.isLoggable(Level.FINER)) {
			log.entering(sourceClass, sourceMethod, new Object[] {file});
		}
		String algorithm = CloudBackupSystemOptions.getInstance().getHashAlgorithm();		

		try (FileInputStream inputStream = new FileInputStream(file)) {
			MessageDigest digest = MessageDigest.getInstance(algorithm);

			byte[] bytesBuffer = new byte[10240];
			int bytesRead = -1;

			while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
				digest.update(bytesBuffer, 0, bytesRead);
			}

			byte[] hashedBytes = digest.digest();
			String hash = convertByteArrayToHexString(hashedBytes);
			if (log.isLoggable(Level.FINER)) {
				log.exiting(sourceClass, sourceMethod, hash);
			}
			return hash;
		} catch (NoSuchAlgorithmException | IOException ex) {
			throw new CloudBackupException("Could not generate hash from file", ex);
		}
	}
	
	public static String convertByteArrayToHexString(byte[] arrayBytes) {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < arrayBytes.length; i++) {
			stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return stringBuffer.toString();
	}
}

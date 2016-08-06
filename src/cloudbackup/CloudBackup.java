package cloudbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

public class CloudBackup {

	private Properties systemProps = null;
	private Properties backupProps = null;
	private boolean verbose = true;

	private CloudBackup(Properties systemProperties, Properties backupProperties) {
		this.systemProps = systemProperties;
		this.backupProps = backupProperties;

		// validate system props
		try {
			validateSystempProps();
		} catch (Exception e) {
			System.err.println("Aborting due to invalid system properties: " + e.getMessage());
		}

		// validate backup props
		try {
			validateBackupProps();
		} catch (Exception e) {
			System.err.println("Aborting due to invalid backup properties: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		if (null == args || args.length != 2) {
			printUsage();
		} else {
			// 1. get generic properties file
			Properties systemProps = new Properties();
			Properties backupProps = new Properties();
			try {
				FileInputStream fis = new FileInputStream("config/cloudBackup.properties");
				systemProps.load(fis);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 2. get backup properties file
			try {
				FileInputStream fis = new FileInputStream(args[1]);
				backupProps.load(fis);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 3. trigger either backup, restore or list
			CloudBackup cloudBackup = new CloudBackup(systemProps, backupProps);
			if ("backup".equals(args[0])) {
				try {
					cloudBackup.backup();
				} catch (Exception e) {
					System.err.println("Backup ended abnormally: " + e.getMessage());
					e.printStackTrace();
				}
			} else if ("list".equals(args[0])) {
				try {
					cloudBackup.list();
				} catch (Exception e) {
					System.err.println("List ended abnormally: " + e.getMessage());
					e.printStackTrace();
				}

			} else if ("restore".equals(args[0])) {
				try {
					cloudBackup.restore();
				} catch (Exception e) {
					System.err.println("Restore ended abnormally: " + e.getMessage());
					e.printStackTrace();
				}

			}
			System.out.println("finished after ms " + (System.currentTimeMillis() - startTime));
		}
	}

	private static final void printUsage() {
		System.out.println("Usage: CloudBackup <operation> <backup_properties>");
		System.out.println(" operation: backup | list | restore");
	}

	private void backup() throws Exception {
		// log
		System.out.println("-----------------------------");
		System.out.println("backing up");

		// access rootdir
		File rootDir = FileUtils.getFile((String) backupProps.getProperty(BACKUP_PROPNAME_ROOTDIR));
		if (!rootDir.exists() || !rootDir.isDirectory()) {
			throw new Exception("invalid rootDirectory: either not found or not a directory ... ");
		}

		// TODO: Factory and read type from sys props
		Backup backup = new GoogleDriveBackup(backupProps);

		// for each file in tree
		Iterator<File> fileIt = FileUtils.iterateFiles(rootDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		int totalFileCount = 0;
		int updatedFileCount = 0;
		int skippedFileCount = 0;
		int newBackupCount = 0;
		
		while (fileIt.hasNext()) {
			File currentFile = fileIt.next();
			totalFileCount++;

			String backupId = hashString(getRelativePath(currentFile, rootDir));
			FileMetaData metaData = new FileMetaData(getRelativePath(currentFile, rootDir), hashFile(currentFile), false);

			BackupItem backupItem = new BackupItem(backupId, metaData);
			if (backup.contains(backupItem)) {

				if (!backup.hasChanged(backupItem)) {
					// skip unmodified file
					skippedFileCount++;
				} else {
					// update
					updatedFileCount++;

					backup.update(backupItem);
				}
			} else {

				backup.add(backupItem);
				newBackupCount++;
			}
		}
		
		backup.markUntouchedFilesAsDeleted();

		System.out.println("total processed files: " + totalFileCount);
		System.out.println("new:                   " + newBackupCount);
		System.out.println("skipped:               " + skippedFileCount);
		System.out.println("updated:               " + updatedFileCount);
	}

	private void restore() throws Exception {

		// log
		System.out.println("-----------------------------");
		System.out.println("restoring");

		// access restoreDir
		File restoreDir = FileUtils.getFile((String) backupProps.getProperty(BACKUP_PROPNAME_RESTOREDIR));
		
		if (!restoreDir.exists() && !restoreDir.mkdirs()) {
		      throw new IOException("Unable to create parent directory");
		}		

		// TODO: Factory and read type from sys props
		Backup backup = new GoogleDriveBackup(backupProps);
		backup.restore();
		
	}

	private void list() {
		// log
		System.out.println("-----------------------------");
		System.out.println("listing");
		// TODO: Factory and read type from sys props
		Backup backup = new GoogleDriveBackup(backupProps);
		backup.list();

	}

	private void validateSystempProps() throws Exception {
		this.verbose = Boolean.parseBoolean((String) systemProps.getOrDefault(SYS_PROPNAME_VERBOSE, "true"));

		if (verbose) {
			System.out.println("-----------------------------");
			systemProps.store(System.out, "system properties");
		}

		if (!systemProps.containsKey(SYS_PROPNAME_MASTERPASS) || "".equals(systemProps.get(SYS_PROPNAME_MASTERPASS))) {
			throw new Exception(SYS_PROPNAME_MASTERPASS + " property in system properties is missing or invalid");
		}

	}

	private void validateBackupProps() throws Exception {
		if (verbose) {
			System.out.println("-----------------------------");
			backupProps.store(System.out, "backup properties");
		}
		if (!backupProps.containsKey(BACKUP_PROPNAME_BACKUPNAME) || "".equals(backupProps.get(BACKUP_PROPNAME_BACKUPNAME))) {
			throw new Exception(BACKUP_PROPNAME_BACKUPNAME + " property in backup properties is missing or invalid");
		}
		if (!backupProps.containsKey(BACKUP_PROPNAME_ROOTDIR) || "".equals(backupProps.get(BACKUP_PROPNAME_ROOTDIR))) {
			throw new Exception(BACKUP_PROPNAME_ROOTDIR + " property in backup properties is missing or invalid");
		} else {
			System.out.println("resolved rootDir: " + FileUtils.getFile((String) backupProps.get(BACKUP_PROPNAME_ROOTDIR)).getCanonicalPath());
		}

	}

	private String hashString(String message) throws Exception {
		String algorithm = (String) systemProps.getOrDefault("hashAlgorithm", "SHA-256");
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));

			return convertByteArrayToHexString(hashedBytes);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
			throw new Exception("Could not generate hash from String", ex);
		}
	}

	private String hashFile(File file) throws Exception {
		String algorithm = (String) systemProps.getOrDefault("hashAlgorithm", "SHA-256");

		try (FileInputStream inputStream = new FileInputStream(file)) {
			MessageDigest digest = MessageDigest.getInstance(algorithm);

			byte[] bytesBuffer = new byte[1024];
			int bytesRead = -1;

			while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
				digest.update(bytesBuffer, 0, bytesRead);
			}

			byte[] hashedBytes = digest.digest();

			return convertByteArrayToHexString(hashedBytes);
		} catch (NoSuchAlgorithmException | IOException ex) {
			throw new Exception("Could not generate hash from file", ex);
		}
	}

	private static String convertByteArrayToHexString(byte[] arrayBytes) {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < arrayBytes.length; i++) {
			stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return stringBuffer.toString();
	}

	private String getRelativePath(File fullFileName, File rootDir) {
		String rootPath = rootDir.getAbsolutePath();
		String filePath = fullFileName.getAbsolutePath();

		return filePath.substring(rootPath.length() + 1);
	}

	public static final String BACKUP_PROPNAME_ROOTDIR = "rootDirectory";
	public static final String BACKUP_PROPNAME_RESTOREDIR = "restoreDirectory";
	public static final String BACKUP_PROPNAME_BACKUPNAME = "backupName";
	private static final String SYS_PROPNAME_MASTERPASS = "masterPassword";
	private static final String SYS_PROPNAME_VERBOSE = "verbose";

}

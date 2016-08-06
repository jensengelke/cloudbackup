package cloudbackup.config;

public class CloudBackupBackupOptionsBean {
	
	private String backupName;
	private String backupDirectory;
	private String restoreDirectory;
	private String googleDriveBackupDirectory;
	private String includeFiles;
	private String excludeFiles;
	private String restoreTimestamp;
	private DataPipeConfig backupDataPipe;
	private DataPipeConfig restoreDataPipe;

	public CloudBackupBackupOptionsBean() {
	}

	public String getBackupName() {
		return backupName;
	}

	public void setBackupName(String backupName) {
		this.backupName = backupName;
	}

	public String getBackupDirectory() {
		return backupDirectory;
	}

	public void setBackupDirectory(String backupDirectory) {
		this.backupDirectory = backupDirectory;
	}

	public String getRestoreDirectory() {
		return restoreDirectory;
	}

	public void setRestoreDirectory(String restoreDirectory) {
		this.restoreDirectory = restoreDirectory;
	}

	public String getGoogleDriveBackupDirectory() {
		return googleDriveBackupDirectory;
	}

	public void setGoogleDriveBackupDirectory(String googleDriveBackupDirectory) {
		this.googleDriveBackupDirectory = googleDriveBackupDirectory;
	}

	public String getIncludeFiles() {
		return includeFiles;
	}

	public void setIncludeFiles(String includeFiles) {
		this.includeFiles = includeFiles;
	}

	public String getExcludeFiles() {
		return excludeFiles;
	}

	public void setExcludeFiles(String excludeFiles) {
		this.excludeFiles = excludeFiles;
	}

	public String getRestoreTimestamp() {
		return restoreTimestamp;
	}

	public void setRestoreTimestamp(String restoreTimestamp) {
		this.restoreTimestamp = restoreTimestamp;
	}

	public DataPipeConfig getBackupDataPipe() {
		return backupDataPipe;
	}

	public void setBackupDataPipe(DataPipeConfig backupDataPipe) {
		this.backupDataPipe = backupDataPipe;
	}

	public DataPipeConfig getRestoreDataPipe() {
		return restoreDataPipe;
	}

	public void setRestoreDataPipe(DataPipeConfig restoreDataPipe) {
		this.restoreDataPipe = restoreDataPipe;
	}
	
	
}

package cloudbackup;

public class BackupItem {
	
	private String backupId;
	private FileMetaData metaData;
	
	public BackupItem(String backupId, FileMetaData metaData) {
		this.backupId = backupId;
		this.metaData = metaData;
	}
	
	public String getBackupId() {
		return backupId;
	}
	public FileMetaData getMetaData() {
		return metaData;
	}
	
	public void addBackupVersion() {
		
	}
	
	

}

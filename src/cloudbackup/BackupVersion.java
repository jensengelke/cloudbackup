package cloudbackup;

import java.util.UUID;

public class BackupVersion {
	
	private UUID versionId;
	private long validFrom;
	private String backupFileName;
	private FileMetaData metaData;
	
	public BackupVersion(String baseBackupFileName, FileMetaData metaData) {
		this.versionId = UUID.randomUUID();
		this.validFrom = System.currentTimeMillis();
		this.backupFileName = baseBackupFileName + versionId.toString();
		this.metaData = metaData;
	}
	
	public FileMetaData getMetaData() {
		return metaData;
	}
	public UUID getVersionId() {
		return versionId;
	}
	public long getValidFrom() {
		return validFrom;
	}
	public String getBackupFileName() {
		return backupFileName;
	}

}

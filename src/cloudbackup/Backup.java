package cloudbackup;

import java.io.IOException;

public interface Backup {
	
	public boolean contains(BackupItem backupItem) throws IOException;
	
	public void add(BackupItem backupItem) throws IOException;
	
	public boolean hasChanged(BackupItem backupItem) throws IOException;
	
	public void update(BackupItem backupItem) throws IOException;
	
	public void list();

	public void restore() throws IOException;
	
	public void markUntouchedFilesAsDeleted() throws IOException;
	
}

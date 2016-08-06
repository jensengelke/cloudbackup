package cloudbackup.config;

import java.util.Date;

public class CloudBackupRunOptions {
	
	private String mode = null;
	private long backupTimestamp = new Date().getTime();
	private boolean running = true;
	private static CloudBackupRunOptions INSTANCE;
	
	private CloudBackupRunOptions() {};
	
	public static CloudBackupRunOptions getInstance() {
		if (null == INSTANCE) {
			INSTANCE = new CloudBackupRunOptions();
		}
		return INSTANCE;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public long getBackupTimestamp() {
		return backupTimestamp;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
	
	

}

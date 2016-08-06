package cloudbackup.exception;

public class CloudBackupConfigException extends CloudBackupException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CloudBackupConfigException(String message) {
		super(message);
	}
	
	public CloudBackupConfigException(String message, Throwable t) {
		super(message, t);
	}
	
}

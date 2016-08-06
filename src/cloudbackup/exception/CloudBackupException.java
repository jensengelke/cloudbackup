package cloudbackup.exception;

public class CloudBackupException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CloudBackupException(String message) {
		super(message);
	}
	
	public CloudBackupException(String message, Throwable t) {
		super(message, t);
	}

}

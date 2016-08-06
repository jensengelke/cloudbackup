package cloudbackup.exception;

public class LocalFileOrDirNotFoundException extends FileSystemAccessException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5761056866370543216L;

	public LocalFileOrDirNotFoundException(String message) {
		super(message);
	}

}

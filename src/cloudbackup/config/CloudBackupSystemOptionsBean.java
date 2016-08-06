package cloudbackup.config;

public class CloudBackupSystemOptionsBean {

	public CloudBackupSystemOptionsBean() {
	}
	
	private String masterPassword;
	private String hashAlgorithm;
	private String loggingProperties;
	private String encryptionAlgorithm;
	
	public String getMasterPassword() {
		return masterPassword;
	}
	public void setMasterPassword(String masterPassword) {
		this.masterPassword = masterPassword;
	}
	public String getHashAlgorithm() {
		return hashAlgorithm;
	}
	public void setHashAlgorithm(String hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}
	public String getLoggingProperties() {
		return loggingProperties;
	}
	public void setLoggingProperties(String loggingProperties) {
		this.loggingProperties = loggingProperties;
	}
	public String getEncryptionAlgorithm() {
		return encryptionAlgorithm;
	}
	public void setEncryptionAlgorithm(String encryptionAlgorithm) {
		this.encryptionAlgorithm = encryptionAlgorithm;
	}
	
}

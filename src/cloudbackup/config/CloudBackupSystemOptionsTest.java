package cloudbackup.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import cloudbackup.CloudBackupTestConstants;
import cloudbackup.exception.CloudBackupException;

public class CloudBackupSystemOptionsTest {
	
	

	@Test
	public void testGetInstance() {
		CloudBackupSystemOptions systemOptions = null;
		
		try {
			CloudBackupSystemOptions.initialize(CloudBackupTestConstants.CONFIG_FILE_PATH);
			systemOptions = CloudBackupSystemOptions.getInstance();
		} catch (CloudBackupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertNotNull("we should now get an instance... ", systemOptions);
	}

	@Test
	public void testInitialize() {
		CloudBackupSystemOptions systemOptions = null;
		try {
			systemOptions = CloudBackupSystemOptions.initialize("config/cloudBackup.json");		
		} catch (CloudBackupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertNotNull("no options", systemOptions);
		assertNotNull("no hashAlgorithm", systemOptions.getHashAlgorithm());
		assertNotNull("no masterPassword", systemOptions.getMasterPassword());
		
		//TODO: test option files with predefined values
		assertEquals("unexpected master password", "test123", systemOptions.getMasterPassword());
		assertEquals("unexpected hash algorithm", "SHA-256", systemOptions.getHashAlgorithm());
	}

}

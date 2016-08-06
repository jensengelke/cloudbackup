package cloudbackup.local;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import cloudbackup.CloudBackupTestConstants;
import cloudbackup.config.CloudBackupSystemOptions;
import cloudbackup.exception.FileSystemAccessException;
import cloudbackup.model.LocalFile;
import cloudbackup.model.LocalFileVersion;

public class LocalFileSystemConnectorTest {

	@Before
	public void setUp() throws Exception {
		CloudBackupSystemOptions.initialize(CloudBackupTestConstants.CONFIG_FILE_PATH);
	}

	@Test
	public void testIndexLocalFileSystem() {
		Set<LocalFile> fileSet = null;
		try {
			fileSet = LocalFileSystemConnector.getInstance().indexLocalFileSystem("d:/$user/jens", false);
		} catch (FileSystemAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertNotNull("no files found.", fileSet);
		assertNotEquals("no files found", 0l, (long)fileSet.size());
		
		System.out.println("number of files: " + fileSet.size());
		
		LocalFile localFile = fileSet.iterator().next();
		System.out.println("inspecting " + localFile);
		assertEquals("one version expected", 1, localFile.getVersions().size());
		LocalFileVersion revision = localFile.getVersions().iterator().next();
		System.out.println("inspecting version " + revision);
		
		assertNull("unnecessary content hash", revision.getContentHash());
		
	}
	
	@Test
	public void testIndexLocalFileSystemWithHash() {
		Set<LocalFile> fileSet = null;
		try {
			fileSet = LocalFileSystemConnector.getInstance().indexLocalFileSystem("d:/$user/jens", true);
		} catch (FileSystemAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertNotNull("no files found.", fileSet);
		assertNotEquals("no files found", 0l, (long)fileSet.size());
		
		System.out.println("number of files: " + fileSet.size());
		
		LocalFile localFile = fileSet.iterator().next();
		System.out.println("inspecting " + localFile);
		assertEquals("one version expected", 1, localFile.getVersions().size());
		LocalFileVersion revision = localFile.getVersions().iterator().next();
		System.out.println("inspecting version " + revision);
		
		assertNotNull("missing content hash", revision.getContentHash());
		
	}

}

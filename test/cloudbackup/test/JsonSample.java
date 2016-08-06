package cloudbackup.test;

import java.io.File;
import java.util.Iterator;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.codehaus.jackson.map.ObjectMapper;

import cloudbackup.model.LocalFile;
import cloudbackup.model.LocalFileVersion;
import cloudbackup.utils.CryptoUtils;

public class JsonSample {
	
	
	public static void main(String[] args) {

		File rootDir = new File("c:/temp/restore");
		Iterator<File> fileIt = FileUtils.iterateFiles(rootDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		
		ObjectMapper mapper = new ObjectMapper();
		while (fileIt.hasNext()) {
			File currentFile = fileIt.next();
			
			
			try {
				LocalFile localFile = new LocalFile(getRelativePath(currentFile, rootDir));
				LocalFileVersion currentVersion = new LocalFileVersion(currentFile.length(), currentFile.lastModified(), CryptoUtils.hashFile(currentFile));
				localFile.addVersion(currentVersion);
				mapper.writeValue(System.out, localFile);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

	}
	
	private static String getRelativePath(File fullFileName, File rootDir) {
		String rootPath = rootDir.getAbsolutePath();
		String filePath = fullFileName.getAbsolutePath();

		return filePath.substring(rootPath.length() + 1);
	}

	
	
}
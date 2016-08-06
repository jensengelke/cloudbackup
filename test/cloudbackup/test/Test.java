package cloudbackup.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Test {
	
	private static int BUFFER = 2048;

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		try {
			
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			FileOutputStream fos = new FileOutputStream(new File("c:/temp/file.zip"));
			ZipOutputStream out = new ZipOutputStream(fos);
			byte[] data = new byte[BUFFER];
			int count;
			ZipEntry e = new ZipEntry("test");
			FileInputStream origin = null;
		
			out.putNextEntry(e);
			origin = new FileInputStream(new File("d:/$user/Jens/050923_DA.doc"));
			
			while((count = origin.read(data, 0, BUFFER)) != -1) {
		               out.write(data, 0, count);
		            }
			
			origin.close();
			out.close();
			fos.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println("elapsed: " + (System.currentTimeMillis() - startTime));
	}

}

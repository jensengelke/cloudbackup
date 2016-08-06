package cloudbackup;

import java.nio.CharBuffer;
import java.util.logging.Logger;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.ptr.LongByReference;

import cloudbackup.config.CloudBackupRunOptions;

public class WinUtils implements Runnable {

	private static Logger log = Logger.getLogger(WinUtils.class.getName());

	public interface Kernel32 extends Library {
		Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
		Kernel32 SYNC_INSTANCE = (Kernel32) Native.synchronizedLibrary(INSTANCE);

		int GetShortPathNameW(WString lpszLongPath, char[] lpdzShortPath, int cchBuffer);

		int GetWindowsDirectoryW(char[] lpdzShortPath, int uSize);

		boolean GetVolumeInformationW(char[] lpRootPathName, CharBuffer lpVolumeNameBuffer, int nVolumeNameSize, LongByReference lpVolumeSerialNumber,
				LongByReference lpMaximumComponentLength, LongByReference lpFileSystemFlags, CharBuffer lpFileSystemNameBuffer, int nFileSystemNameSize);

		int SetThreadExecutionState(int EXECUTION_STATE);

		int ES_DISPLAY_REQUIRED = 0x00000002;
		int ES_SYSTEM_REQUIRED = 0x00000001;
		int ES_CONTINUOUS = 0x80000000;
	}

	public long lastDontSleepCall = 0;
	public long lastGoToSleepCall = 0;

	public void disableGoToSleep() {
		// Disable go to sleep (every 40s)
		if (System.currentTimeMillis() - lastDontSleepCall > 40000) {
			log.info("Calling SetThreadExecutionState ES_SYSTEM_REQUIRED");
			Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_CONTINUOUS);
			lastDontSleepCall = System.currentTimeMillis();
		}
	}

	public void reenableGoToSleep() {
		// Reenable go to sleep
		log.info("Calling SetThreadExecutionState ES_CONTINUOUS");
		Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
	}

	@Override
	public void run() {
		log.info("preventSleepMode thread started.");

		while (CloudBackupRunOptions.getInstance().isRunning()) {
			disableGoToSleep();
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		reenableGoToSleep();

	}

}

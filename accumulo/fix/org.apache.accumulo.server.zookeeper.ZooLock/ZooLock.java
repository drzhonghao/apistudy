

import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.zookeeper.KeeperException;


public class ZooLock extends org.apache.accumulo.fate.zookeeper.ZooLock {
	public static void deleteLock(String path) throws InterruptedException, KeeperException {
		org.apache.accumulo.fate.zookeeper.ZooLock.deleteLock(ZooReaderWriter.getInstance(), path);
	}

	public static boolean deleteLock(String path, String lockData) throws InterruptedException, KeeperException {
		return org.apache.accumulo.fate.zookeeper.ZooLock.deleteLock(ZooReaderWriter.getInstance(), path, lockData);
	}
}


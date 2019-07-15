import org.apache.accumulo.server.zookeeper.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.accumulo.fate.zookeeper.DistributedReadWriteLock;
import org.apache.zookeeper.KeeperException;

public class ZooQueueLock extends org.apache.accumulo.fate.zookeeper.ZooQueueLock {

  public ZooQueueLock(String path, boolean ephemeral) throws KeeperException, InterruptedException {
    super(ZooReaderWriter.getInstance(), path, ephemeral);
  }

  public static void main(String args[]) throws InterruptedException, KeeperException {
    ZooQueueLock lock = new ZooQueueLock("/lock", true);
    DistributedReadWriteLock rlocker = new DistributedReadWriteLock(lock, "reader".getBytes(UTF_8));
    DistributedReadWriteLock wlocker = new DistributedReadWriteLock(lock,
        "wlocker".getBytes(UTF_8));
    final Lock readLock = rlocker.readLock();
    readLock.lock();
    final Lock readLock2 = rlocker.readLock();
    readLock2.lock();
    final Lock writeLock = wlocker.writeLock();
    if (writeLock.tryLock(100, TimeUnit.MILLISECONDS))
      throw new RuntimeException("Write lock achieved during read lock!");
    readLock.unlock();
    readLock2.unlock();
    writeLock.lock();
    if (readLock.tryLock(100, TimeUnit.MILLISECONDS))
      throw new RuntimeException("Read lock achieved during write lock!");
    final Lock writeLock2 = DistributedReadWriteLock.recoverLock(lock, "wlocker".getBytes(UTF_8));
    writeLock2.unlock();
    readLock.lock();
    System.out.println("success");
  }

}

import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.util.ArrayUtil;

/**
 * Spawns a BG thread that periodically (defaults to 3.0
 * seconds, but accepts param in seconds) wakes up and asks
 * IndexWriter for a near real-time reader.  Then runs a
 * single query (body: 1) sorted by docdate, and prints
 * time to reopen and time to run the search.
 *
 * @lucene.experimental It's also not generally usable, eg
 * you cannot change which query is executed.
 */
public class NearRealtimeReaderTask extends PerfTask {

  long pauseMSec = 3000L;

  int reopenCount;
  int[] reopenTimes = new int[1];

  public NearRealtimeReaderTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws Exception {

    final PerfRunData runData = getRunData();

    // Get initial reader
    IndexWriter w = runData.getIndexWriter();
    if (w == null) {
      throw new RuntimeException("please open the writer before invoking NearRealtimeReader");
    }

    if (runData.getIndexReader() != null) {
      throw new RuntimeException("please close the existing reader before invoking NearRealtimeReader");
    }
    
    long t = System.currentTimeMillis();
    DirectoryReader r = DirectoryReader.open(w);
    runData.setIndexReader(r);
    // Transfer our reference to runData
    r.decRef();

    // TODO: gather basic metrics for reporting -- eg mean,
    // stddev, min/max reopen latencies

    // Parent sequence sets stopNow
    reopenCount = 0;
    while(!stopNow) {
      long waitForMsec = (pauseMSec - (System.currentTimeMillis() - t));
      if (waitForMsec > 0) {
        Thread.sleep(waitForMsec);
        //System.out.println("NRT wait: " + waitForMsec + " msec");
      }

      t = System.currentTimeMillis();
      final DirectoryReader newReader = DirectoryReader.openIfChanged(r);
      if (newReader != null) {
        final int delay = (int) (System.currentTimeMillis()-t);
        if (reopenTimes.length == reopenCount) {
          reopenTimes = ArrayUtil.grow(reopenTimes, 1+reopenCount);
        }
        reopenTimes[reopenCount++] = delay;
        // TODO: somehow we need to enable warming, here
        runData.setIndexReader(newReader);
        // Transfer our reference to runData
        newReader.decRef();
        r = newReader;
      }
    }
    stopNow = false;

    return reopenCount;
  }

  @Override
  public void setParams(String params) {
    super.setParams(params);
    pauseMSec = (long) (1000.0*Float.parseFloat(params));
  }

  @Override
  public void close() {
    System.out.println("NRT reopen times:");
    for(int i=0;i<reopenCount;i++) {
      System.out.print(" " + reopenTimes[i]);
    }
    System.out.println();
  }

  @Override
  public boolean supportsParams() {
    return true;
  }
}

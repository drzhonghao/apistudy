import org.apache.cassandra.io.util.DiskOptimizationStrategy;
import org.apache.cassandra.io.util.*;


public class SpinningDiskOptimizationStrategy implements DiskOptimizationStrategy
{
    /**
     * For spinning disks always add one page.
     */
    @Override
    public int bufferSize(long recordSize)
    {
        return roundBufferSize(recordSize + 4096);
    }
}

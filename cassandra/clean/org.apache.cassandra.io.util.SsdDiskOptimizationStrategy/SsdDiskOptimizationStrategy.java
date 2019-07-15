import org.apache.cassandra.io.util.DiskOptimizationStrategy;
import org.apache.cassandra.io.util.*;


public class SsdDiskOptimizationStrategy implements DiskOptimizationStrategy
{
    private final double diskOptimizationPageCrossChance;

    public SsdDiskOptimizationStrategy(double diskOptimizationPageCrossChance)
    {
        this.diskOptimizationPageCrossChance = diskOptimizationPageCrossChance;
    }

    /**
     * For solid state disks only add one page if the chance of crossing to the next page is more
     * than a predifined value.
     *
     * @see org.apache.cassandra.config.Config#disk_optimization_page_cross_chance
     */
    @Override
    public int bufferSize(long recordSize)
    {
        // The crossing probability is calculated assuming a uniform distribution of record
        // start position in a page, so it's the record size modulo the page size divided by
        // the total page size.
        double pageCrossProbability = (recordSize % 4096) / 4096.;
        // if the page cross probability is equal or bigger than disk_optimization_page_cross_chance we add one page
        if ((pageCrossProbability - diskOptimizationPageCrossChance) > -1e-16)
            recordSize += 4096;

        return roundBufferSize(recordSize);
    }
}

import org.apache.cassandra.metrics.DecayingEstimatedHistogramReservoir;
import org.apache.cassandra.metrics.*;


import com.google.common.annotations.VisibleForTesting;

import com.codahale.metrics.Histogram;

/**
 * Adds ability to reset a histogram
 */
public class ClearableHistogram extends Histogram
{
    private final DecayingEstimatedHistogramReservoir reservoirRef;

    /**
     * Creates a new {@link com.codahale.metrics.Histogram} with the given reservoir.
     *
     * @param reservoir the reservoir to create a histogram from
     */
    public ClearableHistogram(DecayingEstimatedHistogramReservoir reservoir)
    {
        super(reservoir);

        this.reservoirRef = reservoir;
    }

    @VisibleForTesting
    public void clear()
    {
        reservoirRef.clear();
    }
}

import org.apache.accumulo.core.iterators.user.*;


import java.util.Iterator;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.iterators.LongCombiner;

/**
 * A Combiner that interprets Values as Longs and returns the largest Long among them.
 */
public class MaxCombiner extends LongCombiner {
  @Override
  public Long typedReduce(Key key, Iterator<Long> iter) {
    long max = Long.MIN_VALUE;
    while (iter.hasNext()) {
      Long l = iter.next();
      if (l > max)
        max = l;
    }
    return max;
  }

  @Override
  public IteratorOptions describeOptions() {
    IteratorOptions io = super.describeOptions();
    io.setName("max");
    io.setDescription("MaxCombiner interprets Values as Longs and finds their"
        + " maximum.  A variety of encodings (variable length, fixed length, or"
        + " string) are available");
    return io;
  }
}

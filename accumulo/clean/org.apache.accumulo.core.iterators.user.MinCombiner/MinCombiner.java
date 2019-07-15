import org.apache.accumulo.core.iterators.user.*;


import java.util.Iterator;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.iterators.LongCombiner;

/**
 * A Combiner that interprets Values as Longs and returns the smallest Long among them.
 */
public class MinCombiner extends LongCombiner {
  @Override
  public Long typedReduce(Key key, Iterator<Long> iter) {
    long min = Long.MAX_VALUE;
    while (iter.hasNext()) {
      Long l = iter.next();
      if (l < min)
        min = l;
    }
    return min;
  }

  @Override
  public IteratorOptions describeOptions() {
    IteratorOptions io = super.describeOptions();
    io.setName("min");
    io.setDescription("MinCombiner interprets Values as Longs and finds their"
        + " minimum.  A variety of encodings (variable length, fixed length, or"
        + " string) are available");
    return io;
  }
}

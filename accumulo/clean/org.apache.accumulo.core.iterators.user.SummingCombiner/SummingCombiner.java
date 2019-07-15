import org.apache.accumulo.core.iterators.user.*;


import java.util.Iterator;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.iterators.LongCombiner;

/**
 * A Combiner that interprets Values as Longs and returns their sum.
 */
public class SummingCombiner extends LongCombiner {
  @Override
  public Long typedReduce(Key key, Iterator<Long> iter) {
    long sum = 0;
    while (iter.hasNext()) {
      Long next = iter.next();
      sum = safeAdd(sum, next);
    }
    return sum;
  }

  @Override
  public IteratorOptions describeOptions() {
    IteratorOptions io = super.describeOptions();
    io.setName("sum");
    io.setDescription("SummingCombiner interprets Values as Longs and adds them"
        + " together.  A variety of encodings (variable length, fixed length, or"
        + " string) are available");
    return io;
  }
}

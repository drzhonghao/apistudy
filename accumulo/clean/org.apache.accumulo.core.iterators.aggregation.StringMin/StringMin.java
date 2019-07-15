import org.apache.accumulo.core.iterators.aggregation.*;


import org.apache.accumulo.core.data.Value;

/**
 * @deprecated since 1.4, replaced by {@link org.apache.accumulo.core.iterators.user.MinCombiner}
 *             with {@link org.apache.accumulo.core.iterators.LongCombiner.Type#STRING}
 */
@Deprecated
public class StringMin implements Aggregator {

  long min = Long.MAX_VALUE;

  @Override
  public Value aggregate() {
    return new Value(Long.toString(min).getBytes());
  }

  @Override
  public void collect(Value value) {
    long l = Long.parseLong(new String(value.get()));
    if (l < min) {
      min = l;
    }
  }

  @Override
  public void reset() {
    min = Long.MAX_VALUE;
  }

}

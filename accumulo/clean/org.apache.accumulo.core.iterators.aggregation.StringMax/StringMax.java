import org.apache.accumulo.core.iterators.aggregation.*;


import org.apache.accumulo.core.data.Value;

/**
 * @deprecated since 1.4, replaced by {@link org.apache.accumulo.core.iterators.user.MaxCombiner}
 *             with {@link org.apache.accumulo.core.iterators.LongCombiner.Type#STRING}
 */
@Deprecated
public class StringMax implements Aggregator {

  long max = Long.MIN_VALUE;

  @Override
  public Value aggregate() {
    return new Value(Long.toString(max).getBytes());
  }

  @Override
  public void collect(Value value) {
    long l = Long.parseLong(new String(value.get()));
    if (l > max) {
      max = l;
    }
  }

  @Override
  public void reset() {
    max = Long.MIN_VALUE;
  }

}

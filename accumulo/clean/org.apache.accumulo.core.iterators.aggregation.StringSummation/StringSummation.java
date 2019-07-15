import org.apache.accumulo.core.iterators.aggregation.*;


import org.apache.accumulo.core.data.Value;

/**
 * @deprecated since 1.4, replaced by
 *             {@link org.apache.accumulo.core.iterators.user.SummingCombiner} with
 *             {@link org.apache.accumulo.core.iterators.LongCombiner.Type#STRING}
 */
@Deprecated
public class StringSummation implements Aggregator {

  long sum = 0;

  @Override
  public Value aggregate() {
    return new Value(Long.toString(sum).getBytes());
  }

  @Override
  public void collect(Value value) {
    sum += Long.parseLong(new String(value.get()));
  }

  @Override
  public void reset() {
    sum = 0;

  }
}

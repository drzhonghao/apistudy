import org.apache.accumulo.core.iterators.system.*;


import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.Sampler;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;

public class SampleIterator extends Filter {

  private Sampler sampler = new RowSampler();

  public SampleIterator(SortedKeyValueIterator<Key,Value> iter, Sampler sampler) {
    setSource(iter);
    this.sampler = sampler;
  }

  @Override
  public boolean accept(Key k, Value v) {
    return sampler.accept(k);
  }

  @Override
  public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
    return new SampleIterator(getSource().deepCopy(env), sampler);
  }
}

import org.apache.accumulo.tserver.MemKey;
import org.apache.accumulo.tserver.*;


import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SkippingIterator;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.system.InterruptibleIterator;

class PartialMutationSkippingIterator extends SkippingIterator implements InterruptibleIterator {

  private int kvCount;

  public PartialMutationSkippingIterator(SortedKeyValueIterator<Key,Value> source, int maxKVCount) {
    setSource(source);
    this.kvCount = maxKVCount;
  }

  @Override
  protected void consume() throws IOException {
    while (getSource().hasTop() && ((MemKey) getSource().getTopKey()).getKVCount() > kvCount)
      getSource().next();
  }

  @Override
  public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
    return new PartialMutationSkippingIterator(getSource().deepCopy(env), kvCount);
  }

  @Override
  public void setInterruptFlag(AtomicBoolean flag) {
    ((InterruptibleIterator) getSource()).setInterruptFlag(flag);
  }

}

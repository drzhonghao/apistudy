import org.apache.accumulo.core.iterators.*;


import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.KeyValue;
import org.apache.accumulo.core.data.Value;

public class IteratorAdapter implements Iterator<Entry<Key,Value>> {

  SortedKeyValueIterator<Key,Value> inner;

  public IteratorAdapter(SortedKeyValueIterator<Key,Value> inner) {
    this.inner = inner;
  }

  @Override
  public boolean hasNext() {
    return inner.hasTop();
  }

  @Override
  public Entry<Key,Value> next() {
    try {
      Entry<Key,Value> result = new KeyValue(new Key(inner.getTopKey()),
          new Value(inner.getTopValue()).get());
      inner.next();
      return result;
    } catch (IOException ex) {
      throw new NoSuchElementException();
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}

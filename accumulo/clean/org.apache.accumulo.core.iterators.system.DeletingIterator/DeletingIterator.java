import org.apache.accumulo.core.iterators.system.*;


import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;

public class DeletingIterator extends WrappingIterator {
  private boolean propogateDeletes;
  private Key workKey = new Key();

  @Override
  public DeletingIterator deepCopy(IteratorEnvironment env) {
    return new DeletingIterator(this, env);
  }

  public DeletingIterator(DeletingIterator other, IteratorEnvironment env) {
    setSource(other.getSource().deepCopy(env));
    propogateDeletes = other.propogateDeletes;
  }

  public DeletingIterator() {}

  public DeletingIterator(SortedKeyValueIterator<Key,Value> iterator, boolean propogateDeletes)
      throws IOException {
    this.setSource(iterator);
    this.propogateDeletes = propogateDeletes;
  }

  @Override
  public void next() throws IOException {
    SortedKeyValueIterator<Key,Value> source = getSource();
    if (super.getTopKey().isDeleted())
      skipRowColumn(source);
    else
      source.next();
    findTop(source);
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {
    // do not want to seek to the middle of a row
    Range seekRange = IteratorUtil.maximizeStartKeyTimeStamp(range);

    super.seek(seekRange, columnFamilies, inclusive);
    SortedKeyValueIterator<Key,Value> source = getSource();
    findTop(source);

    if (range.getStartKey() != null) {
      while (source.hasTop() && source.getTopKey().compareTo(range.getStartKey(),
          PartialKey.ROW_COLFAM_COLQUAL_COLVIS_TIME) < 0) {
        next();
      }

      while (hasTop() && range.beforeStartKey(getTopKey())) {
        next();
      }
    }
  }

  private void findTop(SortedKeyValueIterator<Key,Value> source) throws IOException {
    if (!propogateDeletes) {
      while (source.hasTop() && source.getTopKey().isDeleted()) {
        skipRowColumn(source);
      }
    }
  }

  private void skipRowColumn(SortedKeyValueIterator<Key,Value> source) throws IOException {
    workKey.set(source.getTopKey());

    Key keyToSkip = workKey;
    source.next();

    while (source.hasTop()
        && source.getTopKey().equals(keyToSkip, PartialKey.ROW_COLFAM_COLQUAL_COLVIS)) {
      source.next();
    }
  }

  @Override
  public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options,
      IteratorEnvironment env) {
    throw new UnsupportedOperationException();
  }
}

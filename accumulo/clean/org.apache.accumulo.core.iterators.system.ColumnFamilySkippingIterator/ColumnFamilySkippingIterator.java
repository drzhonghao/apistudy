import org.apache.accumulo.core.iterators.system.*;


import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SkippingIterator;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;

public class ColumnFamilySkippingIterator extends SkippingIterator
    implements InterruptibleIterator {

  protected Set<ByteSequence> colFamSet = null;
  protected TreeSet<ByteSequence> sortedColFams = null;

  protected boolean inclusive = false;
  protected Range range;

  public ColumnFamilySkippingIterator(SortedKeyValueIterator<Key,Value> source) {
    this.setSource(source);
  }

  protected ColumnFamilySkippingIterator(SortedKeyValueIterator<Key,Value> source,
      Set<ByteSequence> colFamSet, boolean inclusive) {
    this(source);
    this.colFamSet = colFamSet;
    this.inclusive = inclusive;
  }

  @Override
  protected void consume() throws IOException {
    SortedKeyValueIterator<Key,Value> source = getSource();
    int count = 0;

    if (inclusive)
      while (source.hasTop() && !colFamSet.contains(source.getTopKey().getColumnFamilyData())) {
        if (count < 10) {
          // it is quicker to call next if we are close, but we never know if we are close
          // so give next a try a few times
          source.next();
          count++;
        } else {
          ByteSequence higherCF = sortedColFams.higher(source.getTopKey().getColumnFamilyData());
          if (higherCF == null) {
            // seek to the next row
            reseek(source.getTopKey().followingKey(PartialKey.ROW));
          } else {
            // seek to the next column family in the sorted list of column families
            reseek(new Key(source.getTopKey().getRowData().toArray(), higherCF.toArray(),
                new byte[0], new byte[0], Long.MAX_VALUE));
          }

          count = 0;
        }
      }
    else if (colFamSet != null && colFamSet.size() > 0)
      while (source.hasTop() && colFamSet.contains(source.getTopKey().getColumnFamilyData())) {
        if (count < 10) {
          source.next();
          count++;
        } else {
          // seek to the next column family in the data
          reseek(source.getTopKey().followingKey(PartialKey.ROW_COLFAM));
          count = 0;
        }
      }
  }

  private void reseek(Key key) throws IOException {
    if (range.afterEndKey(key)) {
      range = new Range(range.getEndKey(), true, range.getEndKey(), range.isEndKeyInclusive());
      getSource().seek(range, colFamSet, inclusive);
    } else {
      range = new Range(key, true, range.getEndKey(), range.isEndKeyInclusive());
      getSource().seek(range, colFamSet, inclusive);
    }
  }

  @Override
  public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
    return new ColumnFamilySkippingIterator(getSource().deepCopy(env), colFamSet, inclusive);
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {

    if (columnFamilies instanceof Set<?>) {
      colFamSet = (Set<ByteSequence>) columnFamilies;
    } else {
      colFamSet = new HashSet<>();
      colFamSet.addAll(columnFamilies);
    }

    if (inclusive) {
      sortedColFams = new TreeSet<>(colFamSet);
    } else {
      sortedColFams = null;
    }

    this.range = range;
    this.inclusive = inclusive;
    super.seek(range, colFamSet, inclusive);
  }

  @Override
  public void setInterruptFlag(AtomicBoolean flag) {
    ((InterruptibleIterator) getSource()).setInterruptFlag(flag);
  }

}

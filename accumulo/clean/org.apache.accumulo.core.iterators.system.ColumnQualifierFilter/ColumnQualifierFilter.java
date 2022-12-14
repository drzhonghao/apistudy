import org.apache.accumulo.core.iterators.system.*;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Column;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;

public class ColumnQualifierFilter extends Filter {
  private final boolean scanColumns;
  private HashSet<ByteSequence> columnFamilies;
  private HashMap<ByteSequence,HashSet<ByteSequence>> columnsQualifiers;

  public ColumnQualifierFilter(SortedKeyValueIterator<Key,Value> iterator, Set<Column> columns) {
    setSource(iterator);
    this.columnFamilies = new HashSet<>();
    this.columnsQualifiers = new HashMap<>();

    for (Column col : columns) {
      if (col.columnQualifier != null) {
        ArrayByteSequence cq = new ArrayByteSequence(col.columnQualifier);
        HashSet<ByteSequence> cfset = this.columnsQualifiers.get(cq);
        if (cfset == null) {
          cfset = new HashSet<>();
          this.columnsQualifiers.put(cq, cfset);
        }

        cfset.add(new ArrayByteSequence(col.columnFamily));
      } else {
        // this whole column family should pass
        columnFamilies.add(new ArrayByteSequence(col.columnFamily));
      }
    }

    // only take action when column qualifies are present
    scanColumns = this.columnsQualifiers.size() > 0;
  }

  private ColumnQualifierFilter(SortedKeyValueIterator<Key,Value> iterator,
      HashSet<ByteSequence> columnFamilies,
      HashMap<ByteSequence,HashSet<ByteSequence>> columnsQualifiers, boolean scanColumns) {
    setSource(iterator);
    this.columnFamilies = columnFamilies;
    this.columnsQualifiers = columnsQualifiers;
    this.scanColumns = scanColumns;
  }

  @Override
  public boolean accept(Key key, Value v) {
    if (!scanColumns)
      return true;

    if (columnFamilies.contains(key.getColumnFamilyData()))
      return true;

    HashSet<ByteSequence> cfset = columnsQualifiers.get(key.getColumnQualifierData());
    // ensure the columm qualifier goes with a paired column family,
    // it is possible that a column qualifier could occur with a
    // column family it was not paired with
    return cfset != null && cfset.contains(key.getColumnFamilyData());
  }

  @Override
  public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
    return new ColumnQualifierFilter(getSource().deepCopy(env), columnFamilies, columnsQualifiers,
        scanColumns);
  }

  public static SortedKeyValueIterator<Key,Value> wrap(SortedKeyValueIterator<Key,Value> source,
      Set<Column> cols) {
    boolean sawNonNullQual = false;
    for (Column col : cols) {
      if (col.getColumnQualifier() != null) {
        sawNonNullQual = true;
        break;
      }
    }

    if (sawNonNullQual) {
      return new ColumnQualifierFilter(source, cols);
    } else {
      return source;
    }
  }
}

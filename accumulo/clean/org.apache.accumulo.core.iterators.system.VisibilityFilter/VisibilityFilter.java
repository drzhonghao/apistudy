import org.apache.accumulo.core.iterators.system.*;


import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.security.VisibilityEvaluator;
import org.apache.accumulo.core.security.VisibilityParseException;
import org.apache.accumulo.core.util.BadArgumentException;
import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisibilityFilter extends Filter {
  protected VisibilityEvaluator ve;
  protected ByteSequence defaultVisibility;
  protected LRUMap cache;
  protected Authorizations authorizations;

  private static final Logger log = LoggerFactory.getLogger(VisibilityFilter.class);

  private VisibilityFilter(SortedKeyValueIterator<Key,Value> iterator,
      Authorizations authorizations, byte[] defaultVisibility) {
    setSource(iterator);
    this.ve = new VisibilityEvaluator(authorizations);
    this.authorizations = authorizations;
    this.defaultVisibility = new ArrayByteSequence(defaultVisibility);
    this.cache = new LRUMap(1000);
  }

  @Override
  public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
    return new VisibilityFilter(getSource().deepCopy(env), authorizations,
        defaultVisibility.toArray());
  }

  @Override
  public boolean accept(Key k, Value v) {
    ByteSequence testVis = k.getColumnVisibilityData();

    if (testVis.length() == 0 && defaultVisibility.length() == 0)
      return true;
    else if (testVis.length() == 0)
      testVis = defaultVisibility;

    Boolean b = (Boolean) cache.get(testVis);
    if (b != null)
      return b;

    try {
      Boolean bb = ve.evaluate(new ColumnVisibility(testVis.toArray()));
      cache.put(testVis, bb);
      return bb;
    } catch (VisibilityParseException e) {
      log.error("Parse Error", e);
      return false;
    } catch (BadArgumentException e) {
      log.error("Parse Error", e);
      return false;
    }
  }

  private static class EmptyAuthsVisibilityFilter extends Filter {

    public EmptyAuthsVisibilityFilter(SortedKeyValueIterator<Key,Value> source) {
      setSource(source);
    }

    @Override
    public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
      return new EmptyAuthsVisibilityFilter(getSource().deepCopy(env));
    }

    @Override
    public boolean accept(Key k, Value v) {
      return k.getColumnVisibilityData().length() == 0;
    }
  }

  public static SortedKeyValueIterator<Key,Value> wrap(SortedKeyValueIterator<Key,Value> source,
      Authorizations authorizations, byte[] defaultVisibility) {
    if (authorizations.isEmpty() && defaultVisibility.length == 0) {
      return new EmptyAuthsVisibilityFilter(source);
    } else {
      return new VisibilityFilter(source, authorizations, defaultVisibility);
    }
  }
}

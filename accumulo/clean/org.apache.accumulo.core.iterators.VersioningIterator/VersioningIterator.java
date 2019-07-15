import org.apache.accumulo.core.iterators.user.VersioningIterator;
import org.apache.accumulo.core.iterators.*;


import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

/**
 * This class remains here for backwards compatibility.
 *
 * @deprecated since 1.4, replaced by
 *             {@link org.apache.accumulo.core.iterators.user.VersioningIterator}
 */
@Deprecated
public class VersioningIterator extends org.apache.accumulo.core.iterators.user.VersioningIterator {
  public VersioningIterator() {}

  public VersioningIterator(SortedKeyValueIterator<Key,Value> iterator, int maxVersions) {
    super();
    this.setSource(iterator);
    this.maxVersions = maxVersions;
  }
}

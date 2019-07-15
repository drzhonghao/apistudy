import org.apache.lucene.codecs.bloom.FuzzySet;
import org.apache.lucene.codecs.bloom.*;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.SegmentWriteState;


/**
 * Class used to create index-time {@link FuzzySet} appropriately configured for
 * each field. Also called to right-size bitsets for serialization.
 * @lucene.experimental
 */
public abstract class BloomFilterFactory {
  
  /**
   * 
   * @param state  The content to be indexed
   * @param info
   *          the field requiring a BloomFilter
   * @return An appropriately sized set or null if no BloomFiltering required
   */
  public abstract FuzzySet getSetForField(SegmentWriteState state, FieldInfo info);
  
  /**
   * Called when downsizing bitsets for serialization
   * 
   * @param fieldInfo
   *          The field with sparse set bits
   * @param initialSet
   *          The bits accumulated
   * @return null or a hopefully more densely packed, smaller bitset
   */
  public FuzzySet downsize(FieldInfo fieldInfo, FuzzySet initialSet) {
    // Aim for a bitset size that would have 10% of bits set (so 90% of searches
    // would fail-fast)
    float targetMaxSaturation = 0.1f;
    return initialSet.downsize(targetMaxSaturation);
  }

  /**
   * Used to determine if the given filter has reached saturation and should be retired i.e. not saved any more
   * @param bloomFilter The bloomFilter being tested
   * @param fieldInfo The field with which this filter is associated
   * @return true if the set has reached saturation and should be retired
   */
  public abstract boolean isSaturated(FuzzySet bloomFilter, FieldInfo fieldInfo);
  
}

import org.apache.lucene.codecs.bloom.BloomFilterFactory;
import org.apache.lucene.codecs.bloom.FuzzySet;
import org.apache.lucene.codecs.bloom.*;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.SegmentWriteState;

/**
 * Default policy is to allocate a bitset with 10% saturation given a unique term per document.
 * Bits are set via MurmurHash2 hashing function.
 *  @lucene.experimental
 */
public class DefaultBloomFilterFactory extends BloomFilterFactory {
  
  @Override
  public FuzzySet getSetForField(SegmentWriteState state,FieldInfo info) {
    //Assume all of the docs have a unique term (e.g. a primary key) and we hope to maintain a set with 10% of bits set
    return FuzzySet.createSetBasedOnQuality(state.segmentInfo.maxDoc(), 0.10f);
  }
  
  @Override
  public boolean isSaturated(FuzzySet bloomFilter, FieldInfo fieldInfo) {
    // Don't bother saving bitsets if >90% of bits are set - we don't want to
    // throw any more memory at this problem.
    return bloomFilter.getSaturation() > 0.9f;
  }
  
}

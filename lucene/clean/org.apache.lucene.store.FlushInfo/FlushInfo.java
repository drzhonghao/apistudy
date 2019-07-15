import org.apache.lucene.store.*;



/**
 * <p>A FlushInfo provides information required for a FLUSH context.
 *  It is used as part of an {@link IOContext} in case of FLUSH context.</p>
 */


public class FlushInfo {
  
  public final int numDocs;
  
  public final long estimatedSegmentSize;
  
  /**
   * <p>Creates a new {@link FlushInfo} instance from
   * the values required for a FLUSH {@link IOContext} context.
   * 
   * These values are only estimates and are not the actual values.
   * 
   */
  
  public FlushInfo(int numDocs, long estimatedSegmentSize) {
    this.numDocs = numDocs;
    this.estimatedSegmentSize = estimatedSegmentSize;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + (int) (estimatedSegmentSize ^ (estimatedSegmentSize >>> 32));
    result = prime * result + numDocs;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    FlushInfo other = (FlushInfo) obj;
    if (estimatedSegmentSize != other.estimatedSegmentSize)
      return false;
    if (numDocs != other.numDocs)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "FlushInfo [numDocs=" + numDocs + ", estimatedSegmentSize="
        + estimatedSegmentSize + "]";
  }

}

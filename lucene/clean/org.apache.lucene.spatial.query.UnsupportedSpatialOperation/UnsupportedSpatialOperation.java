import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.spatial.query.*;


/**
 * Exception thrown when the {@link org.apache.lucene.spatial.SpatialStrategy} cannot implement the requested operation.
 * @lucene.experimental
 */
public class UnsupportedSpatialOperation extends UnsupportedOperationException {

  public UnsupportedSpatialOperation(SpatialOperation op) {
    super(op.getName());
  }
}

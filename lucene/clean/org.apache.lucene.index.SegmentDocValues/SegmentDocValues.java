import org.apache.lucene.index.*;



import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.RefCount;

/**
 * Manages the {@link DocValuesProducer} held by {@link SegmentReader} and
 * keeps track of their reference counting.
 */
final class SegmentDocValues {

  private final Map<Long,RefCount<DocValuesProducer>> genDVProducers = new HashMap<>();

  private RefCount<DocValuesProducer> newDocValuesProducer(SegmentCommitInfo si, Directory dir, final Long gen, FieldInfos infos) throws IOException {
    Directory dvDir = dir;
    String segmentSuffix = "";
    if (gen.longValue() != -1) {
      dvDir = si.info.dir; // gen'd files are written outside CFS, so use SegInfo directory
      segmentSuffix = Long.toString(gen.longValue(), Character.MAX_RADIX);
    }

    // set SegmentReadState to list only the fields that are relevant to that gen
    SegmentReadState srs = new SegmentReadState(dvDir, si.info, infos, IOContext.READ, segmentSuffix);
    DocValuesFormat dvFormat = si.info.getCodec().docValuesFormat();
    return new RefCount<DocValuesProducer>(dvFormat.fieldsProducer(srs)) {
      @SuppressWarnings("synthetic-access")
      @Override
      protected void release() throws IOException {
        object.close();
        synchronized (SegmentDocValues.this) {
          genDVProducers.remove(gen);
        }
      }
    };
  }

  /** Returns the {@link DocValuesProducer} for the given generation. */
  synchronized DocValuesProducer getDocValuesProducer(long gen, SegmentCommitInfo si, Directory dir, FieldInfos infos) throws IOException {
    RefCount<DocValuesProducer> dvp = genDVProducers.get(gen);
    if (dvp == null) {
      dvp = newDocValuesProducer(si, dir, gen, infos);
      assert dvp != null;
      genDVProducers.put(gen, dvp);
    } else {
      dvp.incRef();
    }
    return dvp.get();
  }
  
  /**
   * Decrement the reference count of the given {@link DocValuesProducer}
   * generations. 
   */
  synchronized void decRef(List<Long> dvProducersGens) throws IOException {
    IOUtils.applyToAll(dvProducersGens, gen -> {
      RefCount<DocValuesProducer> dvp = genDVProducers.get(gen);
      assert dvp != null : "gen=" + gen;
      dvp.decRef();
    });
  }
}

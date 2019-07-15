import org.apache.accumulo.tserver.compaction.WriteParameters;
import org.apache.accumulo.tserver.compaction.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.accumulo.server.fs.FileRef;

import com.google.common.collect.Sets;

/**
 * A plan for a compaction: the input files, the files that are *not* inputs to a compaction that
 * should simply be deleted, and the optional parameters used to create the resulting output file.
 */
public class CompactionPlan {
  public final List<FileRef> inputFiles = new ArrayList<>();
  public final List<FileRef> deleteFiles = new ArrayList<>();
  public WriteParameters writeParameters = null;

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append(inputFiles.toString());
    if (!deleteFiles.isEmpty()) {
      b.append(" files to be deleted ");
      b.append(deleteFiles);
      if (writeParameters != null) {
        if (writeParameters.getCompressType() != null)
          b.append(" compress type " + writeParameters.getCompressType());
        if (writeParameters.getHdfsBlockSize() != 0)
          b.append(" hdfs block size " + writeParameters.getHdfsBlockSize());
        if (writeParameters.getBlockSize() != 0)
          b.append(" data block size " + writeParameters.getBlockSize());
        if (writeParameters.getIndexBlockSize() != 0)
          b.append(" index block size " + writeParameters.getIndexBlockSize());
        if (writeParameters.getReplication() != 0)
          b.append(" replication " + writeParameters.getReplication());
      }
    }
    return b.toString();
  }

  /**
   * Validate compaction plan.
   *
   * @param allFiles
   *          All possible files
   * @throws IllegalStateException
   *           thrown when validation fails.
   */
  public final void validate(Set<FileRef> allFiles) {
    Set<FileRef> inputSet = new HashSet<>(inputFiles);
    Set<FileRef> deleteSet = new HashSet<>(deleteFiles);

    if (!allFiles.containsAll(inputSet)) {
      inputSet.removeAll(allFiles);
      throw new IllegalStateException("plan inputs contains files not in allFiles " + inputSet);
    }

    if (!allFiles.containsAll(deleteSet)) {
      deleteSet.removeAll(allFiles);
      throw new IllegalStateException("plan deletes contains files not in allFiles " + deleteSet);
    }

    if (!Collections.disjoint(inputSet, deleteSet)) {
      throw new IllegalStateException("plan contains overlap in inputFiles and deleteFiles "
          + Sets.intersection(inputSet, deleteSet));
    }
  }
}

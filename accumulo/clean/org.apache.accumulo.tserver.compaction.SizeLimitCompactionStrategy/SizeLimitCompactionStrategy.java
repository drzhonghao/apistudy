import org.apache.accumulo.tserver.compaction.*;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.server.fs.FileRef;

/**
 *
 */
public class SizeLimitCompactionStrategy extends DefaultCompactionStrategy {
  public static final String SIZE_LIMIT_OPT = "sizeLimit";

  private long limit;

  @Override
  public void init(Map<String,String> options) {
    limit = AccumuloConfiguration.getMemoryInBytes(options.get(SIZE_LIMIT_OPT));
  }

  private MajorCompactionRequest filterFiles(MajorCompactionRequest mcr) {
    Map<FileRef,DataFileValue> filteredFiles = new HashMap<>();
    for (Entry<FileRef,DataFileValue> entry : mcr.getFiles().entrySet()) {
      if (entry.getValue().getSize() <= limit) {
        filteredFiles.put(entry.getKey(), entry.getValue());
      }
    }

    mcr = new MajorCompactionRequest(mcr);
    mcr.setFiles(filteredFiles);

    return mcr;
  }

  @Override
  public boolean shouldCompact(MajorCompactionRequest request) {
    return super.shouldCompact(filterFiles(request));
  }

  @Override
  public void gatherInformation(MajorCompactionRequest request) throws IOException {
    super.gatherInformation(filterFiles(request));
  }

  @Override
  public CompactionPlan getCompactionPlan(MajorCompactionRequest request) {
    return super.getCompactionPlan(filterFiles(request));
  }

}

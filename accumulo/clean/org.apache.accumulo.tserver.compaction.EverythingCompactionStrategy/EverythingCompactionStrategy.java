import org.apache.accumulo.tserver.compaction.CompactionStrategy;
import org.apache.accumulo.tserver.compaction.MajorCompactionRequest;
import org.apache.accumulo.tserver.compaction.CompactionPlan;
import org.apache.accumulo.tserver.compaction.*;


import java.io.IOException;

/**
 * The default compaction strategy for user initiated compactions. This strategy will always select
 * all files.
 */

public class EverythingCompactionStrategy extends CompactionStrategy {

  @Override
  public boolean shouldCompact(MajorCompactionRequest request) throws IOException {
    return true; // ACCUMULO-3645 compact for empty files too
  }

  @Override
  public CompactionPlan getCompactionPlan(MajorCompactionRequest request) throws IOException {
    CompactionPlan plan = new CompactionPlan();
    plan.inputFiles.addAll(request.getFiles().keySet());
    return plan;
  }
}

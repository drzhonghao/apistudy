import org.apache.lucene.benchmark.byTask.tasks.*;



import java.util.List;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.stats.Report;
import org.apache.lucene.benchmark.byTask.stats.TaskStats;

/**
 * Report by-name-prefix statistics with no aggregations.
 * <br>Other side effects: None.
 */
public class RepSelectByPrefTask extends RepSumByPrefTask {

  public RepSelectByPrefTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws Exception {
    Report rp = reportSelectByPrefix(getRunData().getPoints().taskStats());
    
    System.out.println();
    System.out.println("------------> Report Select By Prefix ("+prefix+") ("+
        rp.getSize()+" about "+rp.getReported()+" out of "+rp.getOutOf()+")");
    System.out.println(rp.getText());
    System.out.println();

    return 0;
  }
  
  protected Report reportSelectByPrefix(List<TaskStats> taskStats) {
    String longestOp = longestOp(taskStats);
    boolean first = true;
    StringBuilder sb = new StringBuilder();
    sb.append(tableTitle(longestOp));
    sb.append(newline);
    int reported = 0;
    for (final TaskStats stat : taskStats) {
      if (stat.getElapsed()>=0 && stat.getTask().getName().startsWith(prefix)) { // only ended tasks with proper name
        reported++;
        if (!first) {
          sb.append(newline);
        }
        first = false;
        String line = taskReportLine(longestOp,stat);
        if (taskStats.size()>2 && reported%2==0) {
          line = line.replaceAll("   "," - ");
        }
        sb.append(line);
      }
    }
    String reptxt = (reported==0 ? "No Matching Entries Were Found!" : sb.toString());
    return new Report(reptxt,reported,reported, taskStats.size());
  }

}

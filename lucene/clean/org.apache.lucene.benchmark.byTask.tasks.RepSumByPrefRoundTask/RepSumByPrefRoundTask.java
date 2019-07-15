import org.apache.lucene.benchmark.byTask.tasks.*;



import java.util.LinkedHashMap;
import java.util.List;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.stats.Report;
import org.apache.lucene.benchmark.byTask.stats.TaskStats;

/**
 * Report all prefix matching statistics grouped/aggregated by name and round.
 * <br>Other side effects: None.
 */
public class RepSumByPrefRoundTask extends RepSumByPrefTask {

  public RepSumByPrefRoundTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws Exception {
    Report rp = reportSumByPrefixRound(getRunData().getPoints().taskStats());
    
    System.out.println();
    System.out.println("------------> Report sum by Prefix ("+prefix+") and Round ("+
        rp.getSize()+" about "+rp.getReported()+" out of "+rp.getOutOf()+")");
    System.out.println(rp.getText());
    System.out.println();

    return 0;
  }

  protected Report reportSumByPrefixRound(List<TaskStats> taskStats) {
    // aggregate by task name and by round
    int reported = 0;
    LinkedHashMap<String,TaskStats> p2 = new LinkedHashMap<>();
    for (final TaskStats stat1 : taskStats) {
      if (stat1.getElapsed()>=0 && stat1.getTask().getName().startsWith(prefix)) { // only ended tasks with proper name
        reported++;
        String name = stat1.getTask().getName();
        String rname = stat1.getRound()+"."+name; // group by round
        TaskStats stat2 = p2.get(rname);
        if (stat2 == null) {
          try {
            stat2 = stat1.clone();
          } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
          }
          p2.put(rname,stat2);
        } else {
          stat2.add(stat1);
        }
      }
    }
    // now generate report from secondary list p2    
    return genPartialReport(reported, p2, taskStats.size());
  }


}

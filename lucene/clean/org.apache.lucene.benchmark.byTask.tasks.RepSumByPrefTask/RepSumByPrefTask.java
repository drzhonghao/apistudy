import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.stats.Report;
import org.apache.lucene.benchmark.byTask.stats.TaskStats;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Report by-name-prefix statistics aggregated by name.
 * <br>Other side effects: None.
 */
public class RepSumByPrefTask extends ReportTask {

  public RepSumByPrefTask(PerfRunData runData) {
    super(runData);
  }

  protected String prefix;

  @Override
  public int doLogic() throws Exception {
    Report rp = reportSumByPrefix(getRunData().getPoints().taskStats());
    
    System.out.println();
    System.out.println("------------> Report Sum By Prefix ("+prefix+") ("+
        rp.getSize()+" about "+rp.getReported()+" out of "+rp.getOutOf()+")");
    System.out.println(rp.getText());
    System.out.println();

    return 0;
  }

  protected Report reportSumByPrefix (List<TaskStats> taskStats) {
    // aggregate by task name
    int reported = 0;
    LinkedHashMap<String,TaskStats> p2 = new LinkedHashMap<>();
    for (final TaskStats stat1 : taskStats) {
      if (stat1.getElapsed()>=0 && stat1.getTask().getName().startsWith(prefix)) { // only ended tasks with proper name
        reported++;
        String name = stat1.getTask().getName();
        TaskStats stat2 = p2.get(name);
        if (stat2 == null) {
          try {
            stat2 = stat1.clone();
          } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
          }
          p2.put(name,stat2);
        } else {
          stat2.add(stat1);
        }
      }
    }
    // now generate report from secondary list p2    
    return genPartialReport(reported, p2, taskStats.size());
  }
  

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  /* (non-Javadoc)
   * @see PerfTask#toString()
   */
  @Override
  public String toString() {
    return super.toString()+" "+prefix;
  }

}

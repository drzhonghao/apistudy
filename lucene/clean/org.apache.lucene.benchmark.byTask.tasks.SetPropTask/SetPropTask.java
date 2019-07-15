import org.apache.lucene.benchmark.byTask.tasks.*;


import org.apache.lucene.benchmark.byTask.PerfRunData;


/**
 * Set a performance test configuration property.
 * A property may have a single value, or a sequence of values, separated by ":". 
 * If a sequence of values is specified, each time a new round starts, 
 * the next (cyclic) value is taken.  
 * <br>Other side effects: none.
 * <br>Takes mandatory param: "name,value" pair. 
 * @see org.apache.lucene.benchmark.byTask.tasks.NewRoundTask
 */
public class SetPropTask extends PerfTask {

  public SetPropTask(PerfRunData runData) {
    super(runData);
  }

  private String name;
  private String value;
  
  @Override
  public int doLogic() throws Exception {
    if (name==null || value==null) {
      throw new Exception(getName()+" - undefined name or value: name="+name+" value="+value);
    }
    getRunData().getConfig().set(name,value);
    return 0;
  }

  /**
   * Set the params (property name and value).
   * @param params property name and value separated by ','.
   */
  @Override
  public void setParams(String params) {
    super.setParams(params);
    int k = params.indexOf(",");
    name = params.substring(0,k).trim();
    value = params.substring(k+1).trim();
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.benchmark.byTask.tasks.PerfTask#supportsParams()
   */
  @Override
  public boolean supportsParams() {
    return true;
  }

}

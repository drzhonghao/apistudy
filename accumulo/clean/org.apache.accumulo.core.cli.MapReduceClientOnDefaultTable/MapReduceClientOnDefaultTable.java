import org.apache.accumulo.core.cli.MapReduceClientOpts;
import org.apache.accumulo.core.cli.*;


import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.hadoop.mapreduce.Job;

import com.beust.jcommander.Parameter;

public class MapReduceClientOnDefaultTable extends MapReduceClientOpts {
  @Parameter(names = "--table", description = "table to use")
  public String tableName;

  public MapReduceClientOnDefaultTable(String table) {
    this.tableName = table;
  }

  public String getTableName() {
    return tableName;
  }

  @Override
  public void setAccumuloConfigs(Job job) throws AccumuloSecurityException {
    super.setAccumuloConfigs(job);
    final String tableName = getTableName();
    final String principal = getPrincipal();
    final AuthenticationToken token = getToken();
    AccumuloInputFormat.setConnectorInfo(job, principal, token);
    AccumuloInputFormat.setInputTableName(job, tableName);
    AccumuloInputFormat.setScanAuthorizations(job, auths);
    AccumuloOutputFormat.setConnectorInfo(job, principal, token);
    AccumuloOutputFormat.setCreateTables(job, true);
    AccumuloOutputFormat.setDefaultTableName(job, tableName);
  }

}

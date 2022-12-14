import org.apache.accumulo.core.cli.MapReduceClientOpts;
import org.apache.accumulo.core.cli.*;


import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.hadoop.mapreduce.Job;

import com.beust.jcommander.Parameter;

public class MapReduceClientOnRequiredTable extends MapReduceClientOpts {

  @Parameter(names = {"-t", "--table"}, required = true, description = "table to use")
  private String tableName;

  @Parameter(names = {"-tf", "--tokenFile"},
      description = "User's token file in HDFS created with \"bin/accumulo create-token\"")
  private String tokenFile = "";

  @Override
  public void setAccumuloConfigs(Job job) throws AccumuloSecurityException {
    super.setAccumuloConfigs(job);

    final String principal = getPrincipal(), tableName = getTableName();

    if (tokenFile.isEmpty()) {
      AuthenticationToken token = getToken();
      AccumuloInputFormat.setConnectorInfo(job, principal, token);
      AccumuloOutputFormat.setConnectorInfo(job, principal, token);
    } else {
      AccumuloInputFormat.setConnectorInfo(job, principal, tokenFile);
      AccumuloOutputFormat.setConnectorInfo(job, principal, tokenFile);
    }
    AccumuloInputFormat.setInputTableName(job, tableName);
    AccumuloInputFormat.setScanAuthorizations(job, auths);
    AccumuloOutputFormat.setCreateTables(job, true);
    AccumuloOutputFormat.setDefaultTableName(job, tableName);
  }

  public String getTableName() {
    return tableName;
  }
}

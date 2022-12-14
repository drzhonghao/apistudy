import org.apache.accumulo.examples.simple.helloworld.*;


import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.accumulo.core.cli.ClientOnRequiredTable;
import org.apache.accumulo.core.cli.ScannerOpts;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

/**
 * Reads all data between two rows; all data after a given row; or all data in a table, depending on
 * the number of arguments given.
 */
public class ReadData {

  private static final Logger log = LoggerFactory.getLogger(ReadData.class);

  static class Opts extends ClientOnRequiredTable {
    @Parameter(names = "--startKey")
    String startKey;
    @Parameter(names = "--endKey")
    String endKey;
  }

  public static void main(String[] args)
      throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
    Opts opts = new Opts();
    ScannerOpts scanOpts = new ScannerOpts();
    opts.parseArgs(ReadData.class.getName(), args, scanOpts);

    Connector connector = opts.getConnector();

    Scanner scan = connector.createScanner(opts.getTableName(), opts.auths);
    scan.setBatchSize(scanOpts.scanBatchSize);
    Key start = null;
    if (opts.startKey != null)
      start = new Key(new Text(opts.startKey));
    Key end = null;
    if (opts.endKey != null)
      end = new Key(new Text(opts.endKey));
    scan.setRange(new Range(start, end));
    Iterator<Entry<Key,Value>> iter = scan.iterator();

    while (iter.hasNext()) {
      Entry<Key,Value> e = iter.next();
      Text colf = e.getKey().getColumnFamily();
      Text colq = e.getKey().getColumnQualifier();
      log.trace("row: " + e.getKey().getRow() + ", colf: " + colf + ", colq: " + colq);
      log.trace(", value: " + e.getValue().toString());
    }
  }
}

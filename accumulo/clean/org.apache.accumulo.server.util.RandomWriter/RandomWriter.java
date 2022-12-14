import org.apache.accumulo.server.util.*;


import java.util.Iterator;
import java.util.Random;

import org.apache.accumulo.core.cli.BatchWriterOpts;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.server.cli.ClientOnDefaultTable;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

public class RandomWriter {

  private static String table_name = "test_write_table";
  private static int num_columns_per_row = 1;
  private static int num_payload_bytes = 1024;
  private static final Logger log = LoggerFactory.getLogger(RandomWriter.class);

  public static class RandomMutationGenerator implements Iterable<Mutation>, Iterator<Mutation> {
    private long max_mutations;
    private int mutations_so_far = 0;
    private Random r = new Random();
    private static final Logger log = LoggerFactory.getLogger(RandomMutationGenerator.class);

    public RandomMutationGenerator(long num_mutations) {
      max_mutations = num_mutations;
    }

    @Override
    public boolean hasNext() {
      return mutations_so_far < max_mutations;
    }

    @Override
    public Mutation next() {
      Text row_value = new Text(
          Long.toString(((r.nextLong() & 0x7fffffffffffffffl) / 177) % 100000000000l));
      Mutation m = new Mutation(row_value);
      for (int column = 0; column < num_columns_per_row; column++) {
        Text column_fam = new Text("col_fam");
        byte[] bytes = new byte[num_payload_bytes];
        r.nextBytes(bytes);
        m.put(column_fam, new Text("" + column), new Value(bytes));
      }
      mutations_so_far++;
      if (mutations_so_far % 1000000 == 0) {
        log.info("Created " + mutations_so_far + " mutations so far");
      }
      return m;
    }

    @Override
    public void remove() {
      mutations_so_far++;
    }

    @Override
    public Iterator<Mutation> iterator() {
      return this;
    }
  }

  static class Opts extends ClientOnDefaultTable {
    @Parameter(names = "--count", description = "number of mutations to write", required = true)
    long count;

    Opts(String table) {
      super(table);
    }
  }

  public static void main(String[] args) throws Exception {
    Opts opts = new Opts(table_name);
    BatchWriterOpts bwOpts = new BatchWriterOpts();
    opts.parseArgs(RandomWriter.class.getName(), args, bwOpts);

    long start = System.currentTimeMillis();
    log.info("starting at " + start + " for user " + opts.getPrincipal());
    try {
      Connector connector = opts.getConnector();
      BatchWriter bw = connector.createBatchWriter(opts.getTableName(),
          bwOpts.getBatchWriterConfig());
      log.info("Writing " + opts.count + " mutations...");
      bw.addMutations(new RandomMutationGenerator(opts.count));
      bw.close();
    } catch (Exception e) {
      log.error("{}", e.getMessage(), e);
      throw e;
    }
    long stop = System.currentTimeMillis();

    log.info("stopping at " + stop);
    log.info("elapsed: " + (((double) stop - (double) start) / 1000.0));
  }

}

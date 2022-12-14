import org.apache.accumulo.shell.format.*;


import java.io.IOException;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.data.ConstraintViolationSummary;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.accumulo.core.util.format.FormatterConfig;
import org.apache.accumulo.shell.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleterFormatter extends DefaultFormatter {

  private static final Logger log = LoggerFactory.getLogger(DeleterFormatter.class);
  private BatchWriter writer;
  private Shell shellState;
  private boolean force;
  private boolean more;

  public DeleterFormatter(BatchWriter writer, Iterable<Entry<Key,Value>> scanner,
      FormatterConfig config, Shell shellState, boolean force) {
    super.initialize(scanner, config);
    this.writer = writer;
    this.shellState = shellState;
    this.force = force;
    this.more = true;
  }

  @Override
  public boolean hasNext() {
    if (!getScannerIterator().hasNext() || !more) {
      try {
        writer.close();
      } catch (MutationsRejectedException e) {
        log.error(e.toString());
        if (Shell.isDebuggingEnabled())
          for (ConstraintViolationSummary cvs : e.getConstraintViolationSummaries())
            log.trace(cvs.toString());
      }
      return false;
    }
    return true;
  }

  /**
   * @return null, because the iteration will provide prompts and handle deletes internally.
   */
  @Override
  public String next() {
    Entry<Key,Value> next = getScannerIterator().next();
    Key key = next.getKey();
    Mutation m = new Mutation(key.getRow());
    String entryStr = formatEntry(next, isDoTimestamps());
    boolean delete = force;
    try {
      if (!force) {
        shellState.getReader().flush();
        String line = shellState.getReader().readLine("Delete { " + entryStr + " } ? ");
        more = line != null;
        delete = line != null && (line.equalsIgnoreCase("y") || line.equalsIgnoreCase("yes"));
      }
      if (delete) {
        m.putDelete(key.getColumnFamily(), key.getColumnQualifier(),
            new ColumnVisibility(key.getColumnVisibility()), key.getTimestamp());
        try {
          writer.addMutation(m);
        } catch (MutationsRejectedException e) {
          log.error(e.toString());
          if (Shell.isDebuggingEnabled())
            for (ConstraintViolationSummary cvs : e.getConstraintViolationSummaries())
              log.trace(cvs.toString());
        }
      }
      shellState.getReader()
          .print(String.format("[%s] %s%n", delete ? "DELETED" : "SKIPPED", entryStr));
    } catch (IOException e) {
      log.error("Cannot write to console", e);
      throw new RuntimeException(e);
    }
    return null;
  }
}

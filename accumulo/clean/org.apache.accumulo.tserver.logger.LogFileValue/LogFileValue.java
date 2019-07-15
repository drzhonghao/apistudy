import org.apache.accumulo.tserver.logger.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.accumulo.core.data.ColumnUpdate;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.server.data.ServerMutation;
import org.apache.hadoop.io.Writable;

public class LogFileValue implements Writable {

  private static final List<Mutation> empty = Collections.emptyList();

  public List<Mutation> mutations = empty;

  @Override
  public void readFields(DataInput in) throws IOException {
    int count = in.readInt();
    mutations = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      ServerMutation mutation = new ServerMutation();
      mutation.readFields(in);
      mutations.add(mutation);
    }
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeInt(mutations.size());
    for (Mutation m : mutations) {
      m.write(out);
    }
  }

  private static String displayLabels(byte[] labels) {
    String s = new String(labels, UTF_8);
    s = s.replace("&", " & ");
    s = s.replace("|", " | ");
    return s;
  }

  public static String format(LogFileValue lfv, int maxMutations) {
    if (lfv.mutations.size() == 0)
      return "";
    StringBuilder builder = new StringBuilder();
    builder.append(lfv.mutations.size() + " mutations:\n");
    int i = 0;
    for (Mutation m : lfv.mutations) {
      if (i++ >= maxMutations) {
        builder.append("...");
        break;
      }
      builder.append("  ").append(new String(m.getRow(), UTF_8)).append("\n");
      for (ColumnUpdate update : m.getUpdates()) {
        String value = new String(update.getValue());
        builder.append("      ").append(new String(update.getColumnFamily(), UTF_8)).append(":")
            .append(new String(update.getColumnQualifier(), UTF_8)).append(" ")
            .append(update.hasTimestamp() ? "[user]:" : "[system]:").append(update.getTimestamp())
            .append(" [").append(displayLabels(update.getColumnVisibility())).append("] ")
            .append(update.isDeleted() ? "<deleted>" : value).append("\n");
      }
    }
    return builder.toString();
  }

  @Override
  public String toString() {
    return format(this, 5);
  }

}

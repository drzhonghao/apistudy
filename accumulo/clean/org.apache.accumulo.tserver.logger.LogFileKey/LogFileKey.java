import org.apache.accumulo.tserver.logger.*;


import static org.apache.accumulo.tserver.logger.LogEvents.DEFINE_TABLET;
import static org.apache.accumulo.tserver.logger.LogEvents.MANY_MUTATIONS;
import static org.apache.accumulo.tserver.logger.LogEvents.MUTATION;
import static org.apache.accumulo.tserver.logger.LogEvents.OPEN;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.hadoop.io.WritableComparable;

public class LogFileKey implements WritableComparable<LogFileKey> {

  public LogEvents event;
  public String filename = null;
  public KeyExtent tablet = null;
  public long seq = -1;
  public int tabletId = -1;
  public static final int VERSION = 2;
  public String tserverSession;

  @Override
  public void readFields(DataInput in) throws IOException {
    int value = in.readByte();
    if (value >= LogEvents.values().length) {
      throw new IOException("Invalid LogEvent type, got ordinal " + value + ", but only know about "
          + LogEvents.values().length + " possible types.");
    }
    event = LogEvents.values()[value];
    switch (event) {
      case OPEN:
        tabletId = in.readInt();
        tserverSession = in.readUTF();
        if (tabletId != VERSION) {
          throw new RuntimeException(String.format(
              "Bad version number for log file: expected %d, but saw %d", VERSION, tabletId));
        }
        break;
      case COMPACTION_FINISH:
        seq = in.readLong();
        tabletId = in.readInt();
        break;
      case COMPACTION_START:
        seq = in.readLong();
        tabletId = in.readInt();
        filename = in.readUTF();
        break;
      case DEFINE_TABLET:
        seq = in.readLong();
        tabletId = in.readInt();
        tablet = new KeyExtent();
        tablet.readFields(in);
        break;
      case MANY_MUTATIONS:
        seq = in.readLong();
        tabletId = in.readInt();
        break;
      case MUTATION:
        seq = in.readLong();
        tabletId = in.readInt();
        break;
      default:
        throw new RuntimeException("Unknown log event type: " + event);
    }

  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeByte(event.ordinal());
    switch (event) {
      case OPEN:
        seq = -1;
        tabletId = -1;
        out.writeInt(VERSION);
        out.writeUTF(tserverSession);
        // out.writeUTF(Accumulo.getInstanceID());
        break;
      case COMPACTION_FINISH:
        out.writeLong(seq);
        out.writeInt(tabletId);
        break;
      case COMPACTION_START:
        out.writeLong(seq);
        out.writeInt(tabletId);
        out.writeUTF(filename);
        break;
      case DEFINE_TABLET:
        out.writeLong(seq);
        out.writeInt(tabletId);
        tablet.write(out);
        break;
      case MANY_MUTATIONS:
        out.writeLong(seq);
        out.writeInt(tabletId);
        break;
      case MUTATION:
        out.writeLong(seq);
        out.writeInt(tabletId);
        break;
      default:
        throw new IllegalArgumentException("Bad value for LogFileEntry type");
    }
  }

  static int eventType(LogEvents event) {
    // Order logs by START, TABLET_DEFINITIONS, COMPACTIONS and then MUTATIONS
    if (event == MUTATION || event == MANY_MUTATIONS) {
      return 3;
    }
    if (event == DEFINE_TABLET) {
      return 1;
    }
    if (event == OPEN) {
      return 0;
    }
    return 2;
  }

  private static int sign(long l) {
    if (l < 0)
      return -1;
    if (l > 0)
      return 1;
    return 0;
  }

  @Override
  public int compareTo(LogFileKey o) {
    if (eventType(this.event) != eventType(o.event)) {
      return eventType(this.event) - eventType(o.event);
    }
    if (this.event == OPEN)
      return 0;
    if (this.tabletId != o.tabletId) {
      return this.tabletId - o.tabletId;
    }
    return sign(this.seq - o.seq);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LogFileKey) {
      return this.compareTo((LogFileKey) obj) == 0;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (int) seq;
  }

  @Override
  public String toString() {
    switch (event) {
      case OPEN:
        return String.format("OPEN %s", tserverSession);
      case COMPACTION_FINISH:
        return String.format("COMPACTION_FINISH %d %d", tabletId, seq);
      case COMPACTION_START:
        return String.format("COMPACTION_START %d %d %s", tabletId, seq, filename);
      case MUTATION:
        return String.format("MUTATION %d %d", tabletId, seq);
      case MANY_MUTATIONS:
        return String.format("MANY_MUTATIONS %d %d", tabletId, seq);
      case DEFINE_TABLET:
        return String.format("DEFINE_TABLET %d %d %s", tabletId, seq, tablet);
    }
    throw new RuntimeException("Unknown type of entry: " + event);
  }
}

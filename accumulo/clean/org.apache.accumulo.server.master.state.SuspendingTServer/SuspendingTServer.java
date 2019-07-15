import org.apache.accumulo.server.master.state.*;


import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.SuspendLocationColumn.SUSPEND_COLUMN;

import java.util.Objects;

import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.util.HostAndPort;

/** For a suspended tablet, the time of suspension and the server it was suspended from. */
public class SuspendingTServer {
  public final HostAndPort server;
  public final long suspensionTime;

  SuspendingTServer(HostAndPort server, long suspensionTime) {
    this.server = Objects.requireNonNull(server);
    this.suspensionTime = suspensionTime;
  }

  public static SuspendingTServer fromValue(Value value) {
    String valStr = value.toString();
    String[] parts = valStr.split("[|]", 2);
    return new SuspendingTServer(HostAndPort.fromString(parts[0]), Long.parseLong(parts[1]));
  }

  public Value toValue() {
    return new Value(server.toString() + "|" + suspensionTime);
  }

  @Override
  public boolean equals(Object rhsObject) {
    if (!(rhsObject instanceof SuspendingTServer)) {
      return false;
    }
    SuspendingTServer rhs = (SuspendingTServer) rhsObject;
    return server.equals(rhs.server) && suspensionTime == rhs.suspensionTime;
  }

  public void setSuspension(Mutation m) {
    m.put(SUSPEND_COLUMN.getColumnFamily(), SUSPEND_COLUMN.getColumnQualifier(), toValue());
  }

  public static void clearSuspension(Mutation m) {
    m.putDelete(SUSPEND_COLUMN.getColumnFamily(), SUSPEND_COLUMN.getColumnQualifier());
  }

  @Override
  public int hashCode() {
    return Objects.hash(server, suspensionTime);
  }

  @Override
  public String toString() {
    return server.toString() + "[" + suspensionTime + "]";
  }
}

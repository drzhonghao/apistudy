import org.apache.accumulo.tserver.mastermessage.*;


import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.master.thrift.MasterClientService.Iface;
import org.apache.accumulo.core.master.thrift.TabletLoadState;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.thrift.TException;

public class TabletStatusMessage implements MasterMessage {

  private KeyExtent extent;
  private TabletLoadState status;

  public TabletStatusMessage(TabletLoadState status, KeyExtent extent) {
    this.extent = extent;
    this.status = status;
  }

  @Override
  public void send(TCredentials auth, String serverName, Iface client)
      throws TException, ThriftSecurityException {
    client.reportTabletStatus(Tracer.traceInfo(), auth, serverName, status, extent.toThrift());
  }
}

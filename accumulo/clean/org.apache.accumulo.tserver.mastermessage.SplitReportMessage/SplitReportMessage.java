import org.apache.accumulo.tserver.mastermessage.*;


import java.util.Map;
import java.util.TreeMap;

import org.apache.accumulo.core.client.impl.Translator;
import org.apache.accumulo.core.client.impl.Translators;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.master.thrift.MasterClientService;
import org.apache.accumulo.core.master.thrift.TabletSplit;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.hadoop.io.Text;
import org.apache.thrift.TException;

public class SplitReportMessage implements MasterMessage {
  private Map<KeyExtent,Text> extents;
  private KeyExtent old_extent;

  public SplitReportMessage(KeyExtent old_extent, KeyExtent ne1, Text np1, KeyExtent ne2,
      Text np2) {
    this.old_extent = old_extent;
    extents = new TreeMap<>();
    extents.put(ne1, np1);
    extents.put(ne2, np2);
  }

  @Override
  public void send(TCredentials credentials, String serverName, MasterClientService.Iface client)
      throws TException, ThriftSecurityException {
    TabletSplit split = new TabletSplit();
    split.oldTablet = old_extent.toThrift();
    split.newTablets = Translator.translate(extents.keySet(), Translators.KET);
    client.reportSplitExtent(Tracer.traceInfo(), credentials, serverName, split);
  }

}

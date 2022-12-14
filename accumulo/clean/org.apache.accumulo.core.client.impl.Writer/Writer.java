import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.*;


import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.accumulo.fate.util.UtilWaitThread.sleepUninterruptibly;

import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocation;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.tabletserver.thrift.ConstraintViolationException;
import org.apache.accumulo.core.tabletserver.thrift.NotServingTabletException;
import org.apache.accumulo.core.tabletserver.thrift.TDurability;
import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.hadoop.io.Text;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Writer {

  private static final Logger log = LoggerFactory.getLogger(Writer.class);

  private ClientContext context;
  private String tableId;

  public Writer(ClientContext context, String tableId) {
    checkArgument(context != null, "context is null");
    checkArgument(tableId != null, "tableId is null");
    this.context = context;
    this.tableId = tableId;
  }

  private static void updateServer(ClientContext context, Mutation m, KeyExtent extent,
      HostAndPort server) throws TException, NotServingTabletException,
      ConstraintViolationException, AccumuloSecurityException {
    checkArgument(m != null, "m is null");
    checkArgument(extent != null, "extent is null");
    checkArgument(server != null, "server is null");
    checkArgument(context != null, "context is null");

    TabletClientService.Iface client = null;
    try {
      client = ThriftUtil.getTServerClient(server, context);
      client.update(Tracer.traceInfo(), context.rpcCreds(), extent.toThrift(), m.toThrift(),
          TDurability.DEFAULT);
      return;
    } catch (ThriftSecurityException e) {
      throw new AccumuloSecurityException(e.user, e.code);
    } finally {
      ThriftUtil.returnClient((TServiceClient) client);
    }
  }

  public void update(Mutation m) throws AccumuloException, AccumuloSecurityException,
      ConstraintViolationException, TableNotFoundException {
    checkArgument(m != null, "m is null");

    if (m.size() == 0)
      throw new IllegalArgumentException("Can not add empty mutations");

    while (true) {
      TabletLocation tabLoc = TabletLocator.getLocator(context, tableId).locateTablet(context,
          new Text(m.getRow()), false, true);

      if (tabLoc == null) {
        log.trace("No tablet location found for row " + new String(m.getRow(), UTF_8));
        sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        continue;
      }

      final HostAndPort parsedLocation = HostAndPort.fromString(tabLoc.tablet_location);
      try {
        updateServer(context, m, tabLoc.tablet_extent, parsedLocation);
        return;
      } catch (NotServingTabletException e) {
        log.trace("Not serving tablet, server = " + parsedLocation);
        TabletLocator.getLocator(context, tableId).invalidateCache(tabLoc.tablet_extent);
      } catch (ConstraintViolationException cve) {
        log.error("error sending update to " + parsedLocation + ": " + cve);
        // probably do not need to invalidate cache, but it does not hurt
        TabletLocator.getLocator(context, tableId).invalidateCache(tabLoc.tablet_extent);
        throw cve;
      } catch (TException e) {
        log.error("error sending update to " + parsedLocation + ": " + e);
        TabletLocator.getLocator(context, tableId).invalidateCache(tabLoc.tablet_extent);
      }

      sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
    }

  }
}

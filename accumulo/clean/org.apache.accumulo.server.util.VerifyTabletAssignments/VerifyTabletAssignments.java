import org.apache.accumulo.server.util.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.Credentials;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.InitialMultiScan;
import org.apache.accumulo.core.data.thrift.IterInfo;
import org.apache.accumulo.core.data.thrift.MultiScanResult;
import org.apache.accumulo.core.data.thrift.TColumn;
import org.apache.accumulo.core.data.thrift.TKeyExtent;
import org.apache.accumulo.core.data.thrift.TRange;
import org.apache.accumulo.core.metadata.MetadataServicer;
import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.tabletserver.thrift.NoSuchScanIDException;
import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.core.trace.thrift.TInfo;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.server.cli.ClientOpts;
import org.apache.hadoop.io.Text;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

public class VerifyTabletAssignments {
  private static final Logger log = LoggerFactory.getLogger(VerifyTabletAssignments.class);

  static class Opts extends ClientOpts {
    @Parameter(names = {"-v", "--verbose"},
        description = "verbose mode (prints locations of tablets)")
    boolean verbose = false;
  }

  public static void main(String[] args) throws Exception {
    Opts opts = new Opts();
    opts.parseArgs(VerifyTabletAssignments.class.getName(), args);

    ClientContext context = new ClientContext(opts.getInstance(),
        new Credentials(opts.getPrincipal(), opts.getToken()), opts.getClientConfiguration());
    Connector conn = opts.getConnector();
    for (String table : conn.tableOperations().list())
      checkTable(context, opts, table, null);

  }

  private static void checkTable(final ClientContext context, final Opts opts, String tableName,
      HashSet<KeyExtent> check) throws AccumuloException, AccumuloSecurityException,
      TableNotFoundException, InterruptedException {

    if (check == null)
      System.out.println("Checking table " + tableName);
    else
      System.out.println("Checking table " + tableName + " again, failures " + check.size());

    TreeMap<KeyExtent,String> tabletLocations = new TreeMap<>();

    String tableId = Tables.getNameToIdMap(context.getInstance()).get(tableName);
    MetadataServicer.forTableId(context, tableId).getTabletLocations(tabletLocations);

    final HashSet<KeyExtent> failures = new HashSet<>();

    Map<HostAndPort,List<KeyExtent>> extentsPerServer = new TreeMap<>();

    for (Entry<KeyExtent,String> entry : tabletLocations.entrySet()) {
      KeyExtent keyExtent = entry.getKey();
      String loc = entry.getValue();
      if (loc == null)
        System.out.println(" Tablet " + keyExtent + " has no location");
      else if (opts.verbose)
        System.out.println(" Tablet " + keyExtent + " is located at " + loc);

      if (loc != null) {
        final HostAndPort parsedLoc = HostAndPort.fromString(loc);
        List<KeyExtent> extentList = extentsPerServer.get(parsedLoc);
        if (extentList == null) {
          extentList = new ArrayList<>();
          extentsPerServer.put(parsedLoc, extentList);
        }

        if (check == null || check.contains(keyExtent))
          extentList.add(keyExtent);
      }
    }

    ExecutorService tp = Executors.newFixedThreadPool(20);
    for (final Entry<HostAndPort,List<KeyExtent>> entry : extentsPerServer.entrySet()) {
      Runnable r = new Runnable() {

        @Override
        public void run() {
          try {
            checkTabletServer(context, entry, failures);
          } catch (Exception e) {
            log.error("Failure on tablet server '" + entry.getKey() + ".", e);
            failures.addAll(entry.getValue());
          }
        }

      };

      tp.execute(r);
    }

    tp.shutdown();

    while (!tp.awaitTermination(1, TimeUnit.HOURS)) {}

    if (failures.size() > 0)
      checkTable(context, opts, tableName, failures);
  }

  private static void checkFailures(HostAndPort server, HashSet<KeyExtent> failures,
      MultiScanResult scanResult) {
    for (TKeyExtent tke : scanResult.failures.keySet()) {
      KeyExtent ke = new KeyExtent(tke);
      System.out.println(" Tablet " + ke + " failed at " + server);
      failures.add(ke);
    }
  }

  private static void checkTabletServer(ClientContext context,
      Entry<HostAndPort,List<KeyExtent>> entry, HashSet<KeyExtent> failures)
      throws ThriftSecurityException, TException, NoSuchScanIDException {
    TabletClientService.Iface client = ThriftUtil.getTServerClient(entry.getKey(), context);

    Map<TKeyExtent,List<TRange>> batch = new TreeMap<>();

    for (KeyExtent keyExtent : entry.getValue()) {
      Text row = keyExtent.getEndRow();
      Text row2 = null;

      if (row == null) {
        row = keyExtent.getPrevEndRow();

        if (row != null) {
          row = new Text(row);
          row.append(new byte[] {'a'}, 0, 1);
        } else {
          row = new Text("1234567890");
        }

        row2 = new Text(row);
        row2.append(new byte[] {'!'}, 0, 1);
      } else {
        row = new Text(row);
        row2 = new Text(row);

        row.getBytes()[row.getLength() - 1] = (byte) (row.getBytes()[row.getLength() - 1] - 1);
      }

      Range r = new Range(row, true, row2, false);
      batch.put(keyExtent.toThrift(), Collections.singletonList(r.toThrift()));
    }
    TInfo tinfo = Tracer.traceInfo();
    Map<String,Map<String,String>> emptyMapSMapSS = Collections.emptyMap();
    List<IterInfo> emptyListIterInfo = Collections.emptyList();
    List<TColumn> emptyListColumn = Collections.emptyList();
    InitialMultiScan is = client.startMultiScan(tinfo, context.rpcCreds(), batch, emptyListColumn,
        emptyListIterInfo, emptyMapSMapSS, Authorizations.EMPTY.getAuthorizationsBB(), false, null,
        0L, null);
    if (is.result.more) {
      MultiScanResult result = client.continueMultiScan(tinfo, is.scanID);
      checkFailures(entry.getKey(), failures, result);

      while (result.more) {
        result = client.continueMultiScan(tinfo, is.scanID);
        checkFailures(entry.getKey(), failures, result);
      }
    }

    client.closeMultiScan(tinfo, is.scanID);

    ThriftUtil.returnClient((TServiceClient) client);
  }
}

import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.*;


import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.accumulo.fate.util.UtilWaitThread.sleepUninterruptibly;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.thrift.ClientService;
import org.apache.accumulo.core.client.impl.thrift.ClientService.Client;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.util.ServerServices;
import org.apache.accumulo.core.util.ServerServices.Service;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooCache;
import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerClient {
  private static final Logger log = LoggerFactory.getLogger(ServerClient.class);

  public static <T> T execute(ClientContext context, ClientExecReturn<T,ClientService.Client> exec)
      throws AccumuloException, AccumuloSecurityException {
    try {
      return executeRaw(context, exec);
    } catch (ThriftSecurityException e) {
      throw new AccumuloSecurityException(e.user, e.code, e);
    } catch (AccumuloException e) {
      throw e;
    } catch (Exception e) {
      throw new AccumuloException(e);
    }
  }

  public static void execute(ClientContext context, ClientExec<ClientService.Client> exec)
      throws AccumuloException, AccumuloSecurityException {
    try {
      executeRaw(context, exec);
    } catch (ThriftSecurityException e) {
      throw new AccumuloSecurityException(e.user, e.code, e);
    } catch (AccumuloException e) {
      throw e;
    } catch (Exception e) {
      throw new AccumuloException(e);
    }
  }

  public static <T> T executeRaw(ClientContext context,
      ClientExecReturn<T,ClientService.Client> exec) throws Exception {
    while (true) {
      ClientService.Client client = null;
      String server = null;
      try {
        Pair<String,Client> pair = ServerClient.getConnection(context);
        server = pair.getFirst();
        client = pair.getSecond();
        return exec.execute(client);
      } catch (TTransportException tte) {
        log.debug("ClientService request failed " + server + ", retrying ... ", tte);
        sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
      } finally {
        if (client != null)
          ServerClient.close(client);
      }
    }
  }

  public static void executeRaw(ClientContext context, ClientExec<ClientService.Client> exec)
      throws Exception {
    while (true) {
      ClientService.Client client = null;
      String server = null;
      try {
        Pair<String,Client> pair = ServerClient.getConnection(context);
        server = pair.getFirst();
        client = pair.getSecond();
        exec.execute(client);
        break;
      } catch (TTransportException tte) {
        log.debug("ClientService request failed " + server + ", retrying ... ", tte);
        sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
      } finally {
        if (client != null)
          ServerClient.close(client);
      }
    }
  }

  static volatile boolean warnedAboutTServersBeingDown = false;

  public static Pair<String,ClientService.Client> getConnection(ClientContext context)
      throws TTransportException {
    return getConnection(context, true);
  }

  public static Pair<String,ClientService.Client> getConnection(ClientContext context,
      boolean preferCachedConnections) throws TTransportException {
    return getConnection(context, preferCachedConnections, context.getClientTimeoutInMillis());
  }

  public static Pair<String,ClientService.Client> getConnection(ClientContext context,
      boolean preferCachedConnections, long rpcTimeout) throws TTransportException {
    checkArgument(context != null, "context is null");
    // create list of servers
    ArrayList<ThriftTransportKey> servers = new ArrayList<>();

    // add tservers
    Instance instance = context.getInstance();
    ZooCache zc = new ZooCacheFactory().getZooCache(instance.getZooKeepers(),
        instance.getZooKeepersSessionTimeOut());
    for (String tserver : zc.getChildren(ZooUtil.getRoot(instance) + Constants.ZTSERVERS)) {
      String path = ZooUtil.getRoot(instance) + Constants.ZTSERVERS + "/" + tserver;
      byte[] data = ZooUtil.getLockData(zc, path);
      if (data != null) {
        String strData = new String(data, UTF_8);
        if (!strData.equals("master"))
          servers.add(new ThriftTransportKey(
              new ServerServices(strData).getAddress(Service.TSERV_CLIENT), rpcTimeout, context));
      }
    }

    boolean opened = false;
    try {
      Pair<String,TTransport> pair = ThriftTransportPool.getInstance().getAnyTransport(servers,
          preferCachedConnections);
      ClientService.Client client = ThriftUtil.createClient(new ClientService.Client.Factory(),
          pair.getSecond());
      opened = true;
      warnedAboutTServersBeingDown = false;
      return new Pair<>(pair.getFirst(), client);
    } finally {
      if (!opened) {
        if (!warnedAboutTServersBeingDown) {
          if (servers.isEmpty()) {
            log.warn("There are no tablet servers: check that zookeeper and accumulo are running.");
          } else {
            log.warn("Failed to find an available server in the list of servers: " + servers);
          }
          warnedAboutTServersBeingDown = true;
        }
      }
    }
  }

  public static void close(ClientService.Client client) {
    if (client != null && client.getInputProtocol() != null
        && client.getInputProtocol().getTransport() != null) {
      ThriftTransportPool.getInstance().returnTransport(client.getInputProtocol().getTransport());
    } else {
      log.debug("Attempt to close null connection to a server", new Exception());
    }
  }
}

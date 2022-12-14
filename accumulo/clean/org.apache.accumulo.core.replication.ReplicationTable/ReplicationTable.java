import org.apache.accumulo.core.replication.*;


import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.replication.ReplicationSchema.StatusSection;
import org.apache.accumulo.core.replication.ReplicationSchema.WorkSection;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.DeprecationUtil;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class ReplicationTable {
  private static final Logger log = LoggerFactory.getLogger(ReplicationTable.class);

  public static final String ID = "+rep";
  public static final String NAME = Namespaces.ACCUMULO_NAMESPACE + ".replication";

  public static final String COMBINER_NAME = "statuscombiner";

  public static final String STATUS_LG_NAME = StatusSection.NAME.toString();
  public static final Set<Text> STATUS_LG_COLFAMS = Collections.singleton(StatusSection.NAME);
  public static final String WORK_LG_NAME = WorkSection.NAME.toString();
  public static final Set<Text> WORK_LG_COLFAMS = Collections.singleton(WorkSection.NAME);
  public static final Map<String,Set<Text>> LOCALITY_GROUPS = ImmutableMap.of(STATUS_LG_NAME,
      STATUS_LG_COLFAMS, WORK_LG_NAME, WORK_LG_COLFAMS);

  public static Scanner getScanner(Connector conn) throws ReplicationTableOfflineException {
    try {
      return conn.createScanner(NAME, Authorizations.EMPTY);
    } catch (TableNotFoundException e) {
      throw new AssertionError(NAME + " should exist, but doesn't.");
    } catch (TableOfflineException e) {
      throw new ReplicationTableOfflineException(e);
    }
  }

  public static BatchWriter getBatchWriter(Connector conn) throws ReplicationTableOfflineException {
    try {
      return conn.createBatchWriter(NAME, new BatchWriterConfig());
    } catch (TableNotFoundException e) {
      throw new AssertionError(NAME + " should exist, but doesn't.");
    } catch (TableOfflineException e) {
      throw new ReplicationTableOfflineException(e);
    }
  }

  public static BatchScanner getBatchScanner(Connector conn, int queryThreads)
      throws ReplicationTableOfflineException {
    try {
      return conn.createBatchScanner(NAME, Authorizations.EMPTY, queryThreads);
    } catch (TableNotFoundException e) {
      throw new AssertionError(NAME + " should exist, but doesn't.");
    } catch (TableOfflineException e) {
      throw new ReplicationTableOfflineException(e);
    }
  }

  public static boolean isOnline(Connector conn) {
    return DeprecationUtil.isMockInstance(conn.getInstance())
        || TableState.ONLINE == Tables.getTableState(conn.getInstance(), ID);
  }

  public static void setOnline(Connector conn) throws AccumuloSecurityException, AccumuloException {
    try {
      log.info("Bringing replication table online");
      conn.tableOperations().online(NAME, true);
    } catch (TableNotFoundException e) {
      throw new AssertionError(NAME + " should exist, but doesn't.");
    }
  }

}

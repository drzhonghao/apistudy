import org.apache.accumulo.master.util.*;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.AdminUtil;
import org.apache.accumulo.fate.ReadOnlyStore;
import org.apache.accumulo.fate.ZooStore;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * A utility to administer FATE operations
 */
public class FateAdmin {

  static class TxOpts {
    @Parameter(description = "<txid>...", required = true)
    List<String> txids = new ArrayList<>();
  }

  @Parameters(commandDescription = "Stop an existing FATE by transaction id")
  static class FailOpts extends TxOpts {}

  @Parameters(commandDescription = "Delete an existing FATE by transaction id")
  static class DeleteOpts extends TxOpts {}

  @Parameters(commandDescription = "List the existing FATE transactions")
  static class PrintOpts {}

  public static void main(String[] args) throws Exception {
    Help opts = new Help();
    JCommander jc = new JCommander(opts);
    jc.setProgramName(FateAdmin.class.getName());
    LinkedHashMap<String,TxOpts> txOpts = new LinkedHashMap<>(2);
    txOpts.put("fail", new FailOpts());
    txOpts.put("delete", new DeleteOpts());
    for (Entry<String,TxOpts> entry : txOpts.entrySet()) {
      jc.addCommand(entry.getKey(), entry.getValue());
    }
    jc.addCommand("print", new PrintOpts());
    jc.parse(args);
    if (opts.help || jc.getParsedCommand() == null) {
      jc.usage();
      System.exit(1);
    }

    System.err.printf("This tool has been deprecated%nFATE administration now"
        + " available within 'accumulo shell'%n$ fate fail <txid>... | delete"
        + " <txid>... | print [<txid>...]%n%n");

    AdminUtil<Master> admin = new AdminUtil<>();

    Instance instance = HdfsZooInstance.getInstance();
    String path = ZooUtil.getRoot(instance) + Constants.ZFATE;
    String masterPath = ZooUtil.getRoot(instance) + Constants.ZMASTER_LOCK;
    IZooReaderWriter zk = ZooReaderWriter.getInstance();
    ZooStore<Master> zs = new ZooStore<>(path, zk);

    if (jc.getParsedCommand().equals("fail")) {
      for (String txid : txOpts.get(jc.getParsedCommand()).txids) {
        if (!admin.prepFail(zs, zk, masterPath, txid)) {
          System.exit(1);
        }
      }
    } else if (jc.getParsedCommand().equals("delete")) {
      for (String txid : txOpts.get(jc.getParsedCommand()).txids) {
        if (!admin.prepDelete(zs, zk, masterPath, txid)) {
          System.exit(1);
        }
        admin.deleteLocks(zs, zk, ZooUtil.getRoot(instance) + Constants.ZTABLE_LOCKS, txid);
      }
    } else if (jc.getParsedCommand().equals("print")) {
      admin.print(new ReadOnlyStore<>(zs), zk, ZooUtil.getRoot(instance) + Constants.ZTABLE_LOCKS);
    }
  }
}



import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.impl.AcceptableThriftTableOperationException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.CompactionStrategyConfigUtil;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;
import org.apache.accumulo.server.master.tableOps.UserCompactionConfig;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.commons.codec.binary.Hex;
import org.apache.hadoop.io.BinaryComparable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CompactRange extends MasterRepo {
	private static final Logger log = LoggerFactory.getLogger(CompactRange.class);

	private static final long serialVersionUID = 1L;

	private final String tableId;

	private final String namespaceId;

	private byte[] startRow;

	private byte[] endRow;

	private byte[] config;

	private String getNamespaceId(Master env) throws Exception {
		return Utils.getNamespaceId(env.getInstance(), tableId, TableOperation.COMPACT, this.namespaceId);
	}

	public CompactRange(String namespaceId, String tableId, byte[] startRow, byte[] endRow, List<IteratorSetting> iterators, CompactionStrategyConfig compactionStrategy) throws AcceptableThriftTableOperationException {
		Objects.requireNonNull(namespaceId, "Invalid argument: null namespaceId");
		Objects.requireNonNull(tableId, "Invalid argument: null tableId");
		Objects.requireNonNull(iterators, "Invalid argument: null iterator list");
		Objects.requireNonNull(compactionStrategy, "Invalid argument: null compactionStrategy");
		this.tableId = tableId;
		this.namespaceId = namespaceId;
		this.startRow = ((startRow.length) == 0) ? null : startRow;
		this.endRow = ((endRow.length) == 0) ? null : endRow;
		if (((iterators.size()) > 0) || (!(compactionStrategy.equals(CompactionStrategyConfigUtil.DEFAULT_STRATEGY)))) {
			this.config = WritableUtils.toByteArray(new UserCompactionConfig(this.startRow, this.endRow, iterators, compactionStrategy));
		}else {
			CompactRange.log.info("No iterators or compaction strategy");
		}
		if ((((this.startRow) != null) && ((this.endRow) != null)) && ((new Text(startRow).compareTo(new Text(endRow))) >= 0))
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.COMPACT, TableOperationExceptionType.BAD_RANGE, "start row must be less than end row");

	}

	@Override
	public long isReady(long tid, Master env) throws Exception {
		return (Utils.reserveNamespace(getNamespaceId(env), tid, false, true, TableOperation.COMPACT)) + (Utils.reserveTable(tableId, tid, false, true, TableOperation.COMPACT));
	}

	@Override
	public Repo<Master> call(final long tid, Master env) throws Exception {
		String zTablePath = ((((((Constants.ZROOT) + "/") + (env.getInstance().getInstanceID())) + (Constants.ZTABLES)) + "/") + (tableId)) + (Constants.ZTABLE_COMPACT_ID);
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		byte[] cid;
		try {
			cid = zoo.mutate(zTablePath, null, null, new IZooReaderWriter.Mutator() {
				@Override
				public byte[] mutate(byte[] currentValue) throws Exception {
					String cvs = new String(currentValue, StandardCharsets.UTF_8);
					String[] tokens = cvs.split(",");
					long flushID = Long.parseLong(tokens[0]);
					flushID++;
					String txidString = String.format("%016x", tid);
					for (int i = 1; i < (tokens.length); i++) {
						if (tokens[i].startsWith(txidString))
							continue;

						CompactRange.log.debug(("txidString : " + txidString));
						CompactRange.log.debug(((("tokens[" + i) + "] : ") + (tokens[i])));
						throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.COMPACT, TableOperationExceptionType.OTHER, "Another compaction with iterators and/or a compaction strategy is running");
					}
					StringBuilder encodedIterators = new StringBuilder();
					if ((config) != null) {
						Hex hex = new Hex();
						encodedIterators.append(",");
						encodedIterators.append(txidString);
						encodedIterators.append("=");
						encodedIterators.append(new String(hex.encode(config), StandardCharsets.UTF_8));
					}
					return ((Long.toString(flushID)) + encodedIterators).getBytes(StandardCharsets.UTF_8);
				}
			});
		} catch (KeeperException.NoNodeException nne) {
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.COMPACT, TableOperationExceptionType.NOTFOUND, null);
		}
		return null;
	}

	static void removeIterators(Master environment, final long txid, String tableId) throws Exception {
		String zTablePath = ((((((Constants.ZROOT) + "/") + (environment.getInstance().getInstanceID())) + (Constants.ZTABLES)) + "/") + tableId) + (Constants.ZTABLE_COMPACT_ID);
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		zoo.mutate(zTablePath, null, null, new IZooReaderWriter.Mutator() {
			@Override
			public byte[] mutate(byte[] currentValue) throws Exception {
				String cvs = new String(currentValue, StandardCharsets.UTF_8);
				String[] tokens = cvs.split(",");
				long flushID = Long.parseLong(tokens[0]);
				String txidString = String.format("%016x", txid);
				StringBuilder encodedIterators = new StringBuilder();
				for (int i = 1; i < (tokens.length); i++) {
					if (tokens[i].startsWith(txidString))
						continue;

					encodedIterators.append(",");
					encodedIterators.append(tokens[i]);
				}
				return ((Long.toString(flushID)) + encodedIterators).getBytes(StandardCharsets.UTF_8);
			}
		});
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
		try {
			CompactRange.removeIterators(env, tid, tableId);
		} finally {
			Utils.unreserveNamespace(getNamespaceId(env), tid, false);
			Utils.unreserveTable(tableId, tid, false);
		}
	}
}


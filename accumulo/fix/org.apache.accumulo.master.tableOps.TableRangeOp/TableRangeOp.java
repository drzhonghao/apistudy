

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.AcceptableThriftTableOperationException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;
import org.apache.accumulo.server.master.state.MergeInfo;
import org.apache.accumulo.server.master.state.MergeState;
import org.apache.hadoop.io.BinaryComparable;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.server.master.state.MergeInfo.Operation.MERGE;


public class TableRangeOp extends MasterRepo {
	private static final Logger log = LoggerFactory.getLogger(TableRangeOp.class);

	private static final long serialVersionUID = 1L;

	private final String tableId;

	private final String namespaceId;

	private byte[] startRow;

	private byte[] endRow;

	private MergeInfo.Operation op;

	private String getNamespaceId(Master env) throws Exception {
		return Utils.getNamespaceId(env.getInstance(), tableId, TableOperation.MERGE, this.namespaceId);
	}

	@Override
	public long isReady(long tid, Master env) throws Exception {
		return (Utils.reserveNamespace(getNamespaceId(env), tid, false, true, TableOperation.MERGE)) + (Utils.reserveTable(tableId, tid, true, true, TableOperation.MERGE));
	}

	public TableRangeOp(MergeInfo.Operation op, String namespaceId, String tableId, Text startRow, Text endRow) throws AcceptableThriftTableOperationException {
		this.tableId = tableId;
		this.namespaceId = namespaceId;
		this.startRow = TextUtil.getBytes(startRow);
		this.endRow = TextUtil.getBytes(endRow);
		this.op = op;
	}

	@Override
	public Repo<Master> call(long tid, Master env) throws Exception {
		if ((RootTable.ID.equals(tableId)) && (MERGE.equals(op))) {
			TableRangeOp.log.warn((("Attempt to merge tablets for " + (RootTable.NAME)) + " does nothing. It is not splittable."));
		}
		Text start = ((startRow.length) == 0) ? null : new Text(startRow);
		Text end = ((endRow.length) == 0) ? null : new Text(endRow);
		if ((start != null) && (end != null))
			if ((start.compareTo(end)) >= 0)
				throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.MERGE, TableOperationExceptionType.BAD_RANGE, "start row must be less than end row");


		env.mustBeOnline(tableId);
		MergeInfo info = env.getMergeInfo(tableId);
		if ((info.getState()) == (MergeState.NONE)) {
			KeyExtent range = new KeyExtent(tableId, end, start);
			env.setMergeState(new MergeInfo(range, op), MergeState.STARTED);
		}
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
		MergeInfo mergeInfo = env.getMergeInfo(tableId);
		if ((mergeInfo.getState()) != (MergeState.NONE))
			TableRangeOp.log.info(("removing merge information " + mergeInfo));

		env.clearMergeState(tableId);
		Utils.unreserveNamespace(namespaceId, tid, false);
		Utils.unreserveTable(tableId, tid, true);
		Utils.unreserveNamespace(getNamespaceId(env), tid, false);
	}
}


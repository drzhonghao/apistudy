

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.accumulo.core.data.Column;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.IterInfo;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.Stat;
import org.apache.accumulo.tserver.scan.ScanTask;
import org.apache.accumulo.tserver.session.Session;
import org.apache.accumulo.tserver.tablet.ScanBatch;
import org.apache.accumulo.tserver.tablet.Scanner;


public class ScanSession extends Session {
	public final Stat nbTimes = new Stat();

	public final KeyExtent extent;

	public final Set<Column> columnSet;

	public final List<IterInfo> ssiList;

	public final Map<String, Map<String, String>> ssio;

	public final Authorizations auths;

	public final AtomicBoolean interruptFlag = new AtomicBoolean();

	public long entriesReturned = 0;

	public long batchCount = 0;

	public volatile ScanTask<ScanBatch> nextBatchTask;

	public Scanner scanner;

	public final long readaheadThreshold;

	public final long batchTimeOut;

	public final String context;

	@Override
	public boolean cleanup() {
		final boolean ret;
		try {
			if ((nextBatchTask) != null)
				nextBatchTask.cancel(true);

		} finally {
			if ((scanner) != null)
				ret = scanner.close();
			else
				ret = true;

		}
		return ret;
	}
}


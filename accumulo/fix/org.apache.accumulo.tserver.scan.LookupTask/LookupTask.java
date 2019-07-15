

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.impl.Translator;
import org.apache.accumulo.core.client.impl.Translators;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Column;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.IterInfo;
import org.apache.accumulo.core.data.thrift.MultiScanResult;
import org.apache.accumulo.core.data.thrift.TKey;
import org.apache.accumulo.core.data.thrift.TKeyExtent;
import org.apache.accumulo.core.data.thrift.TKeyValue;
import org.apache.accumulo.core.data.thrift.TRange;
import org.apache.accumulo.core.iterators.IterationInterruptedException;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.server.conf.TableConfiguration;
import org.apache.accumulo.tserver.TabletServer;
import org.apache.accumulo.tserver.scan.ScanRunState;
import org.apache.accumulo.tserver.scan.ScanTask;
import org.apache.accumulo.tserver.session.MultiScanSession;
import org.apache.accumulo.tserver.session.Session;
import org.apache.accumulo.tserver.tablet.KVEntry;
import org.apache.accumulo.tserver.tablet.Tablet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LookupTask extends ScanTask<MultiScanResult> {
	private static final Logger log = LoggerFactory.getLogger(LookupTask.class);

	private final long scanID;

	@Override
	public void run() {
		MultiScanSession session = ((MultiScanSession) (server.getSession(scanID)));
		String oldThreadName = Thread.currentThread().getName();
		try {
			if ((isCancelled()) || (session == null))
				return;

			TableConfiguration acuTableConf = server.getTableConfiguration(session.threadPoolExtent);
			long maxResultsSize = acuTableConf.getMemoryInBytes(Property.TABLE_SCAN_MAXMEM);
			runState.set(ScanRunState.RUNNING);
			Thread.currentThread().setName((((((("Client: " + (session.client)) + " User: ") + (session.getUser())) + " Start: ") + (session.startTime)) + " Table: "));
			long bytesAdded = 0;
			long maxScanTime = 4000;
			long startTime = System.currentTimeMillis();
			List<KVEntry> results = new ArrayList<>();
			Map<KeyExtent, List<Range>> failures = new HashMap<>();
			List<KeyExtent> fullScans = new ArrayList<>();
			KeyExtent partScan = null;
			Key partNextKey = null;
			boolean partNextKeyInclusive = false;
			Iterator<Map.Entry<KeyExtent, List<Range>>> iter = session.queries.entrySet().iterator();
			while (((iter.hasNext()) && (bytesAdded < maxResultsSize)) && (((System.currentTimeMillis()) - startTime) < maxScanTime)) {
				Map.Entry<KeyExtent, List<Range>> entry = iter.next();
				iter.remove();
				Tablet tablet = server.getOnlineTablet(entry.getKey());
				if (tablet == null) {
					failures.put(entry.getKey(), entry.getValue());
					continue;
				}
				Thread.currentThread().setName(((((((("Client: " + (session.client)) + " User: ") + (session.getUser())) + " Start: ") + (session.startTime)) + " Tablet: ") + (entry.getKey().toString())));
				Tablet.LookupResult lookupResult;
				try {
					if (isCancelled())
						interruptFlag.set(true);

					lookupResult = tablet.lookup(entry.getValue(), session.columnSet, session.auths, results, (maxResultsSize - bytesAdded), session.ssiList, session.ssio, interruptFlag, session.samplerConfig, session.batchTimeOut, session.context);
					interruptFlag.set(false);
				} catch (IOException e) {
					LookupTask.log.warn(("lookup failed for tablet " + (entry.getKey())), e);
					throw new RuntimeException(e);
				}
				bytesAdded += lookupResult.bytesAdded;
				if ((lookupResult.unfinishedRanges.size()) > 0) {
					if (lookupResult.closed) {
						failures.put(entry.getKey(), lookupResult.unfinishedRanges);
					}else {
						session.queries.put(entry.getKey(), lookupResult.unfinishedRanges);
						partScan = entry.getKey();
						partNextKey = lookupResult.unfinishedRanges.get(0).getStartKey();
						partNextKeyInclusive = lookupResult.unfinishedRanges.get(0).isStartKeyInclusive();
					}
				}else {
					fullScans.add(entry.getKey());
				}
			} 
			long finishTime = System.currentTimeMillis();
			session.totalLookupTime += finishTime - startTime;
			session.numEntries += results.size();
			List<TKeyValue> retResults = new ArrayList<>();
			for (KVEntry entry : results)
				retResults.add(new TKeyValue(entry.getKey().toThrift(), ByteBuffer.wrap(entry.getValue().get())));

			Map<TKeyExtent, List<TRange>> retFailures = Translator.translate(failures, Translators.KET, new Translator.ListTranslator<>(Translators.RT));
			List<TKeyExtent> retFullScans = Translator.translate(fullScans, Translators.KET);
			TKeyExtent retPartScan = null;
			TKey retPartNextKey = null;
			if (partScan != null) {
				retPartScan = partScan.toThrift();
				retPartNextKey = partNextKey.toThrift();
			}
			addResult(new MultiScanResult(retResults, retFailures, retFullScans, retPartScan, retPartNextKey, partNextKeyInclusive, ((session.queries.size()) != 0)));
		} catch (IterationInterruptedException iie) {
			if (!(isCancelled())) {
				LookupTask.log.warn("Iteration interrupted, when scan not cancelled", iie);
				addResult(iie);
			}
		} catch (SampleNotPresentException e) {
			addResult(e);
		} catch (Throwable e) {
			LookupTask.log.warn("exception while doing multi-scan ", e);
			addResult(e);
		} finally {
			Thread.currentThread().setName(oldThreadName);
			runState.set(ScanRunState.FINISHED);
		}
	}
}


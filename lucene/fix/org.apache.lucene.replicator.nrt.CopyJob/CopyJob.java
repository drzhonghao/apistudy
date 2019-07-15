

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.replicator.nrt.CopyOneFile;
import org.apache.lucene.replicator.nrt.CopyState;
import org.apache.lucene.replicator.nrt.FileMetaData;
import org.apache.lucene.replicator.nrt.Node;
import org.apache.lucene.replicator.nrt.ReplicaNode;
import org.apache.lucene.util.IOUtils;


public abstract class CopyJob implements Comparable<CopyJob> {
	private static final AtomicLong counter = new AtomicLong();

	protected final ReplicaNode dest;

	protected final Map<String, FileMetaData> files;

	public final long ord = CopyJob.counter.incrementAndGet();

	public final boolean highPriority;

	public final CopyJob.OnceDone onceDone;

	public final long startNS = System.nanoTime();

	public final String reason;

	protected final List<Map.Entry<String, FileMetaData>> toCopy;

	protected long totBytes;

	protected long totBytesCopied;

	protected CopyOneFile current;

	protected volatile Throwable exc;

	protected volatile String cancelReason;

	protected final Map<String, String> copiedFiles = new ConcurrentHashMap<>();

	protected CopyJob(String reason, Map<String, FileMetaData> files, ReplicaNode dest, boolean highPriority, CopyJob.OnceDone onceDone) throws IOException {
		this.reason = reason;
		this.files = files;
		this.dest = dest;
		this.highPriority = highPriority;
		this.onceDone = onceDone;
		try {
			this.toCopy = dest.getFilesToCopy(this.files);
		} catch (Throwable t) {
			cancel("exc during init", t);
			throw new CorruptIndexException("exception while checking local files", "n/a", t);
		}
	}

	public interface OnceDone {
		public void run(CopyJob job) throws IOException;
	}

	public synchronized void transferAndCancel(CopyJob prevJob) throws IOException {
		synchronized(prevJob) {
			dest.message(("CopyJob: now transfer prevJob " + prevJob));
			try {
				_transferAndCancel(prevJob);
			} catch (Throwable t) {
				dest.message("xfer: exc during transferAndCancel");
				cancel("exc during transferAndCancel", t);
				throw IOUtils.rethrowAlways(t);
			}
		}
	}

	private synchronized void _transferAndCancel(CopyJob prevJob) throws IOException {
		assert Thread.holdsLock(prevJob);
		if ((prevJob.exc) != null) {
			dest.message("xfer: prevJob was already cancelled; skip transfer");
			return;
		}
		prevJob.exc = new Throwable();
		Iterator<Map.Entry<String, FileMetaData>> it = toCopy.iterator();
		long bytesAlreadyCopied = 0;
		while (it.hasNext()) {
			Map.Entry<String, FileMetaData> ent = it.next();
			String fileName = ent.getKey();
			String prevTmpFileName = prevJob.copiedFiles.get(fileName);
			if (prevTmpFileName != null) {
				long fileLength = ent.getValue().length;
				bytesAlreadyCopied += fileLength;
				dest.message((((((("xfer: carry over already-copied file " + fileName) + " (") + prevTmpFileName) + ", ") + fileLength) + " bytes)"));
				copiedFiles.put(fileName, prevTmpFileName);
				prevJob.copiedFiles.remove(fileName);
				it.remove();
			}else
				if (((prevJob.current) != null) && (prevJob.current.name.equals(fileName))) {
					dest.message(((((((("xfer: carry over in-progress file " + fileName) + " (") + (prevJob.current.tmpName)) + ") bytesCopied=") + (prevJob.current.getBytesCopied())) + " of ") + (prevJob.current.bytesToCopy)));
					bytesAlreadyCopied += prevJob.current.getBytesCopied();
					assert (current) == null;
					current = newCopyOneFile(prevJob.current);
					assert (prevJob.current.getBytesCopied()) <= (prevJob.current.bytesToCopy);
					prevJob.current = null;
					totBytes += current.metaData.length;
					it.remove();
				}else {
					dest.message((("xfer: file " + fileName) + " will be fully copied"));
				}

		} 
		dest.message(((("xfer: " + bytesAlreadyCopied) + " bytes already copied of ") + (totBytes)));
		dest.message(("xfer: now delete old temp files: " + (prevJob.copiedFiles.values())));
		if ((prevJob.current) != null) {
			IOUtils.closeWhileHandlingException(prevJob.current);
			if (Node.VERBOSE_FILES) {
				dest.message(("remove partial file " + (prevJob.current.tmpName)));
			}
			prevJob.current = null;
		}
	}

	protected abstract CopyOneFile newCopyOneFile(CopyOneFile current);

	public abstract void start() throws IOException;

	public abstract void runBlocking() throws Exception;

	public void cancel(String reason, Throwable exc) throws IOException {
		if ((this.exc) != null) {
			return;
		}
		dest.message(String.format(Locale.ROOT, "top: cancel after copying %s; exc=%s:\n  files=%s\n  copiedFiles=%s", Node.bytesToString(totBytesCopied), exc, ((files) == null ? "null" : files.keySet()), copiedFiles.keySet()));
		if (exc == null) {
			exc = new Throwable();
		}
		this.exc = exc;
		this.cancelReason = reason;
		if ((current) != null) {
			IOUtils.closeWhileHandlingException(current);
			if (Node.VERBOSE_FILES) {
				dest.message(("remove partial file " + (current.tmpName)));
			}
			current = null;
		}
	}

	public abstract boolean conflicts(CopyJob other);

	public abstract void finish() throws IOException;

	public abstract boolean getFailed();

	public abstract Set<String> getFileNamesToCopy();

	public abstract Set<String> getFileNames();

	public abstract CopyState getCopyState();

	public abstract long getTotalBytesCopied();
}




import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.BaseCompositeReader;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FilterCodecReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.SlowCodecReaderWrapper;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.SuppressForbidden;

import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE;


@SuppressForbidden(reason = "System.out required: command line tool")
public class MultiPassIndexSplitter {
	public void split(IndexReader in, Directory[] outputs, boolean seq) throws IOException {
		if ((outputs == null) || ((outputs.length) < 2)) {
			throw new IOException("Invalid number of outputs.");
		}
		if ((in == null) || ((in.numDocs()) < 2)) {
			throw new IOException("Not enough documents for splitting");
		}
		int numParts = outputs.length;
		MultiPassIndexSplitter.FakeDeleteIndexReader input = new MultiPassIndexSplitter.FakeDeleteIndexReader(in);
		int maxDoc = input.maxDoc();
		int partLen = maxDoc / numParts;
		for (int i = 0; i < numParts; i++) {
			input.undeleteAll();
			if (seq) {
				int lo = partLen * i;
				int hi = lo + partLen;
				for (int j = 0; j < lo; j++) {
					input.deleteDocument(j);
				}
				if (i < (numParts - 1)) {
					for (int j = hi; j < maxDoc; j++) {
						input.deleteDocument(j);
					}
				}
			}else {
				for (int j = 0; j < maxDoc; j++) {
					if ((((j + numParts) - i) % numParts) != 0) {
						input.deleteDocument(j);
					}
				}
			}
			IndexWriter w = new IndexWriter(outputs[i], new IndexWriterConfig(null).setOpenMode(CREATE));
			System.err.println((("Writing part " + (i + 1)) + " ..."));
			w.close();
		}
		System.err.println("Done.");
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		if ((args.length) < 5) {
			System.err.println("Usage: MultiPassIndexSplitter -out <outputDir> -num <numParts> [-seq] <inputIndex1> [<inputIndex2 ...]");
			System.err.println("\tinputIndex\tpath to input index, multiple values are ok");
			System.err.println("\t-out ouputDir\tpath to output directory to contain partial indexes");
			System.err.println("\t-num numParts\tnumber of parts to produce");
			System.err.println("\t-seq\tsequential docid-range split (default is round-robin)");
			System.exit((-1));
		}
		ArrayList<IndexReader> indexes = new ArrayList<>();
		String outDir = null;
		int numParts = -1;
		boolean seq = false;
		for (int i = 0; i < (args.length); i++) {
			if (args[i].equals("-out")) {
				outDir = args[(++i)];
			}else
				if (args[i].equals("-num")) {
					numParts = Integer.parseInt(args[(++i)]);
				}else
					if (args[i].equals("-seq")) {
						seq = true;
					}else {
						Path file = Paths.get(args[i]);
						if (!(Files.isDirectory(file))) {
							System.err.println(("Invalid input path - skipping: " + file));
							continue;
						}
						Directory dir = FSDirectory.open(file);
						try {
							if (!(DirectoryReader.indexExists(dir))) {
								System.err.println(("Invalid input index - skipping: " + file));
								continue;
							}
						} catch (Exception e) {
							System.err.println(("Invalid input index - skipping: " + file));
							continue;
						}
						indexes.add(DirectoryReader.open(dir));
					}


		}
		if (outDir == null) {
			throw new Exception("Required argument missing: -out outputDir");
		}
		if (numParts < 2) {
			throw new Exception("Invalid value of required argument: -num numParts");
		}
		if ((indexes.size()) == 0) {
			throw new Exception("No input indexes to process");
		}
		Path out = Paths.get(outDir);
		Files.createDirectories(out);
		Directory[] dirs = new Directory[numParts];
		for (int i = 0; i < numParts; i++) {
			dirs[i] = FSDirectory.open(out.resolve(("part-" + i)));
		}
		MultiPassIndexSplitter splitter = new MultiPassIndexSplitter();
		IndexReader input;
		if ((indexes.size()) == 1) {
			input = indexes.get(0);
		}else {
			input = new MultiReader(indexes.toArray(new IndexReader[indexes.size()]));
		}
		splitter.split(input, dirs, seq);
	}

	private static final class FakeDeleteIndexReader extends BaseCompositeReader<MultiPassIndexSplitter.FakeDeleteLeafIndexReader> {
		public FakeDeleteIndexReader(IndexReader reader) throws IOException {
			super(MultiPassIndexSplitter.FakeDeleteIndexReader.initSubReaders(reader));
		}

		private static MultiPassIndexSplitter.FakeDeleteLeafIndexReader[] initSubReaders(IndexReader reader) throws IOException {
			final List<LeafReaderContext> leaves = reader.leaves();
			final MultiPassIndexSplitter.FakeDeleteLeafIndexReader[] subs = new MultiPassIndexSplitter.FakeDeleteLeafIndexReader[leaves.size()];
			int i = 0;
			for (final LeafReaderContext ctx : leaves) {
				subs[(i++)] = new MultiPassIndexSplitter.FakeDeleteLeafIndexReader(SlowCodecReaderWrapper.wrap(ctx.reader()));
			}
			return subs;
		}

		public void deleteDocument(int docID) {
			final int i = readerIndex(docID);
			getSequentialSubReaders().get(i).deleteDocument((docID - (readerBase(i))));
		}

		public void undeleteAll() {
			for (MultiPassIndexSplitter.FakeDeleteLeafIndexReader r : getSequentialSubReaders()) {
				r.undeleteAll();
			}
		}

		@Override
		protected void doClose() {
		}

		@Override
		public IndexReader.CacheHelper getReaderCacheHelper() {
			return null;
		}
	}

	private static final class FakeDeleteLeafIndexReader extends FilterCodecReader {
		FixedBitSet liveDocs;

		public FakeDeleteLeafIndexReader(CodecReader reader) {
			super(reader);
			undeleteAll();
		}

		@Override
		public int numDocs() {
			return liveDocs.cardinality();
		}

		public void undeleteAll() {
			final int maxDoc = in.maxDoc();
			liveDocs = new FixedBitSet(in.maxDoc());
			if (in.hasDeletions()) {
				final Bits oldLiveDocs = in.getLiveDocs();
				assert oldLiveDocs != null;
				for (int i = 0; i < maxDoc; i++) {
					if (oldLiveDocs.get(i))
						liveDocs.set(i);

				}
			}else {
				liveDocs.set(0, maxDoc);
			}
		}

		public void deleteDocument(int n) {
			liveDocs.clear(n);
		}

		@Override
		public Bits getLiveDocs() {
			return liveDocs;
		}

		@Override
		public IndexReader.CacheHelper getCoreCacheHelper() {
			return in.getCoreCacheHelper();
		}

		@Override
		public IndexReader.CacheHelper getReaderCacheHelper() {
			return null;
		}
	}
}


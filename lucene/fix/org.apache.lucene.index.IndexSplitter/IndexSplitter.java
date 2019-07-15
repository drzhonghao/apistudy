

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class IndexSplitter {
	public final SegmentInfos infos;

	FSDirectory fsDir;

	Path dir;

	@org.apache.lucene.util.SuppressForbidden(reason = "System.out required: command line tool")
	public static void main(String[] args) throws Exception {
		if ((args.length) < 2) {
			System.err.println("Usage: IndexSplitter <srcDir> -l (list the segments and their sizes)");
			System.err.println("IndexSplitter <srcDir> <destDir> <segments>+");
			System.err.println("IndexSplitter <srcDir> -d (delete the following segments)");
			return;
		}
		Path srcDir = Paths.get(args[0]);
		IndexSplitter is = new IndexSplitter(srcDir);
		if (!(Files.exists(srcDir))) {
			throw new Exception((("srcdir:" + (srcDir.toAbsolutePath())) + " doesn't exist"));
		}
		if (args[1].equals("-l")) {
			is.listSegments();
		}else
			if (args[1].equals("-d")) {
				List<String> segs = new ArrayList<>();
				for (int x = 2; x < (args.length); x++) {
					segs.add(args[x]);
				}
				is.remove(segs.toArray(new String[0]));
			}else {
				Path targetDir = Paths.get(args[1]);
				List<String> segs = new ArrayList<>();
				for (int x = 2; x < (args.length); x++) {
					segs.add(args[x]);
				}
				is.split(targetDir, segs.toArray(new String[0]));
			}

	}

	public IndexSplitter(Path dir) throws IOException {
		this.dir = dir;
		fsDir = FSDirectory.open(dir);
		infos = SegmentInfos.readLatestCommit(fsDir);
	}

	@org.apache.lucene.util.SuppressForbidden(reason = "System.out required: command line tool")
	public void listSegments() throws IOException {
		DecimalFormat formatter = new DecimalFormat("###,###.###", DecimalFormatSymbols.getInstance(Locale.ROOT));
		for (int x = 0; x < (infos.size()); x++) {
			SegmentCommitInfo info = infos.info(x);
			String sizeStr = formatter.format(info.sizeInBytes());
			System.out.println((((info.info.name) + " ") + sizeStr));
		}
	}

	private int getIdx(String name) {
		for (int x = 0; x < (infos.size()); x++) {
			if (name.equals(infos.info(x).info.name))
				return x;

		}
		return -1;
	}

	private SegmentCommitInfo getInfo(String name) {
		for (int x = 0; x < (infos.size()); x++) {
			if (name.equals(infos.info(x).info.name))
				return infos.info(x);

		}
		return null;
	}

	public void remove(String[] segs) throws IOException {
		for (String n : segs) {
			int idx = getIdx(n);
		}
		infos.changed();
		infos.commit(fsDir);
	}

	public void split(Path destDir, String[] segs) throws IOException {
		Files.createDirectories(destDir);
		FSDirectory destFSDir = FSDirectory.open(destDir);
		SegmentInfos destInfos = new SegmentInfos(infos.getIndexCreatedVersionMajor());
		destInfos.counter = infos.counter;
		for (String n : segs) {
			SegmentCommitInfo infoPerCommit = getInfo(n);
			SegmentInfo info = infoPerCommit.info;
			SegmentInfo newInfo = new SegmentInfo(destFSDir, info.getVersion(), info.getMinVersion(), info.name, info.maxDoc(), info.getUseCompoundFile(), info.getCodec(), info.getDiagnostics(), info.getId(), new HashMap<>(), null);
			destInfos.add(new SegmentCommitInfo(newInfo, infoPerCommit.getDelCount(), infoPerCommit.getSoftDelCount(), infoPerCommit.getDelGen(), infoPerCommit.getFieldInfosGen(), infoPerCommit.getDocValuesGen()));
			Collection<String> files = infoPerCommit.files();
			for (final String srcName : files) {
				Path srcFile = dir.resolve(srcName);
				Path destFile = destDir.resolve(srcName);
				Files.copy(srcFile, destFile);
			}
		}
		destInfos.changed();
		destInfos.commit(destFSDir);
	}
}


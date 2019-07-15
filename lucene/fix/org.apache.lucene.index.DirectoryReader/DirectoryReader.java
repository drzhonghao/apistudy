

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.index.BaseCompositeReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.Directory;


public abstract class DirectoryReader extends BaseCompositeReader<LeafReader> {
	protected final Directory directory;

	public static DirectoryReader open(final Directory directory) throws IOException {
		return null;
	}

	public static DirectoryReader open(final IndexWriter writer) throws IOException {
		return DirectoryReader.open(writer, true, false);
	}

	public static DirectoryReader open(final IndexWriter writer, boolean applyAllDeletes, boolean writeAllDeletes) throws IOException {
		return null;
	}

	public static DirectoryReader open(final IndexCommit commit) throws IOException {
		return null;
	}

	public static DirectoryReader openIfChanged(DirectoryReader oldReader) throws IOException {
		final DirectoryReader newReader = oldReader.doOpenIfChanged();
		assert newReader != oldReader;
		return newReader;
	}

	public static DirectoryReader openIfChanged(DirectoryReader oldReader, IndexCommit commit) throws IOException {
		final DirectoryReader newReader = oldReader.doOpenIfChanged(commit);
		assert newReader != oldReader;
		return newReader;
	}

	public static DirectoryReader openIfChanged(DirectoryReader oldReader, IndexWriter writer) throws IOException {
		return DirectoryReader.openIfChanged(oldReader, writer, true);
	}

	public static DirectoryReader openIfChanged(DirectoryReader oldReader, IndexWriter writer, boolean applyAllDeletes) throws IOException {
		final DirectoryReader newReader = oldReader.doOpenIfChanged(writer, applyAllDeletes);
		assert newReader != oldReader;
		return newReader;
	}

	public static List<IndexCommit> listCommits(Directory dir) throws IOException {
		final String[] files = dir.listAll();
		List<IndexCommit> commits = new ArrayList<>();
		SegmentInfos latest = SegmentInfos.readLatestCommit(dir);
		final long currentGen = latest.getGeneration();
		for (int i = 0; i < (files.length); i++) {
			final String fileName = files[i];
			if (((fileName.startsWith(IndexFileNames.SEGMENTS)) && (!(fileName.equals(IndexFileNames.OLD_SEGMENTS_GEN)))) && ((SegmentInfos.generationFromSegmentsFileName(fileName)) < currentGen)) {
				SegmentInfos sis = null;
				try {
					sis = SegmentInfos.readCommit(dir, fileName);
				} catch (FileNotFoundException | NoSuchFileException fnfe) {
				}
				if (sis != null) {
				}
			}
		}
		Collections.sort(commits);
		return commits;
	}

	public static boolean indexExists(Directory directory) throws IOException {
		String[] files = directory.listAll();
		String prefix = (IndexFileNames.SEGMENTS) + "_";
		for (String file : files) {
			if (file.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	protected DirectoryReader(Directory directory, LeafReader[] segmentReaders) throws IOException {
		super(segmentReaders);
		this.directory = directory;
	}

	public final Directory directory() {
		return directory;
	}

	protected abstract DirectoryReader doOpenIfChanged() throws IOException;

	protected abstract DirectoryReader doOpenIfChanged(final IndexCommit commit) throws IOException;

	protected abstract DirectoryReader doOpenIfChanged(IndexWriter writer, boolean applyAllDeletes) throws IOException;

	public abstract long getVersion();

	public abstract boolean isCurrent() throws IOException;

	public abstract IndexCommit getIndexCommit() throws IOException;
}


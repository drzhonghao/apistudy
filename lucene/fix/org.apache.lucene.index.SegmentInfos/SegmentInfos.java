

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.SegmentInfoFormat;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.Version;


public final class SegmentInfos implements Cloneable , Iterable<SegmentCommitInfo> {
	public static final int VERSION_53 = 6;

	public static final int VERSION_70 = 7;

	public static final int VERSION_72 = 8;

	public static final int VERSION_74 = 9;

	static final int VERSION_CURRENT = SegmentInfos.VERSION_74;

	public long counter;

	public long version;

	private long generation;

	private long lastGeneration;

	public Map<String, String> userData = Collections.emptyMap();

	private List<SegmentCommitInfo> segments = new ArrayList<>();

	private static PrintStream infoStream = null;

	private byte[] id;

	private Version luceneVersion;

	private Version minSegmentLuceneVersion;

	private final int indexCreatedVersionMajor;

	public SegmentInfos(int indexCreatedVersionMajor) {
		if (indexCreatedVersionMajor > (Version.LATEST.major)) {
			throw new IllegalArgumentException(("indexCreatedVersionMajor is in the future: " + indexCreatedVersionMajor));
		}
		if (indexCreatedVersionMajor < 6) {
			throw new IllegalArgumentException(("indexCreatedVersionMajor must be >= 6, got: " + indexCreatedVersionMajor));
		}
		this.indexCreatedVersionMajor = indexCreatedVersionMajor;
	}

	public SegmentCommitInfo info(int i) {
		return segments.get(i);
	}

	public static long getLastCommitGeneration(String[] files) {
		long max = -1;
		for (String file : files) {
			if ((file.startsWith(IndexFileNames.SEGMENTS)) && (!(file.equals(IndexFileNames.OLD_SEGMENTS_GEN)))) {
				long gen = SegmentInfos.generationFromSegmentsFileName(file);
				if (gen > max) {
					max = gen;
				}
			}
		}
		return max;
	}

	public static long getLastCommitGeneration(Directory directory) throws IOException {
		return SegmentInfos.getLastCommitGeneration(directory.listAll());
	}

	public static String getLastCommitSegmentsFileName(String[] files) {
		return IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS, "", SegmentInfos.getLastCommitGeneration(files));
	}

	public static String getLastCommitSegmentsFileName(Directory directory) throws IOException {
		return IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS, "", SegmentInfos.getLastCommitGeneration(directory));
	}

	public String getSegmentsFileName() {
		return IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS, "", lastGeneration);
	}

	public static long generationFromSegmentsFileName(String fileName) {
		if (fileName.equals(IndexFileNames.SEGMENTS)) {
			return 0;
		}else
			if (fileName.startsWith(IndexFileNames.SEGMENTS)) {
				return Long.parseLong(fileName.substring((1 + (IndexFileNames.SEGMENTS.length()))), Character.MAX_RADIX);
			}else {
				throw new IllegalArgumentException((("fileName \"" + fileName) + "\" is not a segments file"));
			}

	}

	private long getNextPendingGeneration() {
		if ((generation) == (-1)) {
			return 1;
		}else {
			return (generation) + 1;
		}
	}

	public byte[] getId() {
		return id.clone();
	}

	public static final SegmentInfos readCommit(Directory directory, String segmentFileName) throws IOException {
		long generation = SegmentInfos.generationFromSegmentsFileName(segmentFileName);
		try (ChecksumIndexInput input = directory.openChecksumInput(segmentFileName, IOContext.READ)) {
			try {
				return SegmentInfos.readCommit(directory, input, generation);
			} catch (EOFException | NoSuchFileException | FileNotFoundException e) {
				throw new CorruptIndexException("Unexpected file read error while reading index.", input, e);
			}
		}
	}

	public static final SegmentInfos readCommit(Directory directory, ChecksumIndexInput input, long generation) throws IOException {
		int magic = input.readInt();
		if (magic != (CodecUtil.CODEC_MAGIC)) {
			throw new IndexFormatTooOldException(input, magic, CodecUtil.CODEC_MAGIC, CodecUtil.CODEC_MAGIC);
		}
		int format = CodecUtil.checkHeaderNoMagic(input, "segments", SegmentInfos.VERSION_53, SegmentInfos.VERSION_CURRENT);
		byte[] id = new byte[StringHelper.ID_LENGTH];
		input.readBytes(id, 0, id.length);
		CodecUtil.checkIndexHeaderSuffix(input, Long.toString(generation, Character.MAX_RADIX));
		Version luceneVersion = Version.fromBits(input.readVInt(), input.readVInt(), input.readVInt());
		if ((luceneVersion.onOrAfter(Version.LUCENE_6_0_0)) == false) {
			throw new IndexFormatTooOldException(input, (("this index is too old (version: " + luceneVersion) + ")"));
		}
		int indexCreatedVersion = 6;
		if (format >= (SegmentInfos.VERSION_70)) {
			indexCreatedVersion = input.readVInt();
		}
		SegmentInfos infos = new SegmentInfos(indexCreatedVersion);
		infos.id = id;
		infos.generation = generation;
		infos.lastGeneration = generation;
		infos.luceneVersion = luceneVersion;
		infos.version = input.readLong();
		if (format > (SegmentInfos.VERSION_70)) {
			infos.counter = input.readVLong();
		}else {
			infos.counter = input.readInt();
		}
		int numSegments = input.readInt();
		if (numSegments < 0) {
			throw new CorruptIndexException(("invalid segment count: " + numSegments), input);
		}
		if (numSegments > 0) {
			infos.minSegmentLuceneVersion = Version.fromBits(input.readVInt(), input.readVInt(), input.readVInt());
		}else {
		}
		long totalDocs = 0;
		for (int seg = 0; seg < numSegments; seg++) {
			String segName = input.readString();
			if (format < (SegmentInfos.VERSION_70)) {
				byte hasID = input.readByte();
				if (hasID == 0) {
					throw new IndexFormatTooOldException(input, "Segment is from Lucene 4.x");
				}else
					if (hasID != 1) {
						throw new CorruptIndexException(("invalid hasID byte, got: " + hasID), input);
					}

			}
			byte[] segmentID = new byte[StringHelper.ID_LENGTH];
			input.readBytes(segmentID, 0, segmentID.length);
			Codec codec = SegmentInfos.readCodec(input);
			SegmentInfo info = codec.segmentInfoFormat().read(directory, segName, segmentID, IOContext.READ);
			info.setCodec(codec);
			totalDocs += info.maxDoc();
			long delGen = input.readLong();
			int delCount = input.readInt();
			if ((delCount < 0) || (delCount > (info.maxDoc()))) {
				throw new CorruptIndexException(((("invalid deletion count: " + delCount) + " vs maxDoc=") + (info.maxDoc())), input);
			}
			long fieldInfosGen = input.readLong();
			long dvGen = input.readLong();
			int softDelCount = (format > (SegmentInfos.VERSION_72)) ? input.readInt() : 0;
			if ((softDelCount < 0) || (softDelCount > (info.maxDoc()))) {
				throw new CorruptIndexException(((("invalid deletion count: " + softDelCount) + " vs maxDoc=") + (info.maxDoc())), input);
			}
			if ((softDelCount + delCount) > (info.maxDoc())) {
				throw new CorruptIndexException((((("invalid deletion count: " + softDelCount) + delCount) + " vs maxDoc=") + (info.maxDoc())), input);
			}
			SegmentCommitInfo siPerCommit = new SegmentCommitInfo(info, delCount, softDelCount, delGen, fieldInfosGen, dvGen);
			siPerCommit.setFieldInfosFiles(input.readSetOfStrings());
			final Map<Integer, Set<String>> dvUpdateFiles;
			final int numDVFields = input.readInt();
			if (numDVFields == 0) {
				dvUpdateFiles = Collections.emptyMap();
			}else {
				Map<Integer, Set<String>> map = new HashMap<>(numDVFields);
				for (int i = 0; i < numDVFields; i++) {
					map.put(input.readInt(), input.readSetOfStrings());
				}
				dvUpdateFiles = Collections.unmodifiableMap(map);
			}
			siPerCommit.setDocValuesUpdatesFiles(dvUpdateFiles);
			infos.add(siPerCommit);
			Version segmentVersion = info.getVersion();
			if ((segmentVersion.onOrAfter(infos.minSegmentLuceneVersion)) == false) {
				throw new CorruptIndexException(((((("segments file recorded minSegmentLuceneVersion=" + (infos.minSegmentLuceneVersion)) + " but segment=") + info) + " has older version=") + segmentVersion), input);
			}
			if (((infos.indexCreatedVersionMajor) >= 7) && ((segmentVersion.major) < (infos.indexCreatedVersionMajor))) {
				throw new CorruptIndexException(((((("segments file recorded indexCreatedVersionMajor=" + (infos.indexCreatedVersionMajor)) + " but segment=") + info) + " has older version=") + segmentVersion), input);
			}
			if (((infos.indexCreatedVersionMajor) >= 7) && ((info.getMinVersion()) == null)) {
				throw new CorruptIndexException(("segments infos must record minVersion with indexCreatedVersionMajor=" + (infos.indexCreatedVersionMajor)), input);
			}
		}
		infos.userData = input.readMapOfStrings();
		CodecUtil.checkFooter(input);
		return infos;
	}

	private static Codec readCodec(DataInput input) throws IOException {
		final String name = input.readString();
		try {
			return Codec.forName(name);
		} catch (IllegalArgumentException e) {
			if (name.startsWith("Lucene")) {
				throw new IllegalArgumentException((("Could not load codec '" + name) + "'.  Did you forget to add lucene-backward-codecs.jar?"), e);
			}
			throw e;
		}
	}

	public static final SegmentInfos readLatestCommit(Directory directory) throws IOException {
		return new SegmentInfos.FindSegmentsFile<SegmentInfos>(directory) {
			@Override
			protected SegmentInfos doBody(String segmentFileName) throws IOException {
				return SegmentInfos.readCommit(directory, segmentFileName);
			}
		}.run();
	}

	boolean pendingCommit;

	private void write(Directory directory) throws IOException {
		long nextGeneration = getNextPendingGeneration();
		String segmentFileName = IndexFileNames.fileNameFromGeneration(IndexFileNames.PENDING_SEGMENTS, "", nextGeneration);
		generation = nextGeneration;
		IndexOutput segnOutput = null;
		boolean success = false;
		try {
			segnOutput = directory.createOutput(segmentFileName, IOContext.DEFAULT);
			write(directory, segnOutput);
			segnOutput.close();
			directory.sync(Collections.singleton(segmentFileName));
			success = true;
		} finally {
			if (success) {
				pendingCommit = true;
			}else {
				IOUtils.closeWhileHandlingException(segnOutput);
				IOUtils.deleteFilesIgnoringExceptions(directory, segmentFileName);
			}
		}
	}

	public void write(Directory directory, IndexOutput out) throws IOException {
		CodecUtil.writeIndexHeader(out, "segments", SegmentInfos.VERSION_CURRENT, StringHelper.randomId(), Long.toString(generation, Character.MAX_RADIX));
		out.writeVInt(Version.LATEST.major);
		out.writeVInt(Version.LATEST.minor);
		out.writeVInt(Version.LATEST.bugfix);
		out.writeVInt(indexCreatedVersionMajor);
		out.writeLong(version);
		out.writeVLong(counter);
		out.writeInt(size());
		if ((size()) > 0) {
			Version minSegmentVersion = null;
			for (SegmentCommitInfo siPerCommit : this) {
				Version segmentVersion = siPerCommit.info.getVersion();
				if ((minSegmentVersion == null) || ((segmentVersion.onOrAfter(minSegmentVersion)) == false)) {
					minSegmentVersion = segmentVersion;
				}
			}
			out.writeVInt(minSegmentVersion.major);
			out.writeVInt(minSegmentVersion.minor);
			out.writeVInt(minSegmentVersion.bugfix);
		}
		for (SegmentCommitInfo siPerCommit : this) {
			SegmentInfo si = siPerCommit.info;
			out.writeString(si.name);
			byte[] segmentID = si.getId();
			if ((segmentID.length) != (StringHelper.ID_LENGTH)) {
				throw new IllegalStateException(((("cannot write segment: invalid id segment=" + (si.name)) + "id=") + (StringHelper.idToString(segmentID))));
			}
			out.writeBytes(segmentID, segmentID.length);
			out.writeString(si.getCodec().getName());
			out.writeLong(siPerCommit.getDelGen());
			int delCount = siPerCommit.getDelCount();
			if ((delCount < 0) || (delCount > (si.maxDoc()))) {
				throw new IllegalStateException(((((("cannot write segment: invalid maxDoc segment=" + (si.name)) + " maxDoc=") + (si.maxDoc())) + " delCount=") + delCount));
			}
			out.writeInt(delCount);
			out.writeLong(siPerCommit.getFieldInfosGen());
			out.writeLong(siPerCommit.getDocValuesGen());
			int softDelCount = siPerCommit.getSoftDelCount();
			if ((softDelCount < 0) || (softDelCount > (si.maxDoc()))) {
				throw new IllegalStateException(((((("cannot write segment: invalid maxDoc segment=" + (si.name)) + " maxDoc=") + (si.maxDoc())) + " softDelCount=") + softDelCount));
			}
			out.writeInt(softDelCount);
			out.writeSetOfStrings(siPerCommit.getFieldInfosFiles());
			final Map<Integer, Set<String>> dvUpdatesFiles = siPerCommit.getDocValuesUpdatesFiles();
			out.writeInt(dvUpdatesFiles.size());
			for (Map.Entry<Integer, Set<String>> e : dvUpdatesFiles.entrySet()) {
				out.writeInt(e.getKey());
				out.writeSetOfStrings(e.getValue());
			}
		}
		out.writeMapOfStrings(userData);
		CodecUtil.writeFooter(out);
	}

	@Override
	public SegmentInfos clone() {
		try {
			final SegmentInfos sis = ((SegmentInfos) (super.clone()));
			sis.segments = new ArrayList<>(size());
			for (final SegmentCommitInfo info : this) {
				assert (info.info.getCodec()) != null;
				sis.add(info.clone());
			}
			sis.userData = new HashMap<>(userData);
			return sis;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("should not happen", e);
		}
	}

	public long getVersion() {
		return version;
	}

	public long getGeneration() {
		return generation;
	}

	public long getLastGeneration() {
		return lastGeneration;
	}

	public static void setInfoStream(PrintStream infoStream) {
		SegmentInfos.infoStream = infoStream;
	}

	public static PrintStream getInfoStream() {
		return SegmentInfos.infoStream;
	}

	private static void message(String message) {
		SegmentInfos.infoStream.println(((("SIS [" + (Thread.currentThread().getName())) + "]: ") + message));
	}

	public abstract static class FindSegmentsFile<T> {
		final Directory directory;

		public FindSegmentsFile(Directory directory) {
			this.directory = directory;
		}

		public T run() throws IOException {
			return run(null);
		}

		public T run(IndexCommit commit) throws IOException {
			if (commit != null) {
				if ((directory) != (commit.getDirectory()))
					throw new IOException("the specified commit does not match the specified Directory");

				return doBody(commit.getSegmentsFileName());
			}
			long lastGen = -1;
			long gen = -1;
			IOException exc = null;
			for (; ;) {
				lastGen = gen;
				String[] files = directory.listAll();
				String[] files2 = directory.listAll();
				Arrays.sort(files);
				Arrays.sort(files2);
				if (!(Arrays.equals(files, files2))) {
					continue;
				}
				gen = SegmentInfos.getLastCommitGeneration(files);
				if ((SegmentInfos.infoStream) != null) {
					SegmentInfos.message(("directory listing gen=" + gen));
				}
				if (gen == (-1)) {
					throw new IndexNotFoundException(((("no segments* file found in " + (directory)) + ": files: ") + (Arrays.toString(files))));
				}else
					if (gen > lastGen) {
						String segmentFileName = IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS, "", gen);
						try {
							T t = doBody(segmentFileName);
							if ((SegmentInfos.infoStream) != null) {
								SegmentInfos.message(("success on " + segmentFileName));
							}
							return t;
						} catch (IOException err) {
							if (exc == null) {
								exc = err;
							}
							if ((SegmentInfos.infoStream) != null) {
								SegmentInfos.message(((((("primary Exception on '" + segmentFileName) + "': ") + err) + "'; will retry: gen = ") + gen));
							}
						}
					}else {
						throw exc;
					}

			}
		}

		protected abstract T doBody(String segmentFileName) throws IOException;
	}

	public void updateGeneration(SegmentInfos other) {
		lastGeneration = other.lastGeneration;
		generation = other.generation;
	}

	void updateGenerationVersionAndCounter(SegmentInfos other) {
		updateGeneration(other);
		this.version = other.version;
		this.counter = other.counter;
	}

	public void setNextWriteGeneration(long generation) {
		if (generation < (this.generation)) {
			throw new IllegalStateException(((("cannot decrease generation to " + generation) + " from current generation ") + (this.generation)));
		}
		this.generation = generation;
	}

	final void rollbackCommit(Directory dir) {
		if (pendingCommit) {
			pendingCommit = false;
			final String pending = IndexFileNames.fileNameFromGeneration(IndexFileNames.PENDING_SEGMENTS, "", generation);
			IOUtils.deleteFilesIgnoringExceptions(dir, pending);
		}
	}

	final void prepareCommit(Directory dir) throws IOException {
		if (pendingCommit) {
			throw new IllegalStateException("prepareCommit was already called");
		}
		dir.syncMetaData();
		write(dir);
	}

	public Collection<String> files(boolean includeSegmentsFile) throws IOException {
		HashSet<String> files = new HashSet<>();
		if (includeSegmentsFile) {
			final String segmentFileName = getSegmentsFileName();
			if (segmentFileName != null) {
				files.add(segmentFileName);
			}
		}
		final int size = size();
		for (int i = 0; i < size; i++) {
			final SegmentCommitInfo info = info(i);
			files.addAll(info.files());
		}
		return files;
	}

	final String finishCommit(Directory dir) throws IOException {
		if ((pendingCommit) == false) {
			throw new IllegalStateException("prepareCommit was not called");
		}
		boolean success = false;
		final String dest;
		try {
			final String src = IndexFileNames.fileNameFromGeneration(IndexFileNames.PENDING_SEGMENTS, "", generation);
			dest = IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS, "", generation);
			dir.rename(src, dest);
			dir.syncMetaData();
			success = true;
		} finally {
			if (!success) {
				rollbackCommit(dir);
			}
		}
		pendingCommit = false;
		lastGeneration = generation;
		return dest;
	}

	public final void commit(Directory dir) throws IOException {
		prepareCommit(dir);
		finishCommit(dir);
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(getSegmentsFileName()).append(": ");
		final int count = size();
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				buffer.append(' ');
			}
			final SegmentCommitInfo info = info(i);
			buffer.append(info.toString(0));
		}
		return buffer.toString();
	}

	public Map<String, String> getUserData() {
		return userData;
	}

	public void setUserData(Map<String, String> data, boolean doIncrementVersion) {
		if (data == null) {
			userData = Collections.<String, String>emptyMap();
		}else {
			userData = data;
		}
		if (doIncrementVersion) {
			changed();
		}
	}

	void replace(SegmentInfos other) {
		rollbackSegmentInfos(other.asList());
		lastGeneration = other.lastGeneration;
	}

	public int totalMaxDoc() {
		long count = 0;
		for (SegmentCommitInfo info : this) {
			count += info.info.maxDoc();
		}
		return Math.toIntExact(count);
	}

	public void changed() {
		(version)++;
	}

	void setVersion(long newVersion) {
		if (newVersion < (version)) {
			throw new IllegalArgumentException((((("newVersion (=" + newVersion) + ") cannot be less than current version (=") + (version)) + ")"));
		}
		version = newVersion;
	}

	void applyMergeChanges(MergePolicy.OneMerge merge, boolean dropSegment) {
		final Set<SegmentCommitInfo> mergedAway = new HashSet<>(merge.segments);
		boolean inserted = false;
		int newSegIdx = 0;
		for (int segIdx = 0, cnt = segments.size(); segIdx < cnt; segIdx++) {
			assert segIdx >= newSegIdx;
			final SegmentCommitInfo info = segments.get(segIdx);
			if (mergedAway.contains(info)) {
				if ((!inserted) && (!dropSegment)) {
					inserted = true;
					newSegIdx++;
				}
			}else {
				segments.set(newSegIdx, info);
				newSegIdx++;
			}
		}
		segments.subList(newSegIdx, segments.size()).clear();
		if ((!inserted) && (!dropSegment)) {
		}
	}

	List<SegmentCommitInfo> createBackupSegmentInfos() {
		final List<SegmentCommitInfo> list = new ArrayList<>(size());
		for (final SegmentCommitInfo info : this) {
			assert (info.info.getCodec()) != null;
			list.add(info.clone());
		}
		return list;
	}

	void rollbackSegmentInfos(List<SegmentCommitInfo> infos) {
		this.clear();
		this.addAll(infos);
	}

	@Override
	public Iterator<SegmentCommitInfo> iterator() {
		return asList().iterator();
	}

	public List<SegmentCommitInfo> asList() {
		return Collections.unmodifiableList(segments);
	}

	public int size() {
		return segments.size();
	}

	public void add(SegmentCommitInfo si) {
		segments.add(si);
	}

	public void addAll(Iterable<SegmentCommitInfo> sis) {
		for (final SegmentCommitInfo si : sis) {
			this.add(si);
		}
	}

	public void clear() {
		segments.clear();
	}

	public boolean remove(SegmentCommitInfo si) {
		return segments.remove(si);
	}

	void remove(int index) {
		segments.remove(index);
	}

	boolean contains(SegmentCommitInfo si) {
		return segments.contains(si);
	}

	int indexOf(SegmentCommitInfo si) {
		return segments.indexOf(si);
	}

	public Version getCommitLuceneVersion() {
		return luceneVersion;
	}

	public Version getMinSegmentLuceneVersion() {
		return minSegmentLuceneVersion;
	}

	public int getIndexCreatedVersionMajor() {
		return indexCreatedVersionMajor;
	}
}


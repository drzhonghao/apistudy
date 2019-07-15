

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.EmptyFileException;
import org.apache.poi.poifs.common.POIFSBigBlockSize;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.poifs.dev.POIFSViewable;
import org.apache.poi.poifs.filesystem.BlockStore;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.EntryNode;
import org.apache.poi.poifs.filesystem.POIFSDocument;
import org.apache.poi.poifs.filesystem.POIFSMiniStore;
import org.apache.poi.poifs.filesystem.POIFSStream;
import org.apache.poi.poifs.filesystem.POIFSWriterListener;
import org.apache.poi.poifs.nio.ByteArrayBackedDataSource;
import org.apache.poi.poifs.nio.DataSource;
import org.apache.poi.poifs.nio.FileBackedDataSource;
import org.apache.poi.poifs.property.DirectoryProperty;
import org.apache.poi.poifs.property.PropertyTable;
import org.apache.poi.poifs.storage.BATBlock;
import org.apache.poi.poifs.storage.HeaderBlock;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public class POIFSFileSystem extends BlockStore implements Closeable , POIFSViewable {
	private static final int MAX_RECORD_LENGTH = 100000;

	private static final POILogger LOG = POILogFactory.getLogger(POIFSFileSystem.class);

	private static final int MAX_BLOCK_COUNT = 65535;

	private POIFSMiniStore _mini_store;

	private PropertyTable _property_table;

	private List<BATBlock> _xbat_blocks;

	private List<BATBlock> _bat_blocks;

	private HeaderBlock _header;

	private DirectoryNode _root;

	private DataSource _data;

	private POIFSBigBlockSize bigBlockSize = POIFSConstants.SMALLER_BIG_BLOCK_SIZE_DETAILS;

	private POIFSFileSystem(boolean newFS) {
		_header = new HeaderBlock(bigBlockSize);
		_property_table = new PropertyTable(_header);
		_xbat_blocks = new ArrayList<>();
		_bat_blocks = new ArrayList<>();
		_root = null;
		if (newFS) {
			_data = new ByteArrayBackedDataSource(IOUtils.safelyAllocate(((bigBlockSize.getBigBlockSize()) * 3), POIFSFileSystem.MAX_RECORD_LENGTH));
		}
	}

	public POIFSFileSystem() {
		this(true);
		_header.setBATCount(1);
		_header.setBATArray(new int[]{ 1 });
		BATBlock bb = BATBlock.createEmptyBATBlock(bigBlockSize, false);
		bb.setOurBlockIndex(1);
		_bat_blocks.add(bb);
		setNextBlock(0, POIFSConstants.END_OF_CHAIN);
		setNextBlock(1, POIFSConstants.FAT_SECTOR_BLOCK);
		_property_table.setStartBlock(0);
	}

	public POIFSFileSystem(File file) throws IOException {
		this(file, true);
	}

	public POIFSFileSystem(File file, boolean readOnly) throws IOException {
		this(null, file, readOnly, true);
	}

	public POIFSFileSystem(FileChannel channel) throws IOException {
		this(channel, true);
	}

	public POIFSFileSystem(FileChannel channel, boolean readOnly) throws IOException {
		this(channel, null, readOnly, false);
	}

	private POIFSFileSystem(FileChannel channel, File srcFile, boolean readOnly, boolean closeChannelOnError) throws IOException {
		this(false);
		try {
			if (srcFile != null) {
				if ((srcFile.length()) == 0)
					throw new EmptyFileException();

				FileBackedDataSource d = new FileBackedDataSource(srcFile, readOnly);
				channel = d.getChannel();
				_data = d;
			}else {
				_data = new FileBackedDataSource(channel, readOnly);
			}
			ByteBuffer headerBuffer = ByteBuffer.allocate(POIFSConstants.SMALLER_BIG_BLOCK_SIZE);
			IOUtils.readFully(channel, headerBuffer);
			_header = new HeaderBlock(headerBuffer);
			readCoreContents();
		} catch (IOException | RuntimeException e) {
			if (closeChannelOnError && (channel != null)) {
				channel.close();
			}
			throw e;
		}
	}

	public POIFSFileSystem(InputStream stream) throws IOException {
		this(false);
		boolean success = false;
		try (ReadableByteChannel channel = Channels.newChannel(stream)) {
			ByteBuffer headerBuffer = ByteBuffer.allocate(POIFSConstants.SMALLER_BIG_BLOCK_SIZE);
			IOUtils.readFully(channel, headerBuffer);
			_header = new HeaderBlock(headerBuffer);
			POIFSFileSystem.sanityCheckBlockCount(_header.getBATCount());
			long maxSize = BATBlock.calculateMaximumSize(_header);
			if (maxSize > (Integer.MAX_VALUE)) {
				throw new IllegalArgumentException("Unable read a >2gb file via an InputStream");
			}
			ByteBuffer data = ByteBuffer.allocate(((int) (maxSize)));
			headerBuffer.position(0);
			data.put(headerBuffer);
			data.position(headerBuffer.capacity());
			IOUtils.readFully(channel, data);
			success = true;
			_data = new ByteArrayBackedDataSource(data.array(), data.position());
		} finally {
			closeInputStream(stream, success);
		}
		readCoreContents();
	}

	private void closeInputStream(InputStream stream, boolean success) {
		try {
			stream.close();
		} catch (IOException e) {
			if (success) {
				throw new RuntimeException(e);
			}
			POIFSFileSystem.LOG.log(POILogger.ERROR, "can't close input stream", e);
		}
	}

	private void readCoreContents() throws IOException {
		bigBlockSize = _header.getBigBlockSize();
		BlockStore.ChainLoopDetector loopDetector = getChainLoopDetector();
		for (int fatAt : _header.getBATArray()) {
			readBAT(fatAt, loopDetector);
		}
		int remainingFATs = (_header.getBATCount()) - (_header.getBATArray().length);
		BATBlock xfat;
		int nextAt = _header.getXBATIndex();
		for (int i = 0; i < (_header.getXBATCount()); i++) {
			ByteBuffer fatData = getBlockAt(nextAt);
			xfat = BATBlock.createBATBlock(bigBlockSize, fatData);
			xfat.setOurBlockIndex(nextAt);
			nextAt = xfat.getValueAt(bigBlockSize.getXBATEntriesPerBlock());
			_xbat_blocks.add(xfat);
			int xbatFATs = Math.min(remainingFATs, bigBlockSize.getXBATEntriesPerBlock());
			for (int j = 0; j < xbatFATs; j++) {
				int fatAt = xfat.getValueAt(j);
				if ((fatAt == (POIFSConstants.UNUSED_BLOCK)) || (fatAt == (POIFSConstants.END_OF_CHAIN)))
					break;

				readBAT(fatAt, loopDetector);
			}
			remainingFATs -= xbatFATs;
		}
		BATBlock sfat;
		List<BATBlock> sbats = new ArrayList<>();
		nextAt = _header.getSBATStart();
		for (int i = 0; (i < (_header.getSBATCount())) && (nextAt != (POIFSConstants.END_OF_CHAIN)); i++) {
			ByteBuffer fatData = getBlockAt(nextAt);
			sfat = BATBlock.createBATBlock(bigBlockSize, fatData);
			sfat.setOurBlockIndex(nextAt);
			sbats.add(sfat);
			nextAt = getNextBlock(nextAt);
		}
	}

	private void readBAT(int batAt, BlockStore.ChainLoopDetector loopDetector) throws IOException {
		ByteBuffer fatData = getBlockAt(batAt);
		BATBlock bat = BATBlock.createBATBlock(bigBlockSize, fatData);
		bat.setOurBlockIndex(batAt);
		_bat_blocks.add(bat);
	}

	private BATBlock createBAT(int offset, boolean isBAT) throws IOException {
		BATBlock newBAT = BATBlock.createEmptyBATBlock(bigBlockSize, (!isBAT));
		newBAT.setOurBlockIndex(offset);
		ByteBuffer buffer = ByteBuffer.allocate(bigBlockSize.getBigBlockSize());
		int writeTo = (1 + offset) * (bigBlockSize.getBigBlockSize());
		_data.write(buffer, writeTo);
		return newBAT;
	}

	@Override
	protected ByteBuffer getBlockAt(final int offset) throws IOException {
		long blockWanted = offset + 1L;
		long startAt = blockWanted * (bigBlockSize.getBigBlockSize());
		try {
			return _data.read(bigBlockSize.getBigBlockSize(), startAt);
		} catch (IndexOutOfBoundsException e) {
			IndexOutOfBoundsException wrapped = new IndexOutOfBoundsException((("Block " + offset) + " not found"));
			wrapped.initCause(e);
			throw wrapped;
		}
	}

	@Override
	protected ByteBuffer createBlockIfNeeded(final int offset) throws IOException {
		try {
			return getBlockAt(offset);
		} catch (IndexOutOfBoundsException e) {
			long startAt = (offset + 1L) * (bigBlockSize.getBigBlockSize());
			ByteBuffer buffer = ByteBuffer.allocate(getBigBlockSize());
			_data.write(buffer, startAt);
			return getBlockAt(offset);
		}
	}

	@Override
	protected BATBlock.BATBlockAndIndex getBATBlockAndIndex(final int offset) {
		return BATBlock.getBATBlockAndIndex(offset, _header, _bat_blocks);
	}

	@Override
	protected int getNextBlock(final int offset) {
		BATBlock.BATBlockAndIndex bai = getBATBlockAndIndex(offset);
		return bai.getBlock().getValueAt(bai.getIndex());
	}

	@Override
	protected void setNextBlock(final int offset, final int nextBlock) {
		BATBlock.BATBlockAndIndex bai = getBATBlockAndIndex(offset);
		bai.getBlock().setValueAt(bai.getIndex(), nextBlock);
	}

	@Override
	protected int getFreeBlock() throws IOException {
		int numSectors = bigBlockSize.getBATEntriesPerBlock();
		int offset = 0;
		for (BATBlock bat : _bat_blocks) {
			if (bat.hasFreeSectors()) {
				for (int j = 0; j < numSectors; j++) {
					int batValue = bat.getValueAt(j);
					if (batValue == (POIFSConstants.UNUSED_BLOCK)) {
						return offset + j;
					}
				}
			}
			offset += numSectors;
		}
		BATBlock bat = createBAT(offset, true);
		bat.setValueAt(0, POIFSConstants.FAT_SECTOR_BLOCK);
		_bat_blocks.add(bat);
		if ((_header.getBATCount()) >= 109) {
			BATBlock xbat = null;
			for (BATBlock x : _xbat_blocks) {
				if (x.hasFreeSectors()) {
					xbat = x;
					break;
				}
			}
			if (xbat == null) {
				xbat = createBAT((offset + 1), false);
				xbat.setValueAt(0, offset);
				bat.setValueAt(1, POIFSConstants.DIFAT_SECTOR_BLOCK);
				offset++;
				if ((_xbat_blocks.size()) == 0) {
					_header.setXBATStart(offset);
				}else {
					_xbat_blocks.get(((_xbat_blocks.size()) - 1)).setValueAt(bigBlockSize.getXBATEntriesPerBlock(), offset);
				}
				_xbat_blocks.add(xbat);
				_header.setXBATCount(_xbat_blocks.size());
			}else {
				for (int i = 0; i < (bigBlockSize.getXBATEntriesPerBlock()); i++) {
					if ((xbat.getValueAt(i)) == (POIFSConstants.UNUSED_BLOCK)) {
						xbat.setValueAt(i, offset);
						break;
					}
				}
			}
		}else {
			int[] newBATs = new int[(_header.getBATCount()) + 1];
			System.arraycopy(_header.getBATArray(), 0, newBATs, 0, ((newBATs.length) - 1));
			newBATs[((newBATs.length) - 1)] = offset;
			_header.setBATArray(newBATs);
		}
		_header.setBATCount(_bat_blocks.size());
		return offset + 1;
	}

	protected long size() throws IOException {
		return _data.size();
	}

	@Override
	protected BlockStore.ChainLoopDetector getChainLoopDetector() throws IOException {
		return null;
	}

	PropertyTable _get_property_table() {
		return _property_table;
	}

	POIFSMiniStore getMiniStore() {
		return _mini_store;
	}

	void addDocument(final POIFSDocument document) {
	}

	void addDirectory(final DirectoryProperty directory) {
		_property_table.addProperty(directory);
	}

	public DocumentEntry createDocument(final InputStream stream, final String name) throws IOException {
		return getRoot().createDocument(name, stream);
	}

	public DocumentEntry createDocument(final String name, final int size, final POIFSWriterListener writer) throws IOException {
		return getRoot().createDocument(name, size, writer);
	}

	public DirectoryEntry createDirectory(final String name) throws IOException {
		return getRoot().createDirectory(name);
	}

	@SuppressWarnings("UnusedReturnValue")
	public DocumentEntry createOrUpdateDocument(final InputStream stream, final String name) throws IOException {
		return getRoot().createOrUpdateDocument(name, stream);
	}

	public boolean isInPlaceWriteable() {
		return ((_data) instanceof FileBackedDataSource) && (((FileBackedDataSource) (_data)).isWriteable());
	}

	public void writeFilesystem() throws IOException {
		if (!((_data) instanceof FileBackedDataSource)) {
			throw new IllegalArgumentException(("POIFS opened from an inputstream, so writeFilesystem() may " + "not be called. Use writeFilesystem(OutputStream) instead"));
		}
		if (!(((FileBackedDataSource) (_data)).isWriteable())) {
			throw new IllegalArgumentException(("POIFS opened in read only mode, so writeFilesystem() may " + "not be called. Open the FileSystem in read-write mode first"));
		}
		syncWithDataSource();
	}

	public void writeFilesystem(final OutputStream stream) throws IOException {
		syncWithDataSource();
		_data.copyTo(stream);
	}

	private void syncWithDataSource() throws IOException {
		POIFSStream propStream = new POIFSStream(this, _header.getPropertyStart());
		_property_table.preWrite();
		_property_table.write(propStream);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(_header.getBigBlockSize().getBigBlockSize());
		_header.writeData(baos);
		getBlockAt((-1)).put(baos.toByteArray());
		for (BATBlock bat : _bat_blocks) {
			ByteBuffer block = getBlockAt(bat.getOurBlockIndex());
			bat.writeData(block);
		}
		for (BATBlock bat : _xbat_blocks) {
			ByteBuffer block = getBlockAt(bat.getOurBlockIndex());
			bat.writeData(block);
		}
	}

	public void close() throws IOException {
		_data.close();
	}

	public static void main(String[] args) throws IOException {
		if ((args.length) != 2) {
			System.err.println("two arguments required: input filename and output filename");
			System.exit(1);
		}
		try (FileInputStream istream = new FileInputStream(args[0])) {
			try (FileOutputStream ostream = new FileOutputStream(args[1])) {
				try (POIFSFileSystem fs = new POIFSFileSystem(istream)) {
					fs.writeFilesystem(ostream);
				}
			}
		}
	}

	public DirectoryNode getRoot() {
		if ((_root) == null) {
		}
		return _root;
	}

	public DocumentInputStream createDocumentInputStream(final String documentName) throws IOException {
		return getRoot().createDocumentInputStream(documentName);
	}

	void remove(EntryNode entry) throws IOException {
		if (entry instanceof DocumentEntry) {
		}
	}

	public Object[] getViewableArray() {
		if (preferArray()) {
			return getRoot().getViewableArray();
		}
		return new Object[0];
	}

	public Iterator<Object> getViewableIterator() {
		if (!(preferArray())) {
			return getRoot().getViewableIterator();
		}
		return Collections.emptyList().iterator();
	}

	public boolean preferArray() {
		return getRoot().preferArray();
	}

	public String getShortDescription() {
		return "POIFS FileSystem";
	}

	public int getBigBlockSize() {
		return bigBlockSize.getBigBlockSize();
	}

	@SuppressWarnings("WeakerAccess")
	public POIFSBigBlockSize getBigBlockSizeDetails() {
		return bigBlockSize;
	}

	public static POIFSFileSystem create(File file) throws IOException {
		try (POIFSFileSystem tmp = new POIFSFileSystem();OutputStream out = new FileOutputStream(file)) {
			tmp.writeFilesystem(out);
		}
		return new POIFSFileSystem(file, false);
	}

	@Override
	protected int getBlockStoreBlockSize() {
		return getBigBlockSize();
	}

	@org.apache.poi.util.Internal
	public PropertyTable getPropertyTable() {
		return _property_table;
	}

	@org.apache.poi.util.Internal
	public HeaderBlock getHeaderBlock() {
		return _header;
	}

	private static void sanityCheckBlockCount(int block_count) throws IOException {
		if (block_count <= 0) {
			throw new IOException((("Illegal block count; minimum count is 1, got " + block_count) + " instead"));
		}
		if (block_count > (POIFSFileSystem.MAX_BLOCK_COUNT)) {
			throw new IOException((((("Block count " + block_count) + " is too high. POI maximum is ") + (POIFSFileSystem.MAX_BLOCK_COUNT)) + "."));
		}
	}
}


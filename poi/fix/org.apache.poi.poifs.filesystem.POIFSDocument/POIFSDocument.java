

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.poifs.dev.POIFSViewable;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSStream;
import org.apache.poi.poifs.filesystem.POIFSWriterListener;
import org.apache.poi.poifs.property.DocumentProperty;
import org.apache.poi.poifs.property.Property;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;


public final class POIFSDocument implements Iterable<ByteBuffer> , POIFSViewable {
	private static final int MAX_RECORD_LENGTH = 100000;

	private DocumentProperty _property;

	private POIFSFileSystem _filesystem;

	private POIFSStream _stream;

	private int _block_size;

	public POIFSDocument(DocumentNode document) {
	}

	public POIFSDocument(DocumentProperty property, POIFSFileSystem filesystem) {
		this._property = property;
		this._filesystem = filesystem;
		if ((property.getSize()) < (POIFSConstants.BIG_BLOCK_MINIMUM_DOCUMENT_SIZE)) {
		}else {
			_stream = new POIFSStream(_filesystem, property.getStartBlock());
		}
	}

	public POIFSDocument(String name, POIFSFileSystem filesystem, InputStream stream) throws IOException {
		this._filesystem = filesystem;
		int length = store(stream);
		this._property = new DocumentProperty(name, length);
		_property.setStartBlock(_stream.getStartBlock());
	}

	public POIFSDocument(String name, final int size, POIFSFileSystem filesystem, POIFSWriterListener writer) throws IOException {
		this._filesystem = filesystem;
		if (size < (POIFSConstants.BIG_BLOCK_MINIMUM_DOCUMENT_SIZE)) {
		}else {
			_stream = new POIFSStream(filesystem);
		}
		this._property = new DocumentProperty(name, size);
		_property.setStartBlock(_stream.getStartBlock());
	}

	private int store(InputStream stream) throws IOException {
		final int bigBlockSize = POIFSConstants.BIG_BLOCK_MINIMUM_DOCUMENT_SIZE;
		BufferedInputStream bis = new BufferedInputStream(stream, (bigBlockSize + 1));
		bis.mark(bigBlockSize);
		long streamBlockSize = IOUtils.skipFully(bis, bigBlockSize);
		if (streamBlockSize < bigBlockSize) {
		}else {
			_stream = new POIFSStream(_filesystem);
		}
		bis.reset();
		final long length;
		try (OutputStream os = _stream.getOutputStream()) {
			length = IOUtils.copy(bis, os);
			int usedInBlock = ((int) (length % (_block_size)));
			if ((usedInBlock != 0) && (usedInBlock != (_block_size))) {
				int toBlockEnd = (_block_size) - usedInBlock;
				byte[] padding = IOUtils.safelyAllocate(toBlockEnd, POIFSDocument.MAX_RECORD_LENGTH);
				Arrays.fill(padding, ((byte) (255)));
				os.write(padding);
			}
		}
		return ((int) (length));
	}

	void free() throws IOException {
		_stream.free();
		_property.setStartBlock(POIFSConstants.END_OF_CHAIN);
	}

	POIFSFileSystem getFileSystem() {
		return _filesystem;
	}

	int getDocumentBlockSize() {
		return _block_size;
	}

	@Override
	public Iterator<ByteBuffer> iterator() {
		return getBlockIterator();
	}

	Iterator<ByteBuffer> getBlockIterator() {
		return ((getSize()) > 0 ? _stream : Collections.<ByteBuffer>emptyList()).iterator();
	}

	public int getSize() {
		return _property.getSize();
	}

	public void replaceContents(InputStream stream) throws IOException {
		free();
		int size = store(stream);
		_property.setStartBlock(_stream.getStartBlock());
		_property.updateSize(size);
	}

	DocumentProperty getDocumentProperty() {
		return _property;
	}

	public Object[] getViewableArray() {
		String result = "<NO DATA>";
		if ((getSize()) > 0) {
			byte[] data = IOUtils.safelyAllocate(getSize(), POIFSDocument.MAX_RECORD_LENGTH);
			int offset = 0;
			for (ByteBuffer buffer : _stream) {
				int length = Math.min(_block_size, ((data.length) - offset));
				buffer.get(data, offset, length);
				offset += length;
			}
			result = HexDump.dump(data, 0, 0);
		}
		return new String[]{ result };
	}

	public Iterator<Object> getViewableIterator() {
		return Collections.emptyList().iterator();
	}

	public boolean preferArray() {
		return true;
	}

	public String getShortDescription() {
		return (("Document: \"" + (_property.getName())) + "\" size = ") + (getSize());
	}
}


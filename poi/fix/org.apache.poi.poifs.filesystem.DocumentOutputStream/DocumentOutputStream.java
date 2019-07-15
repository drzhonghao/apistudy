

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.POIFSDocument;
import org.apache.poi.poifs.filesystem.POIFSStream;
import org.apache.poi.poifs.property.DocumentProperty;
import org.apache.poi.poifs.property.Property;


public final class DocumentOutputStream extends OutputStream {
	private int _document_size = 0;

	private boolean _closed = false;

	private POIFSDocument _document;

	private DocumentProperty _property;

	private ByteArrayOutputStream _buffer = new ByteArrayOutputStream(POIFSConstants.BIG_BLOCK_MINIMUM_DOCUMENT_SIZE);

	private POIFSStream _stream;

	private OutputStream _stream_output;

	private final long _limit;

	public DocumentOutputStream(DocumentEntry document) throws IOException {
		this(document, (-1));
	}

	public DocumentOutputStream(DirectoryEntry parent, String name) throws IOException {
		this(DocumentOutputStream.createDocument(parent, name), (-1));
	}

	DocumentOutputStream(DocumentEntry document, long limit) throws IOException {
		this(DocumentOutputStream.getDocument(document), limit);
	}

	DocumentOutputStream(POIFSDocument document, long limit) throws IOException {
		_document = document;
		_limit = limit;
	}

	private static POIFSDocument getDocument(DocumentEntry document) throws IOException {
		if (!(document instanceof DocumentNode)) {
			throw new IOException((("Cannot open internal document storage, " + document) + " not a Document Node"));
		}
		return new POIFSDocument(((DocumentNode) (document)));
	}

	private static DocumentEntry createDocument(DirectoryEntry parent, String name) throws IOException {
		if (!(parent instanceof DirectoryNode)) {
			throw new IOException((("Cannot open internal directory storage, " + parent) + " not a Directory Node"));
		}
		return parent.createDocument(name, new ByteArrayInputStream(new byte[0]));
	}

	private void checkBufferSize() throws IOException {
		if ((_buffer.size()) > (POIFSConstants.BIG_BLOCK_MINIMUM_DOCUMENT_SIZE)) {
			byte[] data = _buffer.toByteArray();
			_buffer = null;
			write(data, 0, data.length);
		}else {
		}
	}

	public void write(int b) throws IOException {
		write(new byte[]{ ((byte) (b)) }, 0, 1);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (_closed) {
			throw new IOException("cannot perform requested operation on a closed stream");
		}
		if (((_limit) > (-1)) && (((size()) + len) > (_limit))) {
			throw new IOException("tried to write too much data");
		}
		if ((_buffer) != null) {
			_buffer.write(b, off, len);
			checkBufferSize();
		}else {
			if ((_stream) == null) {
				_stream_output = _stream.getOutputStream();
			}
			_stream_output.write(b, off, len);
			_document_size += len;
		}
	}

	public void close() throws IOException {
		if ((_buffer) != null) {
			_document.replaceContents(new ByteArrayInputStream(_buffer.toByteArray()));
		}else {
			_stream_output.close();
			_property.updateSize(_document_size);
			_property.setStartBlock(_stream.getStartBlock());
		}
		_closed = true;
	}

	public long size() {
		return (_document_size) + ((_buffer) == null ? 0 : _buffer.size());
	}
}


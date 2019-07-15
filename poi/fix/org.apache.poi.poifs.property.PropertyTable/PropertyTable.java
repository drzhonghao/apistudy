

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.apache.poi.poifs.common.POIFSBigBlockSize;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.poifs.filesystem.BATManaged;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSStream;
import org.apache.poi.poifs.property.DirectoryProperty;
import org.apache.poi.poifs.property.Property;
import org.apache.poi.poifs.property.RootProperty;
import org.apache.poi.poifs.storage.HeaderBlock;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class PropertyTable implements BATManaged {
	private static final POILogger _logger = POILogFactory.getLogger(PropertyTable.class);

	private static final int MAX_RECORD_LENGTH = 100000;

	private final HeaderBlock _header_block;

	private final List<Property> _properties = new ArrayList<>();

	private final POIFSBigBlockSize _bigBigBlockSize;

	public PropertyTable(HeaderBlock headerBlock) {
		_header_block = headerBlock;
		_bigBigBlockSize = headerBlock.getBigBlockSize();
	}

	public PropertyTable(final HeaderBlock headerBlock, final POIFSFileSystem filesystem) throws IOException {
		this(headerBlock, new POIFSStream(filesystem, headerBlock.getPropertyStart()));
	}

	PropertyTable(final HeaderBlock headerBlock, final Iterable<ByteBuffer> dataSource) throws IOException {
		_header_block = headerBlock;
		_bigBigBlockSize = headerBlock.getBigBlockSize();
		for (ByteBuffer bb : dataSource) {
			byte[] data;
			if (((bb.hasArray()) && ((bb.arrayOffset()) == 0)) && ((bb.array().length) == (_bigBigBlockSize.getBigBlockSize()))) {
				data = bb.array();
			}else {
				data = IOUtils.safelyAllocate(_bigBigBlockSize.getBigBlockSize(), PropertyTable.MAX_RECORD_LENGTH);
				int toRead = data.length;
				if ((bb.remaining()) < (_bigBigBlockSize.getBigBlockSize())) {
					PropertyTable._logger.log(POILogger.WARN, "Short Property Block, ", bb.remaining(), (" bytes instead of the expected " + (_bigBigBlockSize.getBigBlockSize())));
					toRead = bb.remaining();
				}
				bb.get(data, 0, toRead);
			}
		}
		populatePropertyTree(((DirectoryProperty) (_properties.get(0))));
	}

	public void addProperty(Property property) {
		_properties.add(property);
	}

	public void removeProperty(final Property property) {
		_properties.remove(property);
	}

	public RootProperty getRoot() {
		return ((RootProperty) (_properties.get(0)));
	}

	public int getStartBlock() {
		return _header_block.getPropertyStart();
	}

	public void setStartBlock(final int index) {
		_header_block.setPropertyStart(index);
	}

	public int countBlocks() {
		long rawSize = (_properties.size()) * ((long) (POIFSConstants.PROPERTY_SIZE));
		int blkSize = _bigBigBlockSize.getBigBlockSize();
		int numBlocks = ((int) (rawSize / blkSize));
		if ((rawSize % blkSize) != 0) {
			numBlocks++;
		}
		return numBlocks;
	}

	public void preWrite() {
		List<Property> pList = new ArrayList<>();
		int i = 0;
		for (Property p : _properties) {
			if (p == null)
				continue;

			pList.add(p);
		}
		for (Property p : pList) {
		}
	}

	public void write(POIFSStream stream) throws IOException {
		OutputStream os = stream.getOutputStream();
		for (Property property : _properties) {
			if (property != null) {
				property.writeData(os);
			}
		}
		os.close();
		if ((getStartBlock()) != (stream.getStartBlock())) {
			setStartBlock(stream.getStartBlock());
		}
	}

	private void populatePropertyTree(DirectoryProperty root) throws IOException {
		final Stack<Property> children = new Stack<>();
		while (!(children.empty())) {
			Property property = children.pop();
			if (property == null) {
				continue;
			}
			root.addChild(property);
			if (property.isDirectory()) {
				populatePropertyTree(((DirectoryProperty) (property)));
			}
		} 
	}

	private boolean isValidIndex(int index) {
		if ((index < 0) || (index >= (_properties.size()))) {
			PropertyTable._logger.log(POILogger.WARN, ((("Property index " + index) + "outside the valid range 0..") + (_properties.size())));
			return false;
		}
		return true;
	}
}


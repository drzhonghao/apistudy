

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.poi.poifs.common.POIFSBigBlockSize;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.poifs.filesystem.BlockStore;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSStream;
import org.apache.poi.poifs.property.Property;
import org.apache.poi.poifs.property.RootProperty;
import org.apache.poi.poifs.storage.BATBlock;
import org.apache.poi.poifs.storage.HeaderBlock;


public class POIFSMiniStore extends BlockStore {
	private POIFSFileSystem _filesystem;

	private POIFSStream _mini_stream;

	private List<BATBlock> _sbat_blocks;

	private HeaderBlock _header;

	private RootProperty _root;

	POIFSMiniStore(POIFSFileSystem filesystem, RootProperty root, List<BATBlock> sbats, HeaderBlock header) {
		this._filesystem = filesystem;
		this._sbat_blocks = sbats;
		this._header = header;
		this._root = root;
		this._mini_stream = new POIFSStream(filesystem, root.getStartBlock());
	}

	protected ByteBuffer getBlockAt(final int offset) {
		int byteOffset = offset * (POIFSConstants.SMALL_BLOCK_SIZE);
		int bigBlockNumber = byteOffset / (_filesystem.getBigBlockSize());
		int bigBlockOffset = byteOffset % (_filesystem.getBigBlockSize());
		for (int i = 0; i < bigBlockNumber; i++) {
		}
		return null;
	}

	protected ByteBuffer createBlockIfNeeded(final int offset) throws IOException {
		boolean firstInStore = false;
		if ((_mini_stream.getStartBlock()) == (POIFSConstants.END_OF_CHAIN)) {
			firstInStore = true;
		}
		if (!firstInStore) {
			try {
				return getBlockAt(offset);
			} catch (IndexOutOfBoundsException e) {
			}
		}
		if (firstInStore) {
		}else {
			int block = _mini_stream.getStartBlock();
			while (true) {
			} 
		}
		return createBlockIfNeeded(offset);
	}

	protected BATBlock.BATBlockAndIndex getBATBlockAndIndex(final int offset) {
		return BATBlock.getSBATBlockAndIndex(offset, _header, _sbat_blocks);
	}

	protected int getNextBlock(final int offset) {
		BATBlock.BATBlockAndIndex bai = getBATBlockAndIndex(offset);
		return bai.getBlock().getValueAt(bai.getIndex());
	}

	protected void setNextBlock(final int offset, final int nextBlock) {
		BATBlock.BATBlockAndIndex bai = getBATBlockAndIndex(offset);
		bai.getBlock().setValueAt(bai.getIndex(), nextBlock);
	}

	protected int getFreeBlock() throws IOException {
		int sectorsPerSBAT = _filesystem.getBigBlockSizeDetails().getBATEntriesPerBlock();
		int offset = 0;
		for (BATBlock sbat : _sbat_blocks) {
			if (sbat.hasFreeSectors()) {
				for (int j = 0; j < sectorsPerSBAT; j++) {
					int sbatValue = sbat.getValueAt(j);
					if (sbatValue == (POIFSConstants.UNUSED_BLOCK)) {
						return offset + j;
					}
				}
			}
			offset += sectorsPerSBAT;
		}
		BATBlock newSBAT = BATBlock.createEmptyBATBlock(_filesystem.getBigBlockSizeDetails(), false);
		if ((_header.getSBATCount()) == 0) {
			_header.setSBATBlockCount(1);
		}else {
			int batOffset = _header.getSBATStart();
			while (true) {
			} 
		}
		_sbat_blocks.add(newSBAT);
		return offset;
	}

	@Override
	protected BlockStore.ChainLoopDetector getChainLoopDetector() {
		return null;
	}

	protected int getBlockStoreBlockSize() {
		return POIFSConstants.SMALL_BLOCK_SIZE;
	}

	void syncWithDataSource() throws IOException {
		int blocksUsed = 0;
		for (BATBlock sbat : _sbat_blocks) {
			if (!(sbat.hasFreeSectors())) {
				blocksUsed += _filesystem.getBigBlockSizeDetails().getBATEntriesPerBlock();
			}else {
				blocksUsed += sbat.getUsedSectors(false);
			}
		}
	}
}


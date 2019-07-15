

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.poi.hdgf.chunks.Chunk;
import org.apache.poi.hdgf.chunks.ChunkHeader;
import org.apache.poi.hdgf.chunks.ChunkSeparator;
import org.apache.poi.hdgf.chunks.ChunkTrailer;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class ChunkFactory {
	private static final int MAX_RECORD_LENGTH = 1000000;

	private int version;

	private final Map<Integer, ChunkFactory.CommandDefinition[]> chunkCommandDefinitions = new HashMap<>();

	private static final String chunkTableName = "/org/apache/poi/hdgf/chunks_parse_cmds.tbl";

	private static final POILogger logger = POILogFactory.getLogger(ChunkFactory.class);

	public ChunkFactory(int version) throws IOException {
		this.version = version;
		processChunkParseCommands();
	}

	private void processChunkParseCommands() throws IOException {
		String line;
		InputStream cpd = null;
		BufferedReader inp = null;
		try {
			cpd = ChunkFactory.class.getResourceAsStream(ChunkFactory.chunkTableName);
			if (cpd == null) {
				throw new IllegalStateException(("Unable to find HDGF chunk definition on the classpath - " + (ChunkFactory.chunkTableName)));
			}
			inp = new BufferedReader(new InputStreamReader(cpd, LocaleUtil.CHARSET_1252));
			while ((line = inp.readLine()) != null) {
				if ((line.isEmpty()) || ("# \t".contains(line.substring(0, 1)))) {
					continue;
				}
				if (!(line.matches("^start [0-9]+$"))) {
					throw new IllegalStateException(("Expecting start xxx, found " + line));
				}
				int chunkType = Integer.parseInt(line.substring(6));
				ArrayList<ChunkFactory.CommandDefinition> defsL = new ArrayList<>();
				while ((line = inp.readLine()) != null) {
					if (line.startsWith("end")) {
						break;
					}
					StringTokenizer st = new StringTokenizer(line, " ");
					int defType = Integer.parseInt(st.nextToken());
					int offset = Integer.parseInt(st.nextToken());
					String name = st.nextToken("\uffff").substring(1);
					ChunkFactory.CommandDefinition def = new ChunkFactory.CommandDefinition(defType, offset, name);
					defsL.add(def);
				} 
				ChunkFactory.CommandDefinition[] defs = defsL.toArray(new ChunkFactory.CommandDefinition[defsL.size()]);
				chunkCommandDefinitions.put(Integer.valueOf(chunkType), defs);
			} 
		} finally {
			if (inp != null) {
				inp.close();
			}
			if (cpd != null) {
				cpd.close();
			}
		}
	}

	public int getVersion() {
		return version;
	}

	public Chunk createChunk(byte[] data, int offset) {
		ChunkHeader header = ChunkHeader.createChunkHeader(version, data, offset);
		if ((header.getLength()) < 0) {
			throw new IllegalArgumentException("Found a chunk with a negative length, which isn't allowed");
		}
		int endOfDataPos = (offset + (header.getLength())) + (header.getSizeInBytes());
		if (endOfDataPos > (data.length)) {
			ChunkFactory.logger.log(POILogger.WARN, (("Header called for " + (header.getLength())) + " bytes, but that would take us past the end of the data!"));
			endOfDataPos = data.length;
			if (header.hasTrailer()) {
				endOfDataPos -= 8;
			}
			if (header.hasSeparator()) {
				endOfDataPos -= 4;
			}
		}
		ChunkTrailer trailer = null;
		ChunkSeparator separator = null;
		if (header.hasTrailer()) {
			if (endOfDataPos <= ((data.length) - 8)) {
				trailer = new ChunkTrailer(data, endOfDataPos);
				endOfDataPos += 8;
			}else {
				ChunkFactory.logger.log(POILogger.ERROR, (((("Header claims a length to " + endOfDataPos) + " there's then no space for the trailer in the data (") + (data.length)) + ")"));
			}
		}
		if (header.hasSeparator()) {
			if (endOfDataPos <= ((data.length) - 4)) {
				separator = new ChunkSeparator(data, endOfDataPos);
			}else {
				ChunkFactory.logger.log(POILogger.ERROR, (((("Header claims a length to " + endOfDataPos) + " there's then no space for the separator in the data (") + (data.length)) + ")"));
			}
		}
		byte[] contents = IOUtils.safelyAllocate(header.getLength(), ChunkFactory.MAX_RECORD_LENGTH);
		System.arraycopy(data, (offset + (header.getSizeInBytes())), contents, 0, contents.length);
		Chunk chunk = new Chunk(header, trailer, separator, contents);
		ChunkFactory.CommandDefinition[] defs = chunkCommandDefinitions.get(Integer.valueOf(header.getType()));
		if (defs == null) {
			defs = new ChunkFactory.CommandDefinition[0];
		}
		return chunk;
	}

	public static class CommandDefinition {
		private int type;

		private int offset;

		private String name;

		public CommandDefinition(int type, int offset, String name) {
			this.type = type;
			this.offset = offset;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public int getOffset() {
			return offset;
		}

		public int getType() {
			return type;
		}
	}
}


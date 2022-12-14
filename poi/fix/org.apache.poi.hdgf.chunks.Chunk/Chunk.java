

import java.nio.charset.Charset;
import java.util.ArrayList;
import org.apache.poi.hdgf.chunks.ChunkFactory;
import org.apache.poi.hdgf.chunks.ChunkHeader;
import org.apache.poi.hdgf.chunks.ChunkSeparator;
import org.apache.poi.hdgf.chunks.ChunkTrailer;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class Chunk {
	private byte[] contents;

	private ChunkHeader header;

	private ChunkTrailer trailer;

	private ChunkSeparator separator;

	protected ChunkFactory.CommandDefinition[] commandDefinitions;

	private Chunk.Command[] commands;

	private String name;

	private POILogger logger = POILogFactory.getLogger(Chunk.class);

	public Chunk(ChunkHeader header, ChunkTrailer trailer, ChunkSeparator separator, byte[] contents) {
		this.header = header;
		this.trailer = trailer;
		this.separator = separator;
		this.contents = contents.clone();
	}

	public byte[] _getContents() {
		return contents;
	}

	public ChunkHeader getHeader() {
		return header;
	}

	public ChunkSeparator getSeparator() {
		return separator;
	}

	public ChunkTrailer getTrailer() {
		return trailer;
	}

	public ChunkFactory.CommandDefinition[] getCommandDefinitions() {
		return commandDefinitions;
	}

	public Chunk.Command[] getCommands() {
		return commands;
	}

	public String getName() {
		return name;
	}

	public int getOnDiskSize() {
		int size = (header.getSizeInBytes()) + (contents.length);
		if ((trailer) != null) {
		}
		if ((separator) != null) {
		}
		return size;
	}

	protected void processCommands() {
		if ((commandDefinitions) == null) {
			throw new IllegalStateException("You must supply the command definitions before calling processCommands!");
		}
		ArrayList<Chunk.Command> commandList = new ArrayList<>();
		for (ChunkFactory.CommandDefinition cdef : commandDefinitions) {
			int type = cdef.getType();
			int offset = cdef.getOffset();
			if (type == 10) {
				name = cdef.getName();
				continue;
			}else
				if (type == 18) {
					continue;
				}

			Chunk.Command command;
			if ((type == 11) || (type == 21)) {
				command = new Chunk.BlockOffsetCommand(cdef);
			}else {
				command = new Chunk.Command(cdef);
			}
			switch (type) {
				case 0 :
				case 1 :
				case 2 :
				case 3 :
				case 4 :
				case 5 :
				case 6 :
				case 7 :
				case 11 :
				case 21 :
				case 12 :
				case 16 :
				case 17 :
				case 18 :
				case 28 :
				case 29 :
					break;
				default :
					if (offset >= 19) {
						offset -= 19;
					}
			}
			if (offset >= (contents.length)) {
				logger.log(POILogger.WARN, ((("Command offset " + offset) + " past end of data at ") + (contents.length)));
				continue;
			}
			try {
				switch (type) {
					case 0 :
					case 1 :
					case 2 :
					case 3 :
					case 4 :
					case 5 :
					case 6 :
					case 7 :
						int val = (contents[offset]) & (1 << type);
						command.value = Boolean.valueOf((val > 0));
						break;
					case 8 :
						command.value = Byte.valueOf(contents[offset]);
						break;
					case 9 :
						command.value = Double.valueOf(LittleEndian.getDouble(contents, offset));
						break;
					case 12 :
						if ((contents.length) < 8) {
							command.value = "";
							break;
						}
						int startsAt = 8;
						int endsAt = startsAt;
						for (int j = startsAt; (j < ((contents.length) - 1)) && (endsAt == startsAt); j++) {
							if (((contents[j]) == 0) && ((contents[(j + 1)]) == 0)) {
								endsAt = j;
							}
						}
						if (endsAt == startsAt) {
							endsAt = contents.length;
						}
						int strLen = endsAt - startsAt;
						command.value = new String(contents, startsAt, strLen, header.getChunkCharset().name());
						break;
					case 25 :
						command.value = Short.valueOf(LittleEndian.getShort(contents, offset));
						break;
					case 26 :
						command.value = Integer.valueOf(LittleEndian.getInt(contents, offset));
						break;
					case 11 :
					case 21 :
						if (offset < ((contents.length) - 3)) {
							int bOffset = ((int) (LittleEndian.getUInt(contents, offset)));
							Chunk.BlockOffsetCommand bcmd = ((Chunk.BlockOffsetCommand) (command));
							bcmd.setOffset(bOffset);
						}
						break;
					default :
						logger.log(POILogger.INFO, (("Command of type " + type) + " not processed!"));
				}
			} catch (Exception e) {
				logger.log(POILogger.ERROR, ("Unexpected error processing command, ignoring and continuing. Command: " + command), e);
			}
			commandList.add(command);
		}
		this.commands = commandList.toArray(new Chunk.Command[commandList.size()]);
	}

	public static class Command {
		protected Object value;

		private ChunkFactory.CommandDefinition definition;

		private Command(ChunkFactory.CommandDefinition definition, Object value) {
			this.definition = definition;
			this.value = value;
		}

		private Command(ChunkFactory.CommandDefinition definition) {
			this(definition, null);
		}

		public ChunkFactory.CommandDefinition getDefinition() {
			return definition;
		}

		public Object getValue() {
			return value;
		}
	}

	private static class BlockOffsetCommand extends Chunk.Command {
		private BlockOffsetCommand(ChunkFactory.CommandDefinition definition) {
			super(definition, null);
		}

		private void setOffset(int offset) {
			value = Integer.valueOf(offset);
		}
	}
}


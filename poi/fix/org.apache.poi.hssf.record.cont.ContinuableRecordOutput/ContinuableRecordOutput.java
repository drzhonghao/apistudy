

import org.apache.poi.util.DelayableLittleEndianOutput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.StringUtil;


public final class ContinuableRecordOutput implements LittleEndianOutput {
	private final LittleEndianOutput _out;

	private int _totalPreviousRecordsSize;

	public ContinuableRecordOutput(LittleEndianOutput out, int sid) {
		_out = out;
		_totalPreviousRecordsSize = 0;
	}

	public static ContinuableRecordOutput createForCountingOnly() {
		return new ContinuableRecordOutput(ContinuableRecordOutput.NOPOutput, (-777));
	}

	public int getTotalSize() {
		return 0;
	}

	void terminate() {
	}

	public int getAvailableSpace() {
		return 0;
	}

	public void writeContinue() {
	}

	public void writeContinueIfRequired(int requiredContinuousSize) {
	}

	public void writeStringData(String text) {
		boolean is16bitEncoded = StringUtil.hasMultibyte(text);
		int keepTogetherSize = 1 + 1;
		int optionFlags = 0;
		if (is16bitEncoded) {
			optionFlags |= 1;
			keepTogetherSize += 1;
		}
		writeContinueIfRequired(keepTogetherSize);
		writeByte(optionFlags);
		writeCharacterData(text, is16bitEncoded);
	}

	public void writeString(String text, int numberOfRichTextRuns, int extendedDataSize) {
		boolean is16bitEncoded = StringUtil.hasMultibyte(text);
		int keepTogetherSize = (2 + 1) + 1;
		int optionFlags = 0;
		if (is16bitEncoded) {
			optionFlags |= 1;
			keepTogetherSize += 1;
		}
		if (numberOfRichTextRuns > 0) {
			optionFlags |= 8;
			keepTogetherSize += 2;
		}
		if (extendedDataSize > 0) {
			optionFlags |= 4;
			keepTogetherSize += 4;
		}
		writeContinueIfRequired(keepTogetherSize);
		writeShort(text.length());
		writeByte(optionFlags);
		if (numberOfRichTextRuns > 0) {
			writeShort(numberOfRichTextRuns);
		}
		if (extendedDataSize > 0) {
			writeInt(extendedDataSize);
		}
		writeCharacterData(text, is16bitEncoded);
	}

	private void writeCharacterData(String text, boolean is16bitEncoded) {
		int nChars = text.length();
		int i = 0;
		if (is16bitEncoded) {
			while (true) {
				if (i >= nChars) {
					break;
				}
				writeContinue();
				writeByte(1);
			} 
		}else {
			while (true) {
				if (i >= nChars) {
					break;
				}
				writeContinue();
				writeByte(0);
			} 
		}
	}

	public void write(byte[] b) {
		writeContinueIfRequired(b.length);
	}

	public void write(byte[] b, int offset, int len) {
		int i = 0;
		while (true) {
			if (i >= len) {
				break;
			}
			writeContinue();
		} 
	}

	public void writeByte(int v) {
		writeContinueIfRequired(1);
	}

	public void writeDouble(double v) {
		writeContinueIfRequired(8);
	}

	public void writeInt(int v) {
		writeContinueIfRequired(4);
	}

	public void writeLong(long v) {
		writeContinueIfRequired(8);
	}

	public void writeShort(int v) {
		writeContinueIfRequired(2);
	}

	private static final LittleEndianOutput NOPOutput = new DelayableLittleEndianOutput() {
		public LittleEndianOutput createDelayedOutput(int size) {
			return this;
		}

		public void write(byte[] b) {
		}

		public void write(byte[] b, int offset, int len) {
		}

		public void writeByte(int v) {
		}

		public void writeDouble(double v) {
		}

		public void writeInt(int v) {
		}

		public void writeLong(long v) {
		}

		public void writeShort(int v) {
		}
	};
}


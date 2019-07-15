

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;


public class Stats {
	public long indexNumBytes;

	public long totalTermCount;

	public long totalTermBytes;

	public int nonFloorBlockCount;

	public int floorBlockCount;

	public int floorSubBlockCount;

	public int mixedBlockCount;

	public int termsOnlyBlockCount;

	public int subBlocksOnlyBlockCount;

	public int totalBlockCount;

	public int[] blockCountByPrefixLen = new int[10];

	private int startBlockCount;

	private int endBlockCount;

	public long totalBlockSuffixBytes;

	public long totalBlockStatsBytes;

	public long totalBlockOtherBytes;

	public final String segment;

	public final String field;

	Stats(String segment, String field) {
		this.segment = segment;
		this.field = field;
	}

	void term(BytesRef term) {
		totalTermBytes += term.length;
	}

	void finish() {
		assert (startBlockCount) == (endBlockCount) : (("startBlockCount=" + (startBlockCount)) + " endBlockCount=") + (endBlockCount);
		assert (totalBlockCount) == ((floorSubBlockCount) + (nonFloorBlockCount)) : (((("floorSubBlockCount=" + (floorSubBlockCount)) + " nonFloorBlockCount=") + (nonFloorBlockCount)) + " totalBlockCount=") + (totalBlockCount);
		assert (totalBlockCount) == (((mixedBlockCount) + (termsOnlyBlockCount)) + (subBlocksOnlyBlockCount)) : (((((("totalBlockCount=" + (totalBlockCount)) + " mixedBlockCount=") + (mixedBlockCount)) + " subBlocksOnlyBlockCount=") + (subBlocksOnlyBlockCount)) + " termsOnlyBlockCount=") + (termsOnlyBlockCount);
	}

	@Override
	public String toString() {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		PrintStream out;
		try {
			out = new PrintStream(bos, false, IOUtils.UTF_8);
		} catch (UnsupportedEncodingException bogus) {
			throw new RuntimeException(bogus);
		}
		out.println("  index FST:");
		out.println((("    " + (indexNumBytes)) + " bytes"));
		out.println("  terms:");
		out.println((("    " + (totalTermCount)) + " terms"));
		out.println(((("    " + (totalTermBytes)) + " bytes") + ((totalTermCount) != 0 ? (" (" + (String.format(Locale.ROOT, "%.1f", (((double) (totalTermBytes)) / (totalTermCount))))) + " bytes/term)" : "")));
		out.println("  blocks:");
		out.println((("    " + (totalBlockCount)) + " blocks"));
		out.println((("    " + (termsOnlyBlockCount)) + " terms-only blocks"));
		out.println((("    " + (subBlocksOnlyBlockCount)) + " sub-block-only blocks"));
		out.println((("    " + (mixedBlockCount)) + " mixed blocks"));
		out.println((("    " + (floorBlockCount)) + " floor blocks"));
		out.println((("    " + ((totalBlockCount) - (floorSubBlockCount))) + " non-floor blocks"));
		out.println((("    " + (floorSubBlockCount)) + " floor sub-blocks"));
		out.println(((("    " + (totalBlockSuffixBytes)) + " term suffix bytes") + ((totalBlockCount) != 0 ? (" (" + (String.format(Locale.ROOT, "%.1f", (((double) (totalBlockSuffixBytes)) / (totalBlockCount))))) + " suffix-bytes/block)" : "")));
		out.println(((("    " + (totalBlockStatsBytes)) + " term stats bytes") + ((totalBlockCount) != 0 ? (" (" + (String.format(Locale.ROOT, "%.1f", (((double) (totalBlockStatsBytes)) / (totalBlockCount))))) + " stats-bytes/block)" : "")));
		out.println(((("    " + (totalBlockOtherBytes)) + " other bytes") + ((totalBlockCount) != 0 ? (" (" + (String.format(Locale.ROOT, "%.1f", (((double) (totalBlockOtherBytes)) / (totalBlockCount))))) + " other-bytes/block)" : "")));
		if ((totalBlockCount) != 0) {
			out.println("    by prefix length:");
			int total = 0;
			for (int prefix = 0; prefix < (blockCountByPrefixLen.length); prefix++) {
				final int blockCount = blockCountByPrefixLen[prefix];
				total += blockCount;
				if (blockCount != 0) {
					out.println(((("      " + (String.format(Locale.ROOT, "%2d", prefix))) + ": ") + blockCount));
				}
			}
			assert (totalBlockCount) == total;
		}
		try {
			return bos.toString(IOUtils.UTF_8);
		} catch (UnsupportedEncodingException bogus) {
			throw new RuntimeException(bogus);
		}
	}
}


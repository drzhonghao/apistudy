

import com.google.common.collect.MapMaker;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.cassandra.cache.IMeasurableMemory;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.memory.AbstractAllocator;


public class ColumnIdentifier implements Comparable<ColumnIdentifier> , IMeasurableMemory {
	private static final Pattern PATTERN_DOUBLE_QUOTE = Pattern.compile("\"", Pattern.LITERAL);

	private static final String ESCAPED_DOUBLE_QUOTE = Matcher.quoteReplacement("\"\"");

	public final ByteBuffer bytes;

	private final String text;

	public final long prefixComparison;

	private final boolean interned;

	private static final Pattern UNQUOTED_IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

	private static final long EMPTY_SIZE = ObjectSizes.measure(new ColumnIdentifier(ByteBufferUtil.EMPTY_BYTE_BUFFER, "", false));

	private static final ConcurrentMap<ColumnIdentifier.InternedKey, ColumnIdentifier> internedInstances = new MapMaker().weakValues().makeMap();

	private static final class InternedKey {
		private final AbstractType<?> type;

		private final ByteBuffer bytes;

		InternedKey(AbstractType<?> type, ByteBuffer bytes) {
			this.type = type;
			this.bytes = bytes;
		}

		@Override
		public boolean equals(Object o) {
			if ((this) == o)
				return true;

			if ((o == null) || ((getClass()) != (o.getClass())))
				return false;

			ColumnIdentifier.InternedKey that = ((ColumnIdentifier.InternedKey) (o));
			return (bytes.equals(that.bytes)) && (type.equals(that.type));
		}

		@Override
		public int hashCode() {
			return (bytes.hashCode()) + (31 * (type.hashCode()));
		}
	}

	private static long prefixComparison(ByteBuffer bytes) {
		long prefix = 0;
		ByteBuffer read = bytes.duplicate();
		int i = 0;
		while ((read.hasRemaining()) && (i < 8)) {
			prefix <<= 8;
			prefix |= (read.get()) & 255;
			i++;
		} 
		prefix <<= (8 - i) * 8;
		prefix ^= Long.MIN_VALUE;
		return prefix;
	}

	public ColumnIdentifier(String rawText, boolean keepCase) {
		this.text = (keepCase) ? rawText : rawText.toLowerCase(Locale.US);
		this.bytes = ByteBufferUtil.bytes(this.text);
		this.prefixComparison = ColumnIdentifier.prefixComparison(bytes);
		this.interned = false;
	}

	public ColumnIdentifier(ByteBuffer bytes, AbstractType<?> type) {
		this(bytes, type.getString(bytes), false);
	}

	private ColumnIdentifier(ByteBuffer bytes, String text, boolean interned) {
		this.bytes = bytes;
		this.text = text;
		this.interned = interned;
		this.prefixComparison = ColumnIdentifier.prefixComparison(bytes);
	}

	public static ColumnIdentifier getInterned(ByteBuffer bytes, AbstractType<?> type) {
		return ColumnIdentifier.getInterned(type, bytes, type.getString(bytes));
	}

	public static ColumnIdentifier getInterned(String rawText, boolean keepCase) {
		String text = (keepCase) ? rawText : rawText.toLowerCase(Locale.US);
		ByteBuffer bytes = ByteBufferUtil.bytes(text);
		return ColumnIdentifier.getInterned(UTF8Type.instance, bytes, text);
	}

	public static ColumnIdentifier getInterned(AbstractType<?> type, ByteBuffer bytes, String text) {
		bytes = ByteBufferUtil.minimalBufferFor(bytes);
		ColumnIdentifier.InternedKey key = new ColumnIdentifier.InternedKey(type, bytes);
		ColumnIdentifier id = ColumnIdentifier.internedInstances.get(key);
		if (id != null)
			return id;

		ColumnIdentifier created = new ColumnIdentifier(bytes, text, true);
		ColumnIdentifier previous = ColumnIdentifier.internedInstances.putIfAbsent(key, created);
		return previous == null ? created : previous;
	}

	public boolean isInterned() {
		return interned;
	}

	@Override
	public final int hashCode() {
		return bytes.hashCode();
	}

	@Override
	public final boolean equals(Object o) {
		if ((this) == o)
			return true;

		if (!(o instanceof ColumnIdentifier))
			return false;

		ColumnIdentifier that = ((ColumnIdentifier) (o));
		return bytes.equals(that.bytes);
	}

	@Override
	public String toString() {
		return text;
	}

	public String toCQLString() {
		return ColumnIdentifier.maybeQuote(text);
	}

	public long unsharedHeapSize() {
		return ((ColumnIdentifier.EMPTY_SIZE) + (ObjectSizes.sizeOnHeapOf(bytes))) + (ObjectSizes.sizeOf(text));
	}

	public long unsharedHeapSizeExcludingData() {
		return ((ColumnIdentifier.EMPTY_SIZE) + (ObjectSizes.sizeOnHeapExcludingData(bytes))) + (ObjectSizes.sizeOf(text));
	}

	public ColumnIdentifier clone(AbstractAllocator allocator) {
		return interned ? this : new ColumnIdentifier(allocator.clone(bytes), text, false);
	}

	public int compareTo(ColumnIdentifier that) {
		int c = Long.compare(this.prefixComparison, that.prefixComparison);
		if (c != 0)
			return c;

		if ((this) == that)
			return 0;

		return ByteBufferUtil.compareUnsigned(this.bytes, that.bytes);
	}

	@com.google.common.annotations.VisibleForTesting
	public static String maybeQuote(String text) {
		return ('"' + (ColumnIdentifier.PATTERN_DOUBLE_QUOTE.matcher(text).replaceAll(ColumnIdentifier.ESCAPED_DOUBLE_QUOTE))) + '"';
	}
}


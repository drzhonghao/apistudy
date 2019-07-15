

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.IPartitionerDependentSerializer;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.RingPosition;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.utils.Pair;


public abstract class AbstractBounds<T extends RingPosition<T>> implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final IPartitionerDependentSerializer<AbstractBounds<Token>> tokenSerializer = new AbstractBounds.AbstractBoundsSerializer<Token>(Token.serializer);

	public static final IPartitionerDependentSerializer<AbstractBounds<PartitionPosition>> rowPositionSerializer = new AbstractBounds.AbstractBoundsSerializer<PartitionPosition>(PartitionPosition.serializer);

	private enum Type {

		RANGE,
		BOUNDS;}

	public final T left;

	public final T right;

	public AbstractBounds(T left, T right) {
		assert (left.getPartitioner()) == (right.getPartitioner());
		this.left = left;
		this.right = right;
	}

	public abstract Pair<AbstractBounds<T>, AbstractBounds<T>> split(T position);

	public abstract boolean inclusiveLeft();

	public abstract boolean inclusiveRight();

	public static <T extends RingPosition<T>> boolean strictlyWrapsAround(T left, T right) {
		return !(((left.compareTo(right)) <= 0) || (right.isMinimum()));
	}

	public static <T extends RingPosition<T>> boolean noneStrictlyWrapsAround(Collection<AbstractBounds<T>> bounds) {
		for (AbstractBounds<T> b : bounds) {
			if (AbstractBounds.strictlyWrapsAround(b.left, b.right))
				return false;

		}
		return true;
	}

	@Override
	public int hashCode() {
		return (31 * (left.hashCode())) + (right.hashCode());
	}

	public boolean intersects(Iterable<Range<T>> ranges) {
		for (Range<T> range2 : ranges) {
		}
		return false;
	}

	public abstract boolean contains(T start);

	public abstract List<? extends AbstractBounds<T>> unwrap();

	public String getString(AbstractType<?> keyValidator) {
		return ((((getOpeningString()) + (format(left, keyValidator))) + ", ") + (format(right, keyValidator))) + (getClosingString());
	}

	private String format(T value, AbstractType<?> keyValidator) {
		if (value instanceof DecoratedKey) {
			return keyValidator.getString(((DecoratedKey) (value)).getKey());
		}else {
			return value.toString();
		}
	}

	protected abstract String getOpeningString();

	protected abstract String getClosingString();

	public abstract boolean isStartInclusive();

	public abstract boolean isEndInclusive();

	public abstract AbstractBounds<T> withNewRight(T newRight);

	public static class AbstractBoundsSerializer<T extends RingPosition<T>> implements IPartitionerDependentSerializer<AbstractBounds<T>> {
		private static final int IS_TOKEN_FLAG = 1;

		private static final int START_INCLUSIVE_FLAG = 2;

		private static final int END_INCLUSIVE_FLAG = 4;

		IPartitionerDependentSerializer<T> serializer;

		private static int kindInt(AbstractBounds<?> ab) {
			if (!((ab.left) instanceof Token)) {
			}
			return 0;
		}

		private static int kindFlags(AbstractBounds<?> ab) {
			int flags = 0;
			if ((ab.left) instanceof Token)
				flags |= AbstractBounds.AbstractBoundsSerializer.IS_TOKEN_FLAG;

			if (ab.isStartInclusive())
				flags |= AbstractBounds.AbstractBoundsSerializer.START_INCLUSIVE_FLAG;

			if (ab.isEndInclusive())
				flags |= AbstractBounds.AbstractBoundsSerializer.END_INCLUSIVE_FLAG;

			return flags;
		}

		public AbstractBoundsSerializer(IPartitionerDependentSerializer<T> serializer) {
			this.serializer = serializer;
		}

		public void serialize(AbstractBounds<T> range, DataOutputPlus out, int version) throws IOException {
			if (version < (MessagingService.VERSION_30))
				out.writeInt(AbstractBounds.AbstractBoundsSerializer.kindInt(range));
			else
				out.writeByte(AbstractBounds.AbstractBoundsSerializer.kindFlags(range));

			serializer.serialize(range.left, out, version);
			serializer.serialize(range.right, out, version);
		}

		public AbstractBounds<T> deserialize(DataInput in, IPartitioner p, int version) throws IOException {
			boolean isToken;
			boolean startInclusive;
			boolean endInclusive;
			if (version < (MessagingService.VERSION_30)) {
				int kind = in.readInt();
				isToken = kind >= 0;
				if (!isToken)
					kind = -(kind + 1);

				startInclusive = kind != (AbstractBounds.Type.RANGE.ordinal());
				endInclusive = true;
			}else {
				int flags = in.readUnsignedByte();
				isToken = (flags & (AbstractBounds.AbstractBoundsSerializer.IS_TOKEN_FLAG)) != 0;
				startInclusive = (flags & (AbstractBounds.AbstractBoundsSerializer.START_INCLUSIVE_FLAG)) != 0;
				endInclusive = (flags & (AbstractBounds.AbstractBoundsSerializer.END_INCLUSIVE_FLAG)) != 0;
			}
			T left = serializer.deserialize(in, p, version);
			T right = serializer.deserialize(in, p, version);
			assert isToken == (left instanceof Token);
			if (startInclusive) {
			}else {
			}
			return null;
		}

		public long serializedSize(AbstractBounds<T> ab, int version) {
			int size = (version < (MessagingService.VERSION_30)) ? TypeSizes.sizeof(AbstractBounds.AbstractBoundsSerializer.kindInt(ab)) : 1;
			size += serializer.serializedSize(ab.left, version);
			size += serializer.serializedSize(ab.right, version);
			return size;
		}
	}

	public static <T extends RingPosition<T>> AbstractBounds<T> bounds(AbstractBounds.Boundary<T> min, AbstractBounds.Boundary<T> max) {
		return AbstractBounds.bounds(min.boundary, min.inclusive, max.boundary, max.inclusive);
	}

	public static <T extends RingPosition<T>> AbstractBounds<T> bounds(T min, boolean inclusiveMin, T max, boolean inclusiveMax) {
		if (inclusiveMin && inclusiveMax) {
		}else
			if (inclusiveMax) {
			}else
				if (inclusiveMin) {
				}else {
				}


		return null;
	}

	public static class Boundary<T extends RingPosition<T>> {
		public final T boundary;

		public final boolean inclusive;

		public Boundary(T boundary, boolean inclusive) {
			this.boundary = boundary;
			this.inclusive = inclusive;
		}
	}

	public AbstractBounds.Boundary<T> leftBoundary() {
		return new AbstractBounds.Boundary<>(left, inclusiveLeft());
	}

	public AbstractBounds.Boundary<T> rightBoundary() {
		return new AbstractBounds.Boundary<>(right, inclusiveRight());
	}

	public static <T extends RingPosition<T>> boolean isEmpty(AbstractBounds.Boundary<T> left, AbstractBounds.Boundary<T> right) {
		int c = left.boundary.compareTo(right.boundary);
		return (c > 0) || ((c == 0) && (!((left.inclusive) && (right.inclusive))));
	}

	public static <T extends RingPosition<T>> AbstractBounds.Boundary<T> minRight(AbstractBounds.Boundary<T> right1, T right2, boolean isInclusiveRight2) {
		return AbstractBounds.minRight(right1, new AbstractBounds.Boundary<T>(right2, isInclusiveRight2));
	}

	public static <T extends RingPosition<T>> AbstractBounds.Boundary<T> minRight(AbstractBounds.Boundary<T> right1, AbstractBounds.Boundary<T> right2) {
		int c = right1.boundary.compareTo(right2.boundary);
		if (c != 0)
			return c < 0 ? right1 : right2;

		return right2.inclusive ? right1 : right2;
	}

	public static <T extends RingPosition<T>> AbstractBounds.Boundary<T> maxLeft(AbstractBounds.Boundary<T> left1, T left2, boolean isInclusiveLeft2) {
		return AbstractBounds.maxLeft(left1, new AbstractBounds.Boundary<T>(left2, isInclusiveLeft2));
	}

	public static <T extends RingPosition<T>> AbstractBounds.Boundary<T> maxLeft(AbstractBounds.Boundary<T> left1, AbstractBounds.Boundary<T> left2) {
		int c = left1.boundary.compareTo(left2.boundary);
		if (c != 0)
			return c > 0 ? left1 : left2;

		return left2.inclusive ? left1 : left2;
	}
}


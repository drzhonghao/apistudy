

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.cassandra.hints.Hint;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.io.util.TrackedDataInputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.utils.UUIDSerializer;

import static org.apache.cassandra.net.MessagingService.Verb.HINT;


public final class HintMessage {
	public static final IVersionedSerializer<HintMessage> serializer = new HintMessage.Serializer();

	final UUID hostId;

	@Nullable
	final Hint hint;

	@Nullable
	final UUID unknownTableID;

	HintMessage(UUID hostId, Hint hint) {
		this.hostId = hostId;
		this.hint = hint;
		this.unknownTableID = null;
	}

	HintMessage(UUID hostId, UUID unknownTableID) {
		this.hostId = hostId;
		this.hint = null;
		this.unknownTableID = unknownTableID;
	}

	public MessageOut<HintMessage> createMessageOut() {
		return new MessageOut<>(HINT, this, HintMessage.serializer);
	}

	public static class Serializer implements IVersionedSerializer<HintMessage> {
		public long serializedSize(HintMessage message, int version) {
			long size = UUIDSerializer.serializer.serializedSize(message.hostId, version);
			return size;
		}

		public void serialize(HintMessage message, DataOutputPlus out, int version) throws IOException {
			Objects.requireNonNull(message.hint);
			UUIDSerializer.serializer.serialize(message.hostId, out, version);
		}

		public HintMessage deserialize(DataInputPlus in, int version) throws IOException {
			UUID hostId = UUIDSerializer.serializer.deserialize(in, version);
			long hintSize = in.readUnsignedVInt();
			TrackedDataInputPlus countingIn = new TrackedDataInputPlus(in);
			return null;
		}
	}
}


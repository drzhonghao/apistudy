

import com.google.common.collect.ImmutableMap;
import java.io.DataInput;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.monitoring.ApproximateTime;
import org.apache.cassandra.exceptions.RequestFailureReason;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputBuffer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.metrics.MessagingMetrics;
import org.apache.cassandra.net.CompactEndpointSerializationHelper;
import org.apache.cassandra.net.MessagingService;


public class MessageIn<T> {
	public final InetAddress from;

	public final T payload;

	public final Map<String, byte[]> parameters;

	public final MessagingService.Verb verb;

	public final int version;

	public final long constructionTime;

	private MessageIn(InetAddress from, T payload, Map<String, byte[]> parameters, MessagingService.Verb verb, int version, long constructionTime) {
		this.from = from;
		this.payload = payload;
		this.parameters = parameters;
		this.verb = verb;
		this.version = version;
		this.constructionTime = constructionTime;
	}

	public static <T> MessageIn<T> create(InetAddress from, T payload, Map<String, byte[]> parameters, MessagingService.Verb verb, int version, long constructionTime) {
		return new MessageIn<>(from, payload, parameters, verb, version, constructionTime);
	}

	public static <T> MessageIn<T> create(InetAddress from, T payload, Map<String, byte[]> parameters, MessagingService.Verb verb, int version) {
		return new MessageIn<>(from, payload, parameters, verb, version, ApproximateTime.currentTimeMillis());
	}

	public static <T2> MessageIn<T2> read(DataInputPlus in, int version, int id) throws IOException {
		return MessageIn.read(in, version, id, ApproximateTime.currentTimeMillis());
	}

	public static <T2> MessageIn<T2> read(DataInputPlus in, int version, int id, long constructionTime) throws IOException {
		InetAddress from = CompactEndpointSerializationHelper.deserialize(in);
		MessagingService.Verb verb = MessagingService.verbValues[in.readInt()];
		int parameterCount = in.readInt();
		Map<String, byte[]> parameters;
		if (parameterCount == 0) {
			parameters = Collections.emptyMap();
		}else {
			ImmutableMap.Builder<String, byte[]> builder = ImmutableMap.builder();
			for (int i = 0; i < parameterCount; i++) {
				String key = in.readUTF();
				byte[] value = new byte[in.readInt()];
				in.readFully(value);
				builder.put(key, value);
			}
			parameters = builder.build();
		}
		int payloadSize = in.readInt();
		IVersionedSerializer<T2> serializer = ((IVersionedSerializer<T2>) (MessagingService.instance().verbSerializers.get(verb)));
		if ((payloadSize == 0) || (serializer == null))
			return MessageIn.create(from, null, parameters, verb, version, constructionTime);

		T2 payload = serializer.deserialize(in, version);
		return MessageIn.create(from, payload, parameters, verb, version, constructionTime);
	}

	public static long readConstructionTime(InetAddress from, DataInputPlus input, long currentTime) throws IOException {
		int partial = input.readInt();
		long sentConstructionTime = (currentTime & -4294967296L) | (((partial & 4294967295L) << 2) >> 2);
		long elapsed = currentTime - sentConstructionTime;
		if (elapsed > 0)
			MessagingService.instance().metrics.addTimeTaken(from, elapsed);

		boolean useSentTime = (DatabaseDescriptor.hasCrossNodeTimeout()) && (elapsed > 0);
		return useSentTime ? sentConstructionTime : currentTime;
	}

	public long getLifetimeInMS() {
		return (ApproximateTime.currentTimeMillis()) - (constructionTime);
	}

	public boolean isCrossNode() {
		return !(from.equals(DatabaseDescriptor.getBroadcastAddress()));
	}

	public Stage getMessageType() {
		return MessagingService.verbStages.get(verb);
	}

	public boolean doCallbackOnFailure() {
		return parameters.containsKey(MessagingService.FAILURE_CALLBACK_PARAM);
	}

	public boolean isFailureResponse() {
		return parameters.containsKey(MessagingService.FAILURE_RESPONSE_PARAM);
	}

	public boolean containsFailureReason() {
		return parameters.containsKey(MessagingService.FAILURE_REASON_PARAM);
	}

	public RequestFailureReason getFailureReason() {
		if (containsFailureReason()) {
			try (DataInputBuffer in = new DataInputBuffer(parameters.get(MessagingService.FAILURE_REASON_PARAM))) {
				return RequestFailureReason.fromCode(in.readUnsignedShort());
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}else {
			return RequestFailureReason.UNKNOWN;
		}
	}

	public long getTimeout() {
		return verb.getTimeout();
	}

	public long getSlowQueryTimeout() {
		return DatabaseDescriptor.getSlowQueryTimeout();
	}

	public String toString() {
		StringBuilder sbuf = new StringBuilder();
		sbuf.append("FROM:").append(from).append(" TYPE:").append(getMessageType()).append(" VERB:").append(verb);
		return sbuf.toString();
	}
}


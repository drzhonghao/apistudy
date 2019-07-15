

import com.datastax.driver.core.TypeCodec;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.cassandra.cql3.functions.UDFContext;
import org.apache.cassandra.cql3.functions.UDHelper;
import org.apache.cassandra.transport.ProtocolVersion;


public abstract class JavaUDF {
	private final TypeCodec<Object> returnCodec;

	private final TypeCodec<Object>[] argCodecs;

	protected final UDFContext udfContext;

	protected JavaUDF(TypeCodec<Object> returnCodec, TypeCodec<Object>[] argCodecs, UDFContext udfContext) {
		this.returnCodec = returnCodec;
		this.argCodecs = argCodecs;
		this.udfContext = udfContext;
	}

	protected abstract ByteBuffer executeImpl(ProtocolVersion protocolVersion, List<ByteBuffer> params);

	protected abstract Object executeAggregateImpl(ProtocolVersion protocolVersion, Object firstParam, List<ByteBuffer> params);

	protected Object compose(ProtocolVersion protocolVersion, int argIndex, ByteBuffer value) {
		return null;
	}

	protected ByteBuffer decompose(ProtocolVersion protocolVersion, Object value) {
		return null;
	}

	protected float compose_float(ProtocolVersion protocolVersion, int argIndex, ByteBuffer value) {
		assert (value != null) && ((value.remaining()) > 0);
		return ((float) (UDHelper.deserialize(TypeCodec.cfloat(), protocolVersion, value)));
	}

	protected double compose_double(ProtocolVersion protocolVersion, int argIndex, ByteBuffer value) {
		assert (value != null) && ((value.remaining()) > 0);
		return ((double) (UDHelper.deserialize(TypeCodec.cdouble(), protocolVersion, value)));
	}

	protected byte compose_byte(ProtocolVersion protocolVersion, int argIndex, ByteBuffer value) {
		assert (value != null) && ((value.remaining()) > 0);
		return ((byte) (UDHelper.deserialize(TypeCodec.tinyInt(), protocolVersion, value)));
	}

	protected short compose_short(ProtocolVersion protocolVersion, int argIndex, ByteBuffer value) {
		assert (value != null) && ((value.remaining()) > 0);
		return ((short) (UDHelper.deserialize(TypeCodec.smallInt(), protocolVersion, value)));
	}

	protected int compose_int(ProtocolVersion protocolVersion, int argIndex, ByteBuffer value) {
		assert (value != null) && ((value.remaining()) > 0);
		return ((int) (UDHelper.deserialize(TypeCodec.cint(), protocolVersion, value)));
	}

	protected long compose_long(ProtocolVersion protocolVersion, int argIndex, ByteBuffer value) {
		assert (value != null) && ((value.remaining()) > 0);
		return ((long) (UDHelper.deserialize(TypeCodec.bigint(), protocolVersion, value)));
	}

	protected boolean compose_boolean(ProtocolVersion protocolVersion, int argIndex, ByteBuffer value) {
		assert (value != null) && ((value.remaining()) > 0);
		return ((boolean) (UDHelper.deserialize(TypeCodec.cboolean(), protocolVersion, value)));
	}
}


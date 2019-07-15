

import java.nio.ByteBuffer;
import java.util.List;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.selection.Selection;
import org.apache.cassandra.cql3.selection.Selector;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.transport.ProtocolVersion;


final class ScalarFunctionSelector {
	public boolean isAggregate() {
		return false;
	}

	public void addInput(ProtocolVersion protocolVersion, Selection.ResultSetBuilder rs) throws InvalidRequestException {
	}

	public void reset() {
	}

	public ByteBuffer getOutput(ProtocolVersion protocolVersion) throws InvalidRequestException {
		return null;
	}

	ScalarFunctionSelector(Function fun, List<Selector> argSelectors) {
	}
}


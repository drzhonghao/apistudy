

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.accumulo.core.data.ColumnUpdate;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.thrift.TMutation;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;

import static org.apache.accumulo.core.data.Mutation.SERIALIZED_FORMAT.VERSION2;


public class ServerMutation extends Mutation {
	private long systemTime = 0L;

	public ServerMutation(TMutation tmutation) {
		super(tmutation);
	}

	public ServerMutation(Text key) {
		super(key);
	}

	public ServerMutation() {
	}

	@Override
	protected void droppingOldTimestamp(long ts) {
		this.systemTime = ts;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		if ((getSerializedFormat()) == (VERSION2))
			systemTime = WritableUtils.readVLong(in);

	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		WritableUtils.writeVLong(out, systemTime);
	}

	public void setSystemTimestamp(long v) {
		this.systemTime = v;
	}

	public long getSystemTimestamp() {
		return this.systemTime;
	}

	@Override
	protected ColumnUpdate newColumnUpdate(byte[] cf, byte[] cq, byte[] cv, boolean hasts, long ts, boolean deleted, byte[] val) {
		return null;
	}

	@Override
	public long estimatedMemoryUsed() {
		return (super.estimatedMemoryUsed()) + 8;
	}

	@Override
	public boolean equals(Object o) {
		return (o == (this)) || ((((o != null) && (o instanceof ServerMutation)) && ((systemTime) == (((ServerMutation) (o)).systemTime))) && (super.equals(o)));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + ((int) ((systemTime) & (-1)));
		return result;
	}
}


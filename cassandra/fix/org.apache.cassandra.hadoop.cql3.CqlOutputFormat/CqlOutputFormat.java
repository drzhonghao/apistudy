

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.cassandra.hadoop.HadoopCompat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.TaskAttemptContext;


public class CqlOutputFormat extends OutputFormat<Map<String, ByteBuffer>, List<ByteBuffer>> implements org.apache.hadoop.mapred.OutputFormat<Map<String, ByteBuffer>, List<ByteBuffer>> {
	public static final String BATCH_THRESHOLD = "mapreduce.output.columnfamilyoutputformat.batch.threshold";

	public static final String QUEUE_SIZE = "mapreduce.output.columnfamilyoutputformat.queue.size";

	public void checkOutputSpecs(JobContext context) {
		checkOutputSpecs(HadoopCompat.getConfiguration(context));
	}

	protected void checkOutputSpecs(Configuration conf) {
		if ((ConfigHelper.getOutputKeyspace(conf)) == null)
			throw new UnsupportedOperationException("You must set the keyspace with setOutputKeyspace()");

		if ((ConfigHelper.getOutputPartitioner(conf)) == null)
			throw new UnsupportedOperationException("You must set the output partitioner to the one used by your Cassandra cluster");

		if ((ConfigHelper.getOutputInitialAddress(conf)) == null)
			throw new UnsupportedOperationException("You must set the initial output address to a Cassandra node");

	}

	@Deprecated
	public void checkOutputSpecs(FileSystem filesystem, JobConf job) throws IOException {
		checkOutputSpecs(job);
	}

	public OutputCommitter getOutputCommitter(TaskAttemptContext context) throws IOException, InterruptedException {
		return new CqlOutputFormat.NullOutputCommitter();
	}

	private static class NullOutputCommitter extends OutputCommitter {
		public void abortTask(TaskAttemptContext taskContext) {
		}

		public void cleanupJob(JobContext jobContext) {
		}

		public void commitTask(TaskAttemptContext taskContext) {
		}

		public boolean needsTaskCommit(TaskAttemptContext taskContext) {
			return false;
		}

		public void setupJob(JobContext jobContext) {
		}

		public void setupTask(TaskAttemptContext taskContext) {
		}
	}
}


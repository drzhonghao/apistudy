

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.cassandra.hadoop.HadoopCompat;
import org.apache.cassandra.hadoop.cql3.CqlBulkRecordWriter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.util.Progressable;


public class CqlBulkOutputFormat extends OutputFormat<Object, List<ByteBuffer>> implements org.apache.hadoop.mapred.OutputFormat<Object, List<ByteBuffer>> {
	private static final String OUTPUT_CQL_SCHEMA_PREFIX = "cassandra.table.schema.";

	private static final String OUTPUT_CQL_INSERT_PREFIX = "cassandra.table.insert.";

	private static final String DELETE_SOURCE = "cassandra.output.delete.source";

	private static final String TABLE_ALIAS_PREFIX = "cqlbulkoutputformat.table.alias.";

	@Deprecated
	public CqlBulkRecordWriter getRecordWriter(FileSystem filesystem, JobConf job, String name, Progressable progress) throws IOException {
		return null;
	}

	public CqlBulkRecordWriter getRecordWriter(final TaskAttemptContext context) throws IOException, InterruptedException {
		return null;
	}

	@Override
	public void checkOutputSpecs(JobContext context) {
		checkOutputSpecs(HadoopCompat.getConfiguration(context));
	}

	private void checkOutputSpecs(Configuration conf) {
		if ((ConfigHelper.getOutputKeyspace(conf)) == null) {
			throw new UnsupportedOperationException("you must set the keyspace with setTable()");
		}
	}

	@Deprecated
	public void checkOutputSpecs(FileSystem filesystem, JobConf job) throws IOException {
		checkOutputSpecs(job);
	}

	@Override
	public OutputCommitter getOutputCommitter(TaskAttemptContext context) throws IOException, InterruptedException {
		return new CqlBulkOutputFormat.NullOutputCommitter();
	}

	public static void setTableSchema(Configuration conf, String columnFamily, String schema) {
		conf.set(((CqlBulkOutputFormat.OUTPUT_CQL_SCHEMA_PREFIX) + columnFamily), schema);
	}

	public static void setTableInsertStatement(Configuration conf, String columnFamily, String insertStatement) {
		conf.set(((CqlBulkOutputFormat.OUTPUT_CQL_INSERT_PREFIX) + columnFamily), insertStatement);
	}

	public static String getTableSchema(Configuration conf, String columnFamily) {
		String schema = conf.get(((CqlBulkOutputFormat.OUTPUT_CQL_SCHEMA_PREFIX) + columnFamily));
		if (schema == null) {
			throw new UnsupportedOperationException("You must set the Table schema using setTableSchema.");
		}
		return schema;
	}

	public static String getTableInsertStatement(Configuration conf, String columnFamily) {
		String insert = conf.get(((CqlBulkOutputFormat.OUTPUT_CQL_INSERT_PREFIX) + columnFamily));
		if (insert == null) {
			throw new UnsupportedOperationException("You must set the Table insert statement using setTableSchema.");
		}
		return insert;
	}

	public static void setDeleteSourceOnSuccess(Configuration conf, boolean deleteSrc) {
		conf.setBoolean(CqlBulkOutputFormat.DELETE_SOURCE, deleteSrc);
	}

	public static boolean getDeleteSourceOnSuccess(Configuration conf) {
		return conf.getBoolean(CqlBulkOutputFormat.DELETE_SOURCE, false);
	}

	public static void setTableAlias(Configuration conf, String alias, String columnFamily) {
		conf.set(((CqlBulkOutputFormat.TABLE_ALIAS_PREFIX) + alias), columnFamily);
	}

	public static String getTableForAlias(Configuration conf, String alias) {
		return conf.get(((CqlBulkOutputFormat.TABLE_ALIAS_PREFIX) + alias));
	}

	public static void setIgnoreHosts(Configuration conf, String ignoreNodesCsv) {
		conf.set(CqlBulkRecordWriter.IGNORE_HOSTS, ignoreNodesCsv);
	}

	public static void setIgnoreHosts(Configuration conf, String... ignoreNodes) {
		conf.setStrings(CqlBulkRecordWriter.IGNORE_HOSTS, ignoreNodes);
	}

	public static Collection<String> getIgnoreHosts(Configuration conf) {
		return conf.getStringCollection(CqlBulkRecordWriter.IGNORE_HOSTS);
	}

	public static class NullOutputCommitter extends OutputCommitter {
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




import java.io.IOException;
import java.util.Map;
import org.apache.accumulo.core.client.mapreduce.AbstractInputFormat;
import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;


public class AccumuloMultiTableInputFormat extends AbstractInputFormat<Key, Value> {
	public static void setInputTableConfigs(JobConf job, Map<String, InputTableConfig> configs) {
		InputConfigurator.setInputTableConfigs(AbstractInputFormat.CLASS, job, configs);
	}

	public RecordReader<Key, Value> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
		return null;
	}
}


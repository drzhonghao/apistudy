

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.TabletLocator;
import org.apache.accumulo.core.client.mapred.RangeInputSplit;
import org.apache.accumulo.core.client.mapreduce.AbstractInputFormat;
import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.util.Pair;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;


public abstract class InputFormatBase<K, V> extends AbstractInputFormat<K, V> {
	public static void setInputTableName(JobConf job, String tableName) {
		InputConfigurator.setInputTableName(AbstractInputFormat.CLASS, job, tableName);
	}

	protected static String getInputTableName(JobConf job) {
		return InputConfigurator.getInputTableName(AbstractInputFormat.CLASS, job);
	}

	public static void setRanges(JobConf job, Collection<Range> ranges) {
		InputConfigurator.setRanges(AbstractInputFormat.CLASS, job, ranges);
	}

	protected static List<Range> getRanges(JobConf job) throws IOException {
		return InputConfigurator.getRanges(AbstractInputFormat.CLASS, job);
	}

	public static void fetchColumns(JobConf job, Collection<Pair<Text, Text>> columnFamilyColumnQualifierPairs) {
		InputConfigurator.fetchColumns(AbstractInputFormat.CLASS, job, columnFamilyColumnQualifierPairs);
	}

	protected static Set<Pair<Text, Text>> getFetchedColumns(JobConf job) {
		return InputConfigurator.getFetchedColumns(AbstractInputFormat.CLASS, job);
	}

	public static void addIterator(JobConf job, IteratorSetting cfg) {
		InputConfigurator.addIterator(AbstractInputFormat.CLASS, job, cfg);
	}

	protected static List<IteratorSetting> getIterators(JobConf job) {
		return InputConfigurator.getIterators(AbstractInputFormat.CLASS, job);
	}

	public static void setAutoAdjustRanges(JobConf job, boolean enableFeature) {
		InputConfigurator.setAutoAdjustRanges(AbstractInputFormat.CLASS, job, enableFeature);
	}

	protected static boolean getAutoAdjustRanges(JobConf job) {
		return InputConfigurator.getAutoAdjustRanges(AbstractInputFormat.CLASS, job);
	}

	public static void setScanIsolation(JobConf job, boolean enableFeature) {
		InputConfigurator.setScanIsolation(AbstractInputFormat.CLASS, job, enableFeature);
	}

	protected static boolean isIsolated(JobConf job) {
		return InputConfigurator.isIsolated(AbstractInputFormat.CLASS, job);
	}

	public static void setLocalIterators(JobConf job, boolean enableFeature) {
		InputConfigurator.setLocalIterators(AbstractInputFormat.CLASS, job, enableFeature);
	}

	protected static boolean usesLocalIterators(JobConf job) {
		return InputConfigurator.usesLocalIterators(AbstractInputFormat.CLASS, job);
	}

	public static void setOfflineTableScan(JobConf job, boolean enableFeature) {
		InputConfigurator.setOfflineTableScan(AbstractInputFormat.CLASS, job, enableFeature);
	}

	protected static boolean isOfflineScan(JobConf job) {
		return InputConfigurator.isOfflineScan(AbstractInputFormat.CLASS, job);
	}

	public static void setBatchScan(JobConf job, boolean enableFeature) {
		InputConfigurator.setBatchScan(AbstractInputFormat.CLASS, job, enableFeature);
	}

	public static boolean isBatchScan(JobConf job) {
		return InputConfigurator.isBatchScan(AbstractInputFormat.CLASS, job);
	}

	public static void setSamplerConfiguration(JobConf job, SamplerConfiguration samplerConfig) {
		InputConfigurator.setSamplerConfiguration(AbstractInputFormat.CLASS, job, samplerConfig);
	}

	@Deprecated
	protected static TabletLocator getTabletLocator(JobConf job) throws TableNotFoundException {
		return InputConfigurator.getTabletLocator(AbstractInputFormat.CLASS, job, InputConfigurator.getInputTableName(AbstractInputFormat.CLASS, job));
	}

	protected abstract static class RecordReaderBase<K, V> extends AbstractInputFormat.AbstractRecordReader<K, V> {
		protected List<IteratorSetting> jobIterators(JobConf job, String tableName) {
			return InputFormatBase.getIterators(job);
		}

		@Deprecated
		protected void setupIterators(List<IteratorSetting> iterators, Scanner scanner) {
			for (IteratorSetting iterator : iterators) {
				scanner.addScanIterator(iterator);
			}
		}

		@Deprecated
		protected void setupIterators(JobConf job, Scanner scanner) {
			setupIterators(InputFormatBase.getIterators(job), scanner);
		}
	}

	@Deprecated
	public static class RangeInputSplit extends org.apache.accumulo.core.client.mapred.RangeInputSplit {
		public RangeInputSplit() {
			super();
		}

		public RangeInputSplit(InputFormatBase.RangeInputSplit other) throws IOException {
			super(other);
		}

		public RangeInputSplit(String table, String tableId, Range range, String[] locations) {
			super(table, tableId, range, locations);
		}

		protected RangeInputSplit(String table, Range range, String[] locations) {
			super(table, "", range, locations);
		}
	}
}


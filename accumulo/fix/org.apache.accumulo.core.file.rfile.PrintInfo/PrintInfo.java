

import com.beust.jcommander.Parameter;
import com.google.auto.service.AutoService;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.file.blockfile.impl.CachableBlockFile;
import org.apache.accumulo.core.file.rfile.MetricsGatherer;
import org.apache.accumulo.core.file.rfile.VisMetricsGatherer;
import org.apache.accumulo.core.file.rfile.VisibilityMetric;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.util.LocalityGroupUtil;
import org.apache.accumulo.start.spi.KeywordExecutable;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@AutoService(KeywordExecutable.class)
public class PrintInfo implements KeywordExecutable {
	private static final Logger log = LoggerFactory.getLogger(PrintInfo.class);

	static class Opts extends Help {
		@Parameter(names = { "-d", "--dump" }, description = "dump the key/value pairs")
		boolean dump = false;

		@Parameter(names = { "-v", "--vis" }, description = "show visibility metrics")
		boolean vis = false;

		@Parameter(names = { "--visHash" }, description = "show visibilities as hashes, implies -v")
		boolean hash = false;

		@Parameter(names = { "--histogram" }, description = "print a histogram of the key-value sizes")
		boolean histogram = false;

		@Parameter(names = { "--printIndex" }, description = "prints information about all the index entries")
		boolean printIndex = false;

		@Parameter(names = { "--useSample" }, description = "Use sample data for --dump, --vis, --histogram options")
		boolean useSample = false;

		@Parameter(names = { "--keyStats" }, description = "print key length statistics for index and all data")
		boolean keyStats = false;

		@Parameter(description = " <file> { <file> ... }")
		List<String> files = new ArrayList<>();

		@Parameter(names = { "-c", "--config" }, variableArity = true, description = "Comma-separated Hadoop configuration files")
		List<String> configFiles = new ArrayList<>();
	}

	static class LogHistogram {
		long[] countBuckets = new long[11];

		long[] sizeBuckets = new long[countBuckets.length];

		long totalSize = 0;

		public void add(int size) {
			int bucket = ((int) (Math.log10(size)));
			(countBuckets[bucket])++;
			sizeBuckets[bucket] += size;
			totalSize += size;
		}

		public void print(String indent) {
			System.out.println((indent + "Up to size      count      %-age"));
			for (int i = 1; i < (countBuckets.length); i++) {
				System.out.println(String.format("%s%11.0f : %10d %6.2f%%", indent, Math.pow(10, i), countBuckets[i], (((sizeBuckets[i]) * 100.0) / (totalSize))));
			}
		}
	}

	static class KeyStats {
		private SummaryStatistics stats = new SummaryStatistics();

		private PrintInfo.LogHistogram logHistogram = new PrintInfo.LogHistogram();

		public void add(Key k) {
			int size = k.getSize();
			stats.addValue(size);
			logHistogram.add(size);
		}

		public void print(String indent) {
			logHistogram.print(indent);
			System.out.println();
			System.out.printf("%smin:%,11.2f max:%,11.2f avg:%,11.2f stddev:%,11.2f\n", indent, stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation());
		}
	}

	public static void main(String[] args) throws Exception {
		new PrintInfo().execute(args);
	}

	@Override
	public String keyword() {
		return "rfile-info";
	}

	@Override
	public void execute(final String[] args) throws Exception {
		PrintInfo.Opts opts = new PrintInfo.Opts();
		opts.parseArgs(PrintInfo.class.getName(), args);
		if (opts.files.isEmpty()) {
			System.err.println("No files were given");
			System.exit((-1));
		}
		Configuration conf = new Configuration();
		for (String confFile : opts.configFiles) {
			PrintInfo.log.debug(("Adding Hadoop configuration file " + confFile));
			conf.addResource(new Path(confFile));
		}
		FileSystem hadoopFs = FileSystem.get(conf);
		FileSystem localFs = FileSystem.getLocal(conf);
		PrintInfo.LogHistogram kvHistogram = new PrintInfo.LogHistogram();
		PrintInfo.KeyStats dataKeyStats = new PrintInfo.KeyStats();
		PrintInfo.KeyStats indexKeyStats = new PrintInfo.KeyStats();
		for (String arg : opts.files) {
			Path path = new Path(arg);
			FileSystem fs;
			if (arg.contains(":"))
				fs = path.getFileSystem(conf);
			else {
				PrintInfo.log.warn("Attempting to find file across filesystems. Consider providing URI instead of path");
				fs = (hadoopFs.exists(path)) ? hadoopFs : localFs;
			}
			System.out.println(("Reading file: " + (path.makeQualified(fs.getUri(), fs.getWorkingDirectory()).toString())));
			CachableBlockFile.Reader _rdr = new CachableBlockFile.Reader(fs, path, conf, null, null, SiteConfiguration.getInstance());
			MetricsGatherer<Map<String, ArrayList<VisibilityMetric>>> vmg = new VisMetricsGatherer();
			if ((opts.vis) || (opts.hash)) {
			}
			System.out.println();
			org.apache.accumulo.core.file.rfile.bcfile.PrintInfo.main(new String[]{ arg });
			Map<String, ArrayList<ByteSequence>> localityGroupCF = null;
			if (((((opts.histogram) || (opts.dump)) || (opts.vis)) || (opts.hash)) || (opts.keyStats)) {
				FileSKVIterator dataIter;
				if (opts.useSample) {
					dataIter = null;
					if (dataIter == null) {
						System.out.println("ERROR : This rfile has no sample data");
						return;
					}
				}else {
				}
				if (opts.keyStats) {
				}
				for (String lgName : localityGroupCF.keySet()) {
					dataIter = null;
					LocalityGroupUtil.seek(dataIter, new Range(), lgName, localityGroupCF);
					dataIter = null;
					while (dataIter.hasTop()) {
						Key key = dataIter.getTopKey();
						Value value = dataIter.getTopValue();
						if (opts.dump) {
							System.out.println(((key + " -> ") + value));
							if (System.out.checkError())
								return;

						}
						if (opts.histogram) {
							kvHistogram.add(((key.getSize()) + (value.getSize())));
						}
						if (opts.keyStats) {
							dataKeyStats.add(key);
						}
						dataIter.next();
					} 
				}
			}
			if ((opts.vis) || (opts.hash)) {
				System.out.println();
				vmg.printMetrics(opts.hash, "Visibility", System.out);
			}
			if (opts.histogram) {
				System.out.println();
				kvHistogram.print("");
			}
			if (opts.keyStats) {
				System.out.println();
				System.out.println("Statistics for keys in data :");
				dataKeyStats.print("\t");
				System.out.println();
				System.out.println("Statistics for keys in index :");
				indexKeyStats.print("\t");
			}
			if (System.out.checkError())
				return;

		}
	}
}


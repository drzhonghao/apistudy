

import com.beust.jcommander.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.file.blockfile.impl.CachableBlockFile;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;


public class SplitLarge {
	static class Opts extends Help {
		@Parameter(names = "-m", description = "the maximum size of the key/value pair to shunt to the small file")
		long maxSize = (10 * 1024) * 1024;

		@Parameter(description = "<file.rf> { <file.rf> ... }")
		List<String> files = new ArrayList<>();
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = CachedConfiguration.getInstance();
		FileSystem fs = FileSystem.get(conf);
		SplitLarge.Opts opts = new SplitLarge.Opts();
		opts.parseArgs(SplitLarge.class.getName(), args);
		for (String file : opts.files) {
			AccumuloConfiguration aconf = DefaultConfiguration.getDefaultConfiguration();
			Path path = new Path(file);
			CachableBlockFile.Reader rdr = new CachableBlockFile.Reader(fs, path, conf, null, null, aconf);
		}
	}
}


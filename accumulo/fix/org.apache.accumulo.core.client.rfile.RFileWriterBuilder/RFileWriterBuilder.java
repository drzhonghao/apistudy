

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.apache.accumulo.core.client.rfile.RFile;
import org.apache.accumulo.core.client.rfile.RFileWriter;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.file.FileOperations;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import static org.apache.accumulo.core.client.rfile.FSConfArgs.<init>;


class RFileWriterBuilder implements RFile.OutputArguments , RFile.WriterFSOptions {
	private static class OutputArgs {
		private Path path;

		private OutputStream out;

		OutputArgs(String filename) {
			this.path = new Path(filename);
		}

		OutputArgs(OutputStream out) {
			this.out = out;
		}

		OutputStream getOutputStream() {
			return out;
		}
	}

	private RFileWriterBuilder.OutputArgs out;

	private SamplerConfiguration sampler = null;

	private Map<String, String> tableConfig = Collections.emptyMap();

	private int visCacheSize = 1000;

	@Override
	public RFile.WriterOptions withSampler(SamplerConfiguration samplerConf) {
		Objects.requireNonNull(samplerConf);
		SamplerConfigurationImpl.checkDisjoint(tableConfig, samplerConf);
		this.sampler = samplerConf;
		return this;
	}

	@Override
	public RFileWriter build() throws IOException {
		FileOperations fileops = FileOperations.getInstance();
		AccumuloConfiguration acuconf = AccumuloConfiguration.getDefaultConfiguration();
		HashMap<String, String> userProps = new HashMap<>();
		if ((sampler) != null) {
			userProps.putAll(new SamplerConfigurationImpl(sampler).toTablePropertiesMap());
		}
		userProps.putAll(tableConfig);
		if ((userProps.size()) > 0) {
			acuconf = new ConfigurationCopy(Iterables.concat(acuconf, userProps.entrySet()));
		}
		if ((out.getOutputStream()) != null) {
			FSDataOutputStream fsdo;
			if ((out.getOutputStream()) instanceof FSDataOutputStream) {
				fsdo = ((FSDataOutputStream) (out.getOutputStream()));
			}else {
				fsdo = new FSDataOutputStream(out.getOutputStream(), new FileSystem.Statistics("foo"));
			}
		}else {
		}
	}

	@Override
	public RFile.WriterOptions withFileSystem(FileSystem fs) {
		Objects.requireNonNull(fs);
		return this;
	}

	@Override
	public RFile.WriterFSOptions to(String filename) {
		Objects.requireNonNull(filename);
		this.out = new RFileWriterBuilder.OutputArgs(filename);
		return this;
	}

	@Override
	public RFile.WriterOptions to(OutputStream out) {
		Objects.requireNonNull(out);
		this.out = new RFileWriterBuilder.OutputArgs(out);
		return this;
	}

	@Override
	public RFile.WriterOptions withTableProperties(Iterable<Map.Entry<String, String>> tableConfig) {
		Objects.requireNonNull(tableConfig);
		HashMap<String, String> cfg = new HashMap<>();
		for (Map.Entry<String, String> entry : tableConfig) {
			cfg.put(entry.getKey(), entry.getValue());
		}
		SamplerConfigurationImpl.checkDisjoint(cfg, sampler);
		this.tableConfig = cfg;
		return this;
	}

	@Override
	public RFile.WriterOptions withTableProperties(Map<String, String> tableConfig) {
		Objects.requireNonNull(tableConfig);
		return withTableProperties(tableConfig.entrySet());
	}

	@Override
	public RFile.WriterOptions withVisibilityCacheSize(int maxSize) {
		Preconditions.checkArgument((maxSize > 0));
		this.visCacheSize = maxSize;
		return this;
	}
}


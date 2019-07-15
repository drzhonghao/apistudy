

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.rfile.RFile;
import org.apache.accumulo.core.client.rfile.RFileSource;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;


class RFileScannerBuilder implements RFile.InputArguments , RFile.ScannerFSOptions {
	static class InputArgs {
		private Path[] paths;

		private RFileSource[] sources;

		InputArgs(String... files) {
			this.paths = new Path[files.length];
			for (int i = 0; i < (files.length); i++) {
				this.paths[i] = new Path(files[i]);
			}
		}

		InputArgs(RFileSource... sources) {
			this.sources = sources;
		}

		RFileSource[] getSources() throws IOException {
			if ((sources) == null) {
				sources = new RFileSource[paths.length];
				for (int i = 0; i < (paths.length); i++) {
				}
			}else {
				for (int i = 0; i < (sources.length); i++) {
					if (!((sources[i].getInputStream()) instanceof FSDataInputStream)) {
						sources[i] = new RFileSource(new FSDataInputStream(sources[i].getInputStream()), sources[i].getLength());
					}
				}
			}
			return sources;
		}
	}

	@Override
	public RFile.ScannerOptions withoutSystemIterators() {
		return this;
	}

	@Override
	public RFile.ScannerOptions withAuthorizations(Authorizations auths) {
		Objects.requireNonNull(auths);
		return this;
	}

	@Override
	public RFile.ScannerOptions withDataCache(long cacheSize) {
		Preconditions.checkArgument((cacheSize > 0));
		return this;
	}

	@Override
	public RFile.ScannerOptions withIndexCache(long cacheSize) {
		Preconditions.checkArgument((cacheSize > 0));
		return this;
	}

	@Override
	public Scanner build() {
		return null;
	}

	@Override
	public RFile.ScannerOptions withFileSystem(FileSystem fs) {
		Objects.requireNonNull(fs);
		return this;
	}

	@Override
	public RFile.ScannerOptions from(RFileSource... inputs) {
		return this;
	}

	@Override
	public RFile.ScannerFSOptions from(String... files) {
		return this;
	}

	@Override
	public RFile.ScannerOptions withTableProperties(Iterable<Map.Entry<String, String>> tableConfig) {
		Objects.requireNonNull(tableConfig);
		for (Map.Entry<String, String> entry : tableConfig) {
		}
		return this;
	}

	@Override
	public RFile.ScannerOptions withTableProperties(Map<String, String> tableConfig) {
		Objects.requireNonNull(tableConfig);
		return this;
	}

	@Override
	public RFile.ScannerOptions withBounds(Range range) {
		Objects.requireNonNull(range);
		return this;
	}
}


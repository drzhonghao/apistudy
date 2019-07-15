

import java.io.IOException;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.file.rfile.bcfile.BCFile;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;


public class PrintInfo {
	public static void printMetaBlockInfo(Configuration conf, FileSystem fs, Path path) throws IOException {
		FSDataInputStream fsin = fs.open(path);
		BCFile.Reader bcfr = null;
		try {
			bcfr = new BCFile.Reader(fsin, fs.getFileStatus(path).getLen(), conf, SiteConfiguration.getInstance());
		} finally {
			if (bcfr != null) {
				bcfr.close();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		FileSystem hadoopFs = FileSystem.get(conf);
		FileSystem localFs = FileSystem.getLocal(conf);
		Path path = new Path(args[0]);
		FileSystem fs;
		if (args[0].contains(":"))
			fs = path.getFileSystem(conf);
		else
			fs = (hadoopFs.exists(path)) ? hadoopFs : localFs;

		PrintInfo.printMetaBlockInfo(conf, fs, path);
	}
}


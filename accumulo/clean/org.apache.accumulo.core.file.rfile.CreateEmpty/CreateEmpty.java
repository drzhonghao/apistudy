import org.apache.accumulo.core.file.rfile.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.file.FileSKVWriter;
import org.apache.accumulo.core.file.rfile.bcfile.Compression;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Create an empty RFile for use in recovering from data loss where Accumulo still refers internally
 * to a path.
 */
public class CreateEmpty {
  private static final Logger log = LoggerFactory.getLogger(CreateEmpty.class);

  public static class NamedLikeRFile implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
      if (!value.endsWith(".rf")) {
        throw new ParameterException("File must end with .rf and '" + value + "' does not.");
      }
    }
  }

  public static class IsSupportedCompressionAlgorithm implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
      String[] algorithms = Compression.getSupportedAlgorithms();
      if (!((Arrays.asList(algorithms)).contains(value))) {
        throw new ParameterException(
            "Compression codec must be one of " + Arrays.toString(algorithms));
      }
    }
  }

  static class Opts extends Help {
    @Parameter(names = {"-c", "--codec"}, description = "the compression codec to use.",
        validateWith = IsSupportedCompressionAlgorithm.class)
    String codec = Compression.COMPRESSION_NONE;
    @Parameter(
        description = " <path> { <path> ... } Each path given is a URL."
            + " Relative paths are resolved according to the default filesystem defined in"
            + " your Hadoop configuration, which is usually an HDFS instance.",
        required = true, validateWith = NamedLikeRFile.class)
    List<String> files = new ArrayList<>();
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = CachedConfiguration.getInstance();

    Opts opts = new Opts();
    opts.parseArgs(CreateEmpty.class.getName(), args);

    for (String arg : opts.files) {
      Path path = new Path(arg);
      log.info("Writing to file '" + path + "'");
      FileSKVWriter writer = (new RFileOperations()).newWriterBuilder()
          .forFile(arg, path.getFileSystem(conf), conf)
          .withTableConfiguration(DefaultConfiguration.getDefaultConfiguration())
          .withCompression(opts.codec).build();
      writer.close();
    }
  }

}

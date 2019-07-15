import org.apache.lucene.index.*;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

final class TrackingTmpOutputDirectoryWrapper extends FilterDirectory {
  private final Map<String,String> fileNames = new HashMap<>();

  TrackingTmpOutputDirectoryWrapper(Directory in) {
    super(in);
  }

  @Override
  public IndexOutput createOutput(String name, IOContext context) throws IOException {
    IndexOutput output = super.createTempOutput(name, "", context);
    fileNames.put(name, output.getName());
    return output;
  }

  @Override
  public IndexInput openInput(String name, IOContext context) throws IOException {
    String tmpName = fileNames.get(name);
    return super.openInput(tmpName, context);
  }

  public Map<String, String> getTemporaryFiles() {
    return fileNames;
  }
}

import org.apache.lucene.misc.*;


import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.HardlinkCopyDirectoryWrapper;
import org.apache.lucene.util.SuppressForbidden;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Merges indices specified on the command line into the index
 * specified as the first command line argument.
 */
@SuppressForbidden(reason = "System.out required: command line tool")
public class IndexMergeTool {
  
  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("Usage: IndexMergeTool <mergedIndex> <index1> <index2> [index3] ...");
      System.exit(1);
    }

    // Try to use hardlinks to source segments, if possible.
    Directory mergedIndex = new HardlinkCopyDirectoryWrapper(FSDirectory.open(Paths.get(args[0])));

    IndexWriter writer = new IndexWriter(mergedIndex, 
        new IndexWriterConfig(null).setOpenMode(OpenMode.CREATE));

    Directory[] indexes = new Directory[args.length - 1];
    for (int i = 1; i < args.length; i++) {
      indexes[i  - 1] = FSDirectory.open(Paths.get(args[i]));
    }

    System.out.println("Merging...");
    writer.addIndexes(indexes);

    System.out.println("Full merge...");
    writer.forceMerge(1);
    writer.close();
    System.out.println("Done.");
  }
  
}

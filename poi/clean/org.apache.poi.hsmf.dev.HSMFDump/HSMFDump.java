import org.apache.poi.hsmf.dev.*;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.ChunkGroup;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.PropertiesChunk;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.parsers.POIFSChunkParser;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * Dumps out the chunk details, and where possible contents
 */
public class HSMFDump {
   private POIFSFileSystem fs;
   public HSMFDump(POIFSFileSystem fs) {
      this.fs = fs;
   }
   
   public void dump() throws IOException {
      dump(System.out);
   }
   public void dump(PrintStream out) throws IOException {
      ChunkGroup[] chunkGroups = POIFSChunkParser.parse(fs);
      for(ChunkGroup chunks : chunkGroups) {
         out.println(chunks.getClass().getSimpleName());
         for(Chunk chunk : chunks.getChunks()) {
            MAPIProperty attr = MAPIProperty.get(chunk.getChunkId());
            
            if (chunk instanceof PropertiesChunk) {
               PropertiesChunk props = (PropertiesChunk)chunk;
               out.println(
                     "   Properties - " + props.getProperties().size() + ":"
               );
               
               for (MAPIProperty prop : props.getProperties().keySet()) {
                  out.println(
                        "       * " + prop
                  );
                  for (PropertyValue v : props.getValues(prop)) {
                     out.println(
                           "        = " + v
                     );
                  }
               }
            } else {
               String idName = attr.id + " - " + attr.name;
               if(attr == MAPIProperty.UNKNOWN) {
                  idName = chunk.getChunkId() + " - (unknown)";
               }
               
               out.println(
                     "   " + idName + " - " + chunk.getType().getName()
               );
               out.println(
                     "       " + chunk
               );
            }
         }
         out.println();
      }
   }
   
   public static void main(String[] args) throws Exception {
      for(String file : args) {
         POIFSFileSystem fs = new POIFSFileSystem(new File(file), true);
         HSMFDump dump = new HSMFDump(fs);
         dump.dump();
         fs.close();
      }
   }
}

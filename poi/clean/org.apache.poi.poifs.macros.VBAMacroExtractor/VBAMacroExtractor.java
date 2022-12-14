import org.apache.poi.poifs.macros.VBAMacroReader;
import org.apache.poi.poifs.macros.*;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.util.StringUtil;

/**
 * This tool extracts out the source of all VBA Modules of an office file,
 *  both OOXML (eg XLSM) and OLE2/POIFS (eg DOC), to STDOUT or a directory.
 * 
 * @since 3.15-beta2
 */
public class VBAMacroExtractor {
    public static void main(String args[]) throws IOException {
        if (args.length == 0) {
            System.err.println("Use:");
            System.err.println("   VBAMacroExtractor <office.doc> [output]");
            System.err.println("");
            System.err.println("If an output directory is given, macros are written there");
            System.err.println("Otherwise they are output to the screen");
            System.exit(1);
        }
        
        File input = new File(args[0]);
        File output = null;
        if (args.length > 1) {
            output = new File(args[1]);
        }
        
        VBAMacroExtractor extractor = new VBAMacroExtractor();
        extractor.extract(input, output);
    }
    
    /**
      * Extracts the VBA modules from a macro-enabled office file and writes them
      * to files in <tt>outputDir</tt>.
      *
      * Creates the <tt>outputDir</tt>, directory, including any necessary but
      * nonexistent parent directories, if <tt>outputDir</tt> does not exist.
      * If <tt>outputDir</tt> is null, writes the contents to standard out instead.
      *
      * @param input  the macro-enabled office file.
      * @param outputDir  the directory to write the extracted VBA modules to.
      * @param extension  file extension of the extracted VBA modules
      * @since 3.15-beta2
      */
    public void extract(File input, File outputDir, String extension) throws IOException {
        if (! input.exists()) throw new FileNotFoundException(input.toString());
        System.err.print("Extracting VBA Macros from " + input + " to ");
        if (outputDir != null) {
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw new IOException("Output directory " + outputDir + " could not be created");
            }
            System.err.println(outputDir);
        } else {
            System.err.println("STDOUT");
        }
        
        VBAMacroReader reader = new VBAMacroReader(input);
        Map<String,String> macros = reader.readMacros();
        reader.close();
        
        final String divider = "---------------------------------------";
        for (Entry<String, String> entry : macros.entrySet()) {
            String moduleName = entry.getKey();
            String moduleCode = entry.getValue();
            if (outputDir == null) {
                System.out.println(divider);
                System.out.println(moduleName);
                System.out.println("");
                System.out.println(moduleCode);
            } else {
                File out = new File(outputDir, moduleName + extension);
                FileOutputStream fout = new FileOutputStream(out);
                OutputStreamWriter fwriter = new OutputStreamWriter(fout, StringUtil.UTF8);
                fwriter.write(moduleCode);
                fwriter.close();
                fout.close();
                System.out.println("Extracted " + out);
            }
        }
        if (outputDir == null) {
            System.out.println(divider);
        }
    }

    /**
      * Extracts the VBA modules from a macro-enabled office file and writes them
      * to <tt>.vba</tt> files in <tt>outputDir</tt>. 
      * 
      * Creates the <tt>outputDir</tt>, directory, including any necessary but
      * nonexistent parent directories, if <tt>outputDir</tt> does not exist.
      * If <tt>outputDir</tt> is null, writes the contents to standard out instead.
      * 
      * @param input  the macro-enabled office file.
      * @param outputDir  the directory to write the extracted VBA modules to.
      * @since 3.15-beta2
      */
    public void extract(File input, File outputDir) throws IOException {
        extract(input, outputDir, ".vba");
    }
}

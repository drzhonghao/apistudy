import org.apache.poi.ooxml.extractor.*;


import java.io.File;

import org.apache.poi.extractor.POITextExtractor;

/**
 * A command line wrapper around {@link ExtractorFactory}, useful
 * for when debugging.
 */
public class CommandLineTextExtractor {
    public static final String DIVIDER = "=======================";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Use:");
            System.err.println("   CommandLineTextExtractor <filename> [filename] [filename]");
            System.exit(1);
        }

        for (String arg : args) {
            System.out.println(DIVIDER);

            File f = new File(arg);
            System.out.println(f);

            try (POITextExtractor extractor = ExtractorFactory.createExtractor(f)) {
                POITextExtractor metadataExtractor =
                        extractor.getMetadataTextExtractor();

                System.out.println("   " + DIVIDER);
                String metaData = metadataExtractor.getText();
                System.out.println(metaData);
                System.out.println("   " + DIVIDER);
                String text = extractor.getText();
                System.out.println(text);
                System.out.println(DIVIDER);
                System.out.println("Had " + metaData.length() + " characters of metadata and " + text.length() + " characters of text");
            }
        }
    }
}

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.*;


import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Section;

public final class QuickTest {
    public QuickTest() {
    }

    public static void main(String[] args) throws IOException {
        HWPFDocument doc = new HWPFDocument(new FileInputStream(args[0]));
        Range r = doc.getRange();

        System.out.println("Example you supplied:");
        System.out.println("---------------------");
        for (int x = 0; x < r.numSections(); x++) {
            Section s = r.getSection(x);
            for (int y = 0; y < s.numParagraphs(); y++) {
                Paragraph p = s.getParagraph(y);
                for (int z = 0; z < p.numCharacterRuns(); z++) {
                    // character run
                    CharacterRun run = p.getCharacterRun(z);
                    // character run text
                    String text = run.text();
                    // show us the text
                    System.out.print(text);
                }
                // use a new line at the paragraph break
                System.out.println();
            }
        }
        doc.close();
    }
}

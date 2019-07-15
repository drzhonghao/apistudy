import org.apache.poi.hslf.dev.*;


import java.io.IOException;
import java.util.List;

import org.apache.poi.hslf.model.textproperties.BitMaskTextProp;
import org.apache.poi.hslf.model.textproperties.TextProp;
import org.apache.poi.hslf.model.textproperties.TextPropCollection;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.SlideListWithText;
import org.apache.poi.hslf.record.StyleTextPropAtom;
import org.apache.poi.hslf.record.TextBytesAtom;
import org.apache.poi.hslf.record.TextCharsAtom;
import org.apache.poi.hslf.usermodel.HSLFSlideShowImpl;

/**
 * Uses record level code to locate StyleTextPropAtom entries.
 * Having found them, it shows the contents
 */
public final class TextStyleListing {
	public static void main(String[] args) throws IOException {
		if(args.length < 1) {
			System.err.println("Need to give a filename");
			System.exit(1);
		}

		HSLFSlideShowImpl ss = new HSLFSlideShowImpl(args[0]);

		// Find the documents, and then their SLWT
		Record[] records = ss.getRecords();
		for(int i=0; i<records.length; i++) {
			if(records[i].getRecordType() == 1000l) {
				Record docRecord = records[i];
				Record[] docChildren = docRecord.getChildRecords();
				for(int j=0; j<docChildren.length; j++) {
					if(docChildren[j] instanceof SlideListWithText) {
						Record[] slwtChildren = docChildren[j].getChildRecords();

						int lastTextLen = -1;
						for(int k=0; k<slwtChildren.length; k++) {
							if(slwtChildren[k] instanceof TextCharsAtom) {
								lastTextLen = ((TextCharsAtom)slwtChildren[k]).getText().length();
							}
							if(slwtChildren[k] instanceof TextBytesAtom) {
								lastTextLen = ((TextBytesAtom)slwtChildren[k]).getText().length();
							}

							if(slwtChildren[k] instanceof StyleTextPropAtom) {
								StyleTextPropAtom stpa = (StyleTextPropAtom)slwtChildren[k];
								stpa.setParentTextSize(lastTextLen);
								showStyleTextPropAtom(stpa);
							}
						}
					}
				}
			}
		}
		
		ss.close();
	}

	public static void showStyleTextPropAtom(StyleTextPropAtom stpa) {
		System.out.println("\nFound a StyleTextPropAtom");

		List<TextPropCollection> paragraphStyles = stpa.getParagraphStyles();
		System.out.println("Contains " + paragraphStyles.size() + " paragraph styles:");
		for(int i=0; i<paragraphStyles.size(); i++) {
			TextPropCollection tpc = paragraphStyles.get(i);
			System.out.println(" In paragraph styling " + i + ":");
			System.out.println("  Characters covered is " + tpc.getCharactersCovered());
			showTextProps(tpc);
		}

		List<TextPropCollection> charStyles = stpa.getCharacterStyles();
		System.out.println("Contains " + charStyles.size() + " character styles:");
		for(int i=0; i<charStyles.size(); i++) {
			TextPropCollection tpc = charStyles.get(i);
			System.out.println("  In character styling " + i + ":");
			System.out.println("    Characters covered is " + tpc.getCharactersCovered());
			showTextProps(tpc);
		}
	}

	public static void showTextProps(TextPropCollection tpc) {
		List<TextProp> textProps = tpc.getTextPropList();
		System.out.println("    Contains " + textProps.size() + " TextProps");
		for(int i=0; i<textProps.size(); i++) {
			TextProp tp = textProps.get(i);
			System.out.println("      " + i + " - " + tp.getName());
			System.out.println("          = " + tp.getValue());
			System.out.println("          @ " + tp.getMask());

			if(tp instanceof BitMaskTextProp) {
				BitMaskTextProp bmtp = (BitMaskTextProp)tp;
				String[] subPropNames = bmtp.getSubPropNames();
				boolean[] subPropMatches = bmtp.getSubPropMatches();
				for(int j=0; j<subPropNames.length; j++) {
					System.out.println("            -> " + j + " - " + subPropNames[j]);
					System.out.println("               " + j + " = " + subPropMatches[j]);
				}
			}
		}
	}
}

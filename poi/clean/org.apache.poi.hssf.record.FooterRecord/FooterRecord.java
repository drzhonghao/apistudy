import org.apache.poi.hssf.record.HeaderFooterBase;
import org.apache.poi.hssf.record.*;


/**
 * Title:        Footer Record (0x0015)<p>
 * Description:  Specifies the footer for a sheet<p>
 * REFERENCE:  PG 317 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class FooterRecord extends HeaderFooterBase implements Cloneable {
	public final static short sid = 0x0015;

	public FooterRecord(String text) {
		super(text);
	}

	public FooterRecord(RecordInputStream in) {
		super(in);
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[FOOTER]\n");
		buffer.append("    .footer = ").append(getText()).append("\n");
		buffer.append("[/FOOTER]\n");
		return buffer.toString();
	}

	public short getSid() {
		return sid;
	}

	@Override
	public FooterRecord clone() {
		return new FooterRecord(getText());
	}
}

import org.apache.poi.hssf.record.common.*;


import org.apache.poi.hssf.record.FeatRecord;
import org.apache.poi.hssf.record.PasswordRecord;
import org.apache.poi.hssf.record.PasswordRev4Record;
//import org.apache.poi.hssf.record.Feat11Record;
//import org.apache.poi.hssf.record.Feat12Record;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.StringUtil;

/**
 * Title: FeatProtection (Protection Shared Feature) common record part
 * <P>
 * This record part specifies Protection data for a sheet, stored
 *  as part of a Shared Feature. It can be found in records such
 *  as {@link FeatRecord}
 */
public final class FeatProtection implements SharedFeature {
	@SuppressWarnings("RedundantFieldInitialization")
	public static final long NO_SELF_RELATIVE_SECURITY_FEATURE = 0;
	public static final long HAS_SELF_RELATIVE_SECURITY_FEATURE = 1;

	private int fSD;
	
	/**
	 * 0 means no password. Otherwise indicates the
	 *  password verifier algorithm (same kind as 
	 *   {@link PasswordRecord} and
	 *   {@link PasswordRev4Record})
	 */
	private int passwordVerifier;
	
	private String title;
	private byte[] securityDescriptor;
	
	public FeatProtection() {
		securityDescriptor = new byte[0];
	}

	public FeatProtection(RecordInputStream in) {
		fSD = in.readInt();
		passwordVerifier = in.readInt();
		
		title = StringUtil.readUnicodeString(in);
		
		securityDescriptor = in.readRemainder();
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(" [FEATURE PROTECTION]\n");
		buffer.append("   Self Relative = " + fSD); 
		buffer.append("   Password Verifier = " + passwordVerifier);
		buffer.append("   Title = " + title);
		buffer.append("   Security Descriptor Size = " + securityDescriptor.length);
		buffer.append(" [/FEATURE PROTECTION]\n");
		return buffer.toString();
	}

	public void serialize(LittleEndianOutput out) {
		out.writeInt(fSD);
		out.writeInt(passwordVerifier);
		StringUtil.writeUnicodeString(out, title);
		out.write(securityDescriptor);
	}

	public int getDataSize() {
		return 4 + 4 + StringUtil.getEncodedSize(title) + securityDescriptor.length;
	}

	public int getPasswordVerifier() {
		return passwordVerifier;
	}
	public void setPasswordVerifier(int passwordVerifier) {
		this.passwordVerifier = passwordVerifier;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public int getFSD() {
		return fSD;
	}
}

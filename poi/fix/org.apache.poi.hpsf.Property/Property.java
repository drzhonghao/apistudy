

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.DatatypeConverter;
import org.apache.poi.hpsf.Filetime;
import org.apache.poi.hpsf.UnsupportedVariantTypeException;
import org.apache.poi.hpsf.Variant;
import org.apache.poi.hpsf.VariantSupport;
import org.apache.poi.hpsf.VariantTypeException;
import org.apache.poi.hpsf.WritingNotSupportedException;
import org.apache.poi.hpsf.wellknown.PropertyIDMap;
import org.apache.poi.util.CodePageUtil;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public class Property {
	public static final int DEFAULT_CODEPAGE = CodePageUtil.CP_WINDOWS_1252;

	private static final POILogger LOG = POILogFactory.getLogger(Property.class);

	private long id;

	private long type;

	private Object value;

	public Property() {
	}

	public Property(Property p) {
		this(p.id, p.type, p.value);
	}

	public Property(final long id, final long type, final Object value) {
		this.id = id;
		this.type = type;
		this.value = value;
	}

	public Property(final long id, final byte[] src, final long offset, final int length, final int codepage) throws UnsupportedEncodingException {
		this.id = id;
		if (id == 0) {
			throw new UnsupportedEncodingException("Dictionary not allowed here");
		}
		int o = ((int) (offset));
		type = LittleEndian.getUInt(src, o);
		o += LittleEndianConsts.INT_SIZE;
		try {
			value = VariantSupport.read(src, o, length, ((int) (type)), codepage);
		} catch (UnsupportedVariantTypeException ex) {
			value = ex.getValue();
		}
	}

	public Property(final long id, LittleEndianByteArrayInputStream leis, final int length, final int codepage) throws UnsupportedEncodingException {
		this.id = id;
		if (id == 0) {
			throw new UnsupportedEncodingException("Dictionary not allowed here");
		}
		type = leis.readUInt();
		try {
			value = VariantSupport.read(leis, length, ((int) (type)), codepage);
		} catch (UnsupportedVariantTypeException ex) {
			value = ex.getValue();
		}
	}

	public long getID() {
		return id;
	}

	public void setID(final long id) {
		this.id = id;
	}

	public long getType() {
		return type;
	}

	public void setType(final long type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	protected int getSize(int property) throws WritingNotSupportedException {
		int length = Variant.getVariantLength(type);
		if ((length >= 0) || ((type) == (Variant.VT_EMPTY))) {
			return length;
		}
		if (length == (-2)) {
			throw new WritingNotSupportedException(type, null);
		}
		if (((type) == (Variant.VT_LPSTR)) || ((type) == (Variant.VT_LPWSTR))) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				length = (write(bos, property)) - (2 * (LittleEndianConsts.INT_SIZE));
				length += (4 - (length & 3)) & 3;
				return length;
			} catch (IOException e) {
				throw new WritingNotSupportedException(type, this.value);
			}
		}
		throw new WritingNotSupportedException(type, this.value);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Property)) {
			return false;
		}
		final Property p = ((Property) (o));
		final Object pValue = p.getValue();
		final long pId = p.getID();
		if (((id) != pId) || (((id) != 0) && (!(typesAreEqual(type, p.getType()))))) {
			return false;
		}
		if (((value) == null) && (pValue == null)) {
			return true;
		}
		if (((value) == null) || (pValue == null)) {
			return false;
		}
		final Class<?> valueClass = value.getClass();
		final Class<?> pValueClass = pValue.getClass();
		if ((!(valueClass.isAssignableFrom(pValueClass))) && (!(pValueClass.isAssignableFrom(valueClass)))) {
			return false;
		}
		if ((value) instanceof byte[]) {
			byte[] thisVal = ((byte[]) (value));
			byte[] otherVal = ((byte[]) (pValue));
			int len = Property.unpaddedLength(thisVal);
			if (len != (Property.unpaddedLength(otherVal))) {
				return false;
			}
			for (int i = 0; i < len; i++) {
				if ((thisVal[i]) != (otherVal[i])) {
					return false;
				}
			}
			return true;
		}
		return value.equals(pValue);
	}

	private static int unpaddedLength(byte[] buf) {
		final int end = (buf.length) - (((buf.length) + 3) % 4);
		for (int i = buf.length; i > end; i--) {
			if ((buf[(i - 1)]) != 0) {
				return i;
			}
		}
		return end;
	}

	private boolean typesAreEqual(final long t1, final long t2) {
		return ((t1 == t2) || ((t1 == (Variant.VT_LPSTR)) && (t2 == (Variant.VT_LPWSTR)))) || ((t2 == (Variant.VT_LPSTR)) && (t1 == (Variant.VT_LPWSTR)));
	}

	@Override
	public int hashCode() {
		long hashCode = 0;
		hashCode += id;
		hashCode += type;
		if ((value) != null) {
			hashCode += value.hashCode();
		}
		return ((int) (hashCode & 4294967295L));
	}

	@Override
	public String toString() {
		return toString(Property.DEFAULT_CODEPAGE, null);
	}

	public String toString(int codepage, PropertyIDMap idMap) {
		final StringBuilder b = new StringBuilder();
		b.append("Property[");
		b.append("id: ");
		b.append(id);
		String idName = (idMap == null) ? null : idMap.get(id);
		if (idName == null) {
			idName = PropertyIDMap.getFallbackProperties().get(id);
		}
		if (idName != null) {
			b.append(" (");
			b.append(idName);
			b.append(")");
		}
		b.append(", type: ");
		b.append(getType());
		b.append(" (");
		b.append(getVariantName());
		b.append(") ");
		final Object value = getValue();
		b.append(", value: ");
		if (value instanceof String) {
			b.append(((String) (value)));
			b.append("\n");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				write(bos, codepage);
			} catch (Exception e) {
				Property.LOG.log(POILogger.WARN, "can't serialize string", e);
			}
			if ((bos.size()) > (2 * (LittleEndianConsts.INT_SIZE))) {
				final String hex = HexDump.dump(bos.toByteArray(), ((-2) * (LittleEndianConsts.INT_SIZE)), (2 * (LittleEndianConsts.INT_SIZE)));
				b.append(hex);
			}
		}else
			if (value instanceof byte[]) {
				b.append("\n");
				byte[] bytes = ((byte[]) (value));
				if ((bytes.length) > 0) {
					String hex = HexDump.dump(bytes, 0L, 0);
					b.append(hex);
				}
			}else
				if (value instanceof Date) {
					Date d = ((Date) (value));
					long filetime = Filetime.dateToFileTime(d);
					if (Filetime.isUndefined(d)) {
						b.append("<undefined>");
					}else
						if ((filetime >>> 32) == 0) {
							long l = filetime * 100;
							TimeUnit tu = TimeUnit.NANOSECONDS;
							final long hr = tu.toHours(l);
							l -= TimeUnit.HOURS.toNanos(hr);
							final long min = tu.toMinutes(l);
							l -= TimeUnit.MINUTES.toNanos(min);
							final long sec = tu.toSeconds(l);
							l -= TimeUnit.SECONDS.toNanos(sec);
							final long ms = tu.toMillis(l);
							String str = String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hr, min, sec, ms);
							b.append(str);
						}else {
							Calendar cal = Calendar.getInstance(LocaleUtil.TIMEZONE_UTC, Locale.ROOT);
							cal.setTime(d);
							b.append(DatatypeConverter.printDateTime(cal));
						}

				}else
					if ((((type) == (Variant.VT_EMPTY)) || ((type) == (Variant.VT_NULL))) || (value == null)) {
						b.append("null");
					}else {
						b.append(value);
						String decoded = decodeValueFromID();
						if (decoded != null) {
							b.append(" (");
							b.append(decoded);
							b.append(")");
						}
					}



		b.append(']');
		return b.toString();
	}

	private String getVariantName() {
		if ((getID()) == 0) {
			return "dictionary";
		}
		return Variant.getVariantName(getType());
	}

	private String decodeValueFromID() {
		try {
			switch (((int) (getID()))) {
				case PropertyIDMap.PID_CODEPAGE :
					return CodePageUtil.codepageToEncoding(((Number) (value)).intValue());
				case PropertyIDMap.PID_LOCALE :
					return LocaleUtil.getLocaleFromLCID(((Number) (value)).intValue());
			}
		} catch (Exception e) {
			Property.LOG.log(POILogger.WARN, ("Can't decode id " + (getID())));
		}
		return null;
	}

	public int write(final OutputStream out, final int codepage) throws IOException, WritingNotSupportedException {
		int length = 0;
		long variantType = getType();
		if ((variantType == (Variant.VT_LPSTR)) && (codepage != (CodePageUtil.CP_UTF16))) {
			String csStr = CodePageUtil.codepageToEncoding((codepage > 0 ? codepage : Property.DEFAULT_CODEPAGE));
			if (!(Charset.forName(csStr).newEncoder().canEncode(((String) (value))))) {
				variantType = Variant.VT_LPWSTR;
			}
		}
		LittleEndian.putUInt(variantType, out);
		length += LittleEndianConsts.INT_SIZE;
		length += VariantSupport.write(out, variantType, getValue(), codepage);
		return length;
	}
}


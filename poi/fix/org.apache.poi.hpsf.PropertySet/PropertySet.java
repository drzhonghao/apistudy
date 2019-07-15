

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.poi.EmptyFileException;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.HPSFRuntimeException;
import org.apache.poi.hpsf.MissingSectionException;
import org.apache.poi.hpsf.NoFormatIDException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.NoSingleSectionException;
import org.apache.poi.hpsf.Property;
import org.apache.poi.hpsf.Section;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hpsf.WritingNotSupportedException;
import org.apache.poi.hpsf.wellknown.PropertyIDMap;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.util.CodePageUtil;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.LittleEndianOutputStream;


public class PropertySet {
	public static final int OS_WIN16 = 0;

	public static final int OS_MACINTOSH = 1;

	public static final int OS_WIN32 = 2;

	static final int BYTE_ORDER_ASSERTION = 65534;

	static final int FORMAT_ASSERTION = 0;

	static final int OFFSET_HEADER = ((((LittleEndianConsts.SHORT_SIZE) + (LittleEndianConsts.SHORT_SIZE)) + (LittleEndianConsts.INT_SIZE)) + (ClassID.LENGTH)) + (LittleEndianConsts.INT_SIZE);

	private int byteOrder;

	private int format;

	private int osVersion;

	private ClassID classID;

	private final List<Section> sections = new ArrayList<>();

	public PropertySet() {
		byteOrder = PropertySet.BYTE_ORDER_ASSERTION;
		format = PropertySet.FORMAT_ASSERTION;
		osVersion = ((PropertySet.OS_WIN32) << 16) | 2564;
		classID = new ClassID();
		addSection(new Section());
	}

	public PropertySet(final InputStream stream) throws IOException, NoPropertySetStreamException {
		if (!(PropertySet.isPropertySetStream(stream))) {
			throw new NoPropertySetStreamException();
		}
		final byte[] buffer = IOUtils.toByteArray(stream);
		init(buffer, 0, buffer.length);
	}

	public PropertySet(final byte[] stream, final int offset, final int length) throws UnsupportedEncodingException, NoPropertySetStreamException {
		if (!(PropertySet.isPropertySetStream(stream, offset, length))) {
			throw new NoPropertySetStreamException();
		}
		init(stream, offset, length);
	}

	public PropertySet(final byte[] stream) throws UnsupportedEncodingException, NoPropertySetStreamException {
		this(stream, 0, stream.length);
	}

	public PropertySet(PropertySet ps) {
		setByteOrder(ps.getByteOrder());
		setFormat(ps.getFormat());
		setOSVersion(ps.getOSVersion());
		setClassID(ps.getClassID());
		for (final Section section : ps.getSections()) {
			sections.add(new Section(section));
		}
	}

	public int getByteOrder() {
		return byteOrder;
	}

	@SuppressWarnings("WeakerAccess")
	public void setByteOrder(int byteOrder) {
		this.byteOrder = byteOrder;
	}

	public int getFormat() {
		return format;
	}

	public void setFormat(int format) {
		this.format = format;
	}

	public int getOSVersion() {
		return osVersion;
	}

	@SuppressWarnings("WeakerAccess")
	public void setOSVersion(int osVersion) {
		this.osVersion = osVersion;
	}

	public ClassID getClassID() {
		return classID;
	}

	@SuppressWarnings("WeakerAccess")
	public void setClassID(ClassID classID) {
		this.classID = classID;
	}

	public int getSectionCount() {
		return sections.size();
	}

	public List<Section> getSections() {
		return Collections.unmodifiableList(sections);
	}

	public void addSection(final Section section) {
		sections.add(section);
	}

	public void clearSections() {
		sections.clear();
	}

	public PropertyIDMap getPropertySetIDMap() {
		return null;
	}

	public static boolean isPropertySetStream(final InputStream stream) throws IOException {
		final int BUFFER_SIZE = 50;
		try {
			final byte[] buffer = IOUtils.peekFirstNBytes(stream, BUFFER_SIZE);
			return PropertySet.isPropertySetStream(buffer, 0, buffer.length);
		} catch (EmptyFileException e) {
			return false;
		}
	}

	@SuppressWarnings({ "unused", "WeakerAccess" })
	public static boolean isPropertySetStream(final byte[] src, final int offset, final int length) {
		LittleEndianByteArrayInputStream leis = new LittleEndianByteArrayInputStream(src, offset, length);
		try {
			final int byteOrder = leis.readUShort();
			if (byteOrder != (PropertySet.BYTE_ORDER_ASSERTION)) {
				return false;
			}
			final int format = leis.readUShort();
			if (format != (PropertySet.FORMAT_ASSERTION)) {
				return false;
			}
			final long osVersion = leis.readUInt();
			byte[] clsBuf = new byte[ClassID.LENGTH];
			leis.readFully(clsBuf);
			final ClassID classID = new ClassID(clsBuf, 0);
			final long sectionCount = leis.readUInt();
			return sectionCount >= 0;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private void init(final byte[] src, final int offset, final int length) throws UnsupportedEncodingException {
		int o = offset;
		byteOrder = LittleEndian.getUShort(src, o);
		o += LittleEndianConsts.SHORT_SIZE;
		format = LittleEndian.getUShort(src, o);
		o += LittleEndianConsts.SHORT_SIZE;
		osVersion = ((int) (LittleEndian.getUInt(src, o)));
		o += LittleEndianConsts.INT_SIZE;
		classID = new ClassID(src, o);
		o += ClassID.LENGTH;
		final int sectionCount = LittleEndian.getInt(src, o);
		o += LittleEndianConsts.INT_SIZE;
		if (sectionCount < 0) {
			throw new HPSFRuntimeException((("Section count " + sectionCount) + " is negative."));
		}
		for (int i = 0; i < sectionCount; i++) {
			final Section s = new Section(src, o);
			o += (ClassID.LENGTH) + (LittleEndianConsts.INT_SIZE);
			sections.add(s);
		}
	}

	public void write(final OutputStream out) throws IOException, WritingNotSupportedException {
		out.write(toBytes());
		out.close();
	}

	private byte[] toBytes() throws IOException, WritingNotSupportedException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LittleEndianOutputStream leos = new LittleEndianOutputStream(bos);
		final int nrSections = getSectionCount();
		leos.writeShort(getByteOrder());
		leos.writeShort(getFormat());
		leos.writeInt(getOSVersion());
		PropertySet.putClassId(bos, getClassID());
		leos.writeInt(nrSections);
		assert (bos.size()) == (PropertySet.OFFSET_HEADER);
		final int[][] offsets = new int[getSectionCount()][2];
		int secCnt = 0;
		for (final Section section : getSections()) {
			final ClassID formatID = section.getFormatID();
			if (formatID == null) {
				throw new NoFormatIDException();
			}
			PropertySet.putClassId(bos, formatID);
			offsets[(secCnt++)][0] = bos.size();
			leos.writeInt((-1));
		}
		secCnt = 0;
		for (final Section section : getSections()) {
			offsets[(secCnt++)][1] = bos.size();
			section.write(bos);
		}
		byte[] result = bos.toByteArray();
		for (int[] off : offsets) {
			LittleEndian.putInt(result, off[0], off[1]);
		}
		return result;
	}

	public void write(final DirectoryEntry dir, final String name) throws IOException, WritingNotSupportedException {
		if (dir.hasEntry(name)) {
			final Entry e = dir.getEntry(name);
			e.delete();
		}
		dir.createDocument(name, toInputStream());
	}

	public InputStream toInputStream() throws IOException, WritingNotSupportedException {
		return new ByteArrayInputStream(toBytes());
	}

	String getPropertyStringValue(final int propertyId) {
		Object propertyValue = getProperty(propertyId);
		return PropertySet.getPropertyStringValue(propertyValue);
	}

	public static String getPropertyStringValue(final Object propertyValue) {
		if (propertyValue == null) {
			return null;
		}
		if (propertyValue instanceof String) {
			return ((String) (propertyValue));
		}
		if (propertyValue instanceof byte[]) {
			byte[] b = ((byte[]) (propertyValue));
			switch (b.length) {
				case 0 :
					return "";
				case 1 :
					return Byte.toString(b[0]);
				case 2 :
					return Integer.toString(LittleEndian.getUShort(b));
				case 4 :
					return Long.toString(LittleEndian.getUInt(b));
				default :
					try {
						return CodePageUtil.getStringFromCodePage(b, Property.DEFAULT_CODEPAGE);
					} catch (UnsupportedEncodingException e) {
						return "";
					}
			}
		}
		return propertyValue.toString();
	}

	public boolean isSummaryInformation() {
		return (!(sections.isEmpty())) && (PropertySet.matchesSummary(getFirstSection().getFormatID(), SummaryInformation.FORMAT_ID));
	}

	public boolean isDocumentSummaryInformation() {
		return (!(sections.isEmpty())) && (PropertySet.matchesSummary(getFirstSection().getFormatID(), DocumentSummaryInformation.FORMAT_ID));
	}

	static boolean matchesSummary(ClassID actual, ClassID... expected) {
		for (ClassID sum : expected) {
			if ((sum.equals(actual)) || (sum.equalsInverted(actual))) {
				return true;
			}
		}
		return false;
	}

	public Property[] getProperties() throws NoSingleSectionException {
		return getFirstSection().getProperties();
	}

	protected Object getProperty(final int id) throws NoSingleSectionException {
		return getFirstSection().getProperty(id);
	}

	boolean getPropertyBooleanValue(final int id) throws NoSingleSectionException {
		return false;
	}

	int getPropertyIntValue(final int id) throws NoSingleSectionException {
		return 0;
	}

	public boolean wasNull() throws NoSingleSectionException {
		return getFirstSection().wasNull();
	}

	@SuppressWarnings("WeakerAccess")
	public Section getFirstSection() {
		if (sections.isEmpty()) {
			throw new MissingSectionException("Property set does not contain any sections.");
		}
		return sections.get(0);
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "5.0.0")
	public Section getSingleSection() {
		final int sectionCount = getSectionCount();
		if (sectionCount != 1) {
			throw new NoSingleSectionException((("Property set contains " + sectionCount) + " sections."));
		}
		return sections.get(0);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof PropertySet)) {
			return false;
		}
		final PropertySet ps = ((PropertySet) (o));
		int byteOrder1 = ps.getByteOrder();
		int byteOrder2 = getByteOrder();
		ClassID classID1 = ps.getClassID();
		ClassID classID2 = getClassID();
		int format1 = ps.getFormat();
		int format2 = getFormat();
		int osVersion1 = ps.getOSVersion();
		int osVersion2 = getOSVersion();
		int sectionCount1 = ps.getSectionCount();
		int sectionCount2 = getSectionCount();
		if (((((byteOrder1 != byteOrder2) || (!(classID1.equals(classID2)))) || (format1 != format2)) || (osVersion1 != osVersion2)) || (sectionCount1 != sectionCount2)) {
			return false;
		}
		return getSections().containsAll(ps.getSections());
	}

	@org.apache.poi.util.NotImplemented
	@Override
	public int hashCode() {
		throw new UnsupportedOperationException("FIXME: Not yet implemented.");
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		final int sectionCount = getSectionCount();
		b.append(getClass().getName());
		b.append('[');
		b.append("byteOrder: ");
		b.append(getByteOrder());
		b.append(", classID: ");
		b.append(getClassID());
		b.append(", format: ");
		b.append(getFormat());
		b.append(", OSVersion: ");
		b.append(getOSVersion());
		b.append(", sectionCount: ");
		b.append(sectionCount);
		b.append(", sections: [\n");
		for (Section section : getSections()) {
			b.append(section.toString(getPropertySetIDMap()));
		}
		b.append(']');
		b.append(']');
		return b.toString();
	}

	void remove1stProperty(long id) {
		getFirstSection().removeProperty(id);
	}

	void set1stProperty(long id, String value) {
		getFirstSection().setProperty(((int) (id)), value);
	}

	void set1stProperty(long id, int value) {
		getFirstSection().setProperty(((int) (id)), value);
	}

	void set1stProperty(long id, boolean value) {
		getFirstSection().setProperty(((int) (id)), value);
	}

	@SuppressWarnings("SameParameterValue")
	void set1stProperty(long id, byte[] value) {
		getFirstSection().setProperty(((int) (id)), value);
	}

	private static void putClassId(final ByteArrayOutputStream out, final ClassID n) {
		byte[] b = new byte[16];
		n.write(b, 0);
		out.write(b, 0, b.length);
	}
}


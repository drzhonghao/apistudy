

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.hpsf.Filetime;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.Section;
import org.apache.poi.hpsf.Thumbnail;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.apache.poi.hpsf.Variant;
import org.apache.poi.hpsf.wellknown.PropertyIDMap;


public final class SummaryInformation extends PropertySet {
	public static final String DEFAULT_STREAM_NAME = "\u0005SummaryInformation";

	public static final ClassID FORMAT_ID = new ClassID("{F29F85E0-4FF9-1068-AB91-08002B27B3D9}");

	@Override
	public PropertyIDMap getPropertySetIDMap() {
		return PropertyIDMap.getSummaryInformationProperties();
	}

	public SummaryInformation() {
		getFirstSection().setFormatID(SummaryInformation.FORMAT_ID);
	}

	public SummaryInformation(final PropertySet ps) throws UnexpectedPropertySetTypeException {
		super(ps);
		if (!(isSummaryInformation())) {
			throw new UnexpectedPropertySetTypeException(("Not a " + (getClass().getName())));
		}
	}

	public SummaryInformation(final InputStream stream) throws IOException, UnsupportedEncodingException, MarkUnsupportedException, NoPropertySetStreamException {
		super(stream);
	}

	public String getTitle() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_TITLE);
	}

	public void setTitle(final String title) {
	}

	public void removeTitle() {
	}

	public String getSubject() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_SUBJECT);
	}

	public void setSubject(final String subject) {
	}

	public void removeSubject() {
	}

	public String getAuthor() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_AUTHOR);
	}

	public void setAuthor(final String author) {
	}

	public void removeAuthor() {
	}

	public String getKeywords() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_KEYWORDS);
	}

	public void setKeywords(final String keywords) {
	}

	public void removeKeywords() {
	}

	public String getComments() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_COMMENTS);
	}

	public void setComments(final String comments) {
	}

	public void removeComments() {
	}

	public String getTemplate() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_TEMPLATE);
	}

	public void setTemplate(final String template) {
	}

	public void removeTemplate() {
	}

	public String getLastAuthor() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_LASTAUTHOR);
	}

	public void setLastAuthor(final String lastAuthor) {
	}

	public void removeLastAuthor() {
	}

	public String getRevNumber() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_REVNUMBER);
	}

	public void setRevNumber(final String revNumber) {
	}

	public void removeRevNumber() {
	}

	public long getEditTime() {
		final Date d = ((Date) (getProperty(PropertyIDMap.PID_EDITTIME)));
		if (d == null) {
			return 0;
		}
		return Filetime.dateToFileTime(d);
	}

	public void setEditTime(final long time) {
		final Date d = Filetime.filetimeToDate(time);
		getFirstSection().setProperty(PropertyIDMap.PID_EDITTIME, Variant.VT_FILETIME, d);
	}

	public void removeEditTime() {
	}

	public Date getLastPrinted() {
		return ((Date) (getProperty(PropertyIDMap.PID_LASTPRINTED)));
	}

	public void setLastPrinted(final Date lastPrinted) {
		getFirstSection().setProperty(PropertyIDMap.PID_LASTPRINTED, Variant.VT_FILETIME, lastPrinted);
	}

	public void removeLastPrinted() {
	}

	public Date getCreateDateTime() {
		return ((Date) (getProperty(PropertyIDMap.PID_CREATE_DTM)));
	}

	public void setCreateDateTime(final Date createDateTime) {
		getFirstSection().setProperty(PropertyIDMap.PID_CREATE_DTM, Variant.VT_FILETIME, createDateTime);
	}

	public void removeCreateDateTime() {
	}

	public Date getLastSaveDateTime() {
		return ((Date) (getProperty(PropertyIDMap.PID_LASTSAVE_DTM)));
	}

	public void setLastSaveDateTime(final Date time) {
		final Section s = getFirstSection();
		s.setProperty(PropertyIDMap.PID_LASTSAVE_DTM, Variant.VT_FILETIME, time);
	}

	public void removeLastSaveDateTime() {
	}

	public int getPageCount() {
		return 0;
	}

	public void setPageCount(final int pageCount) {
	}

	public void removePageCount() {
	}

	public int getWordCount() {
		return 0;
	}

	public void setWordCount(final int wordCount) {
	}

	public void removeWordCount() {
	}

	public int getCharCount() {
		return 0;
	}

	public void setCharCount(final int charCount) {
	}

	public void removeCharCount() {
	}

	public byte[] getThumbnail() {
		return ((byte[]) (getProperty(PropertyIDMap.PID_THUMBNAIL)));
	}

	public Thumbnail getThumbnailThumbnail() {
		byte[] data = getThumbnail();
		if (data == null) {
			return null;
		}
		return new Thumbnail(data);
	}

	public void setThumbnail(final byte[] thumbnail) {
		getFirstSection().setProperty(PropertyIDMap.PID_THUMBNAIL, Variant.VT_LPSTR, thumbnail);
	}

	public void removeThumbnail() {
	}

	public String getApplicationName() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_APPNAME);
	}

	public void setApplicationName(final String applicationName) {
	}

	public void removeApplicationName() {
	}

	public int getSecurity() {
		return 0;
	}

	public void setSecurity(final int security) {
	}

	public void removeSecurity() {
	}
}


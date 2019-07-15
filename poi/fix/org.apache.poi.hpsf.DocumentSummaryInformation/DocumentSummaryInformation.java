

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.hpsf.CustomProperties;
import org.apache.poi.hpsf.CustomProperty;
import org.apache.poi.hpsf.HPSFRuntimeException;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.Property;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.Section;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.apache.poi.hpsf.wellknown.PropertyIDMap;


public class DocumentSummaryInformation extends PropertySet {
	public static final String DEFAULT_STREAM_NAME = "\u0005DocumentSummaryInformation";

	private static final ClassID DOC_SUMMARY_INFORMATION = new ClassID("{D5CDD502-2E9C-101B-9397-08002B2CF9AE}");

	private static final ClassID USER_DEFINED_PROPERTIES = new ClassID("{D5CDD505-2E9C-101B-9397-08002B2CF9AE}");

	public static final ClassID[] FORMAT_ID = new ClassID[]{ DocumentSummaryInformation.DOC_SUMMARY_INFORMATION, DocumentSummaryInformation.USER_DEFINED_PROPERTIES };

	@Override
	public PropertyIDMap getPropertySetIDMap() {
		return PropertyIDMap.getDocumentSummaryInformationProperties();
	}

	public DocumentSummaryInformation() {
		getFirstSection().setFormatID(DocumentSummaryInformation.DOC_SUMMARY_INFORMATION);
	}

	public DocumentSummaryInformation(final PropertySet ps) throws UnexpectedPropertySetTypeException {
		super(ps);
		if (!(isDocumentSummaryInformation())) {
			throw new UnexpectedPropertySetTypeException(("Not a " + (getClass().getName())));
		}
	}

	public DocumentSummaryInformation(final InputStream stream) throws IOException, UnsupportedEncodingException, MarkUnsupportedException, NoPropertySetStreamException {
		super(stream);
	}

	public String getCategory() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_CATEGORY);
	}

	public void setCategory(final String category) {
		getFirstSection().setProperty(PropertyIDMap.PID_CATEGORY, category);
	}

	public void removeCategory() {
	}

	public String getPresentationFormat() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_PRESFORMAT);
	}

	public void setPresentationFormat(final String presentationFormat) {
		getFirstSection().setProperty(PropertyIDMap.PID_PRESFORMAT, presentationFormat);
	}

	public void removePresentationFormat() {
	}

	public int getByteCount() {
		return 0;
	}

	public void setByteCount(final int byteCount) {
	}

	public void removeByteCount() {
	}

	public int getLineCount() {
		return 0;
	}

	public void setLineCount(final int lineCount) {
	}

	public void removeLineCount() {
	}

	public int getParCount() {
		return 0;
	}

	public void setParCount(final int parCount) {
	}

	public void removeParCount() {
	}

	public int getSlideCount() {
		return 0;
	}

	public void setSlideCount(final int slideCount) {
	}

	public void removeSlideCount() {
	}

	public int getNoteCount() {
		return 0;
	}

	public void setNoteCount(final int noteCount) {
	}

	public void removeNoteCount() {
	}

	public int getHiddenCount() {
		return 0;
	}

	public void setHiddenCount(final int hiddenCount) {
	}

	public void removeHiddenCount() {
	}

	public int getMMClipCount() {
		return 0;
	}

	public void setMMClipCount(final int mmClipCount) {
	}

	public void removeMMClipCount() {
	}

	public boolean getScale() {
		return false;
	}

	public void setScale(final boolean scale) {
	}

	public void removeScale() {
	}

	public byte[] getHeadingPair() {
		notYetImplemented("Reading byte arrays ");
		return ((byte[]) (getProperty(PropertyIDMap.PID_HEADINGPAIR)));
	}

	public void setHeadingPair(final byte[] headingPair) {
		notYetImplemented("Writing byte arrays ");
	}

	public void removeHeadingPair() {
	}

	public byte[] getDocparts() {
		notYetImplemented("Reading byte arrays");
		return ((byte[]) (getProperty(PropertyIDMap.PID_DOCPARTS)));
	}

	public void setDocparts(final byte[] docparts) {
		notYetImplemented("Writing byte arrays");
	}

	public void removeDocparts() {
	}

	public String getManager() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_MANAGER);
	}

	public void setManager(final String manager) {
	}

	public void removeManager() {
	}

	public String getCompany() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_COMPANY);
	}

	public void setCompany(final String company) {
	}

	public void removeCompany() {
	}

	public boolean getLinksDirty() {
		return false;
	}

	public void setLinksDirty(final boolean linksDirty) {
	}

	public void removeLinksDirty() {
	}

	public int getCharCountWithSpaces() {
		return 0;
	}

	public void setCharCountWithSpaces(int count) {
	}

	public void removeCharCountWithSpaces() {
	}

	public boolean getHyperlinksChanged() {
		return false;
	}

	public void setHyperlinksChanged(boolean changed) {
	}

	public void removeHyperlinksChanged() {
	}

	public int getApplicationVersion() {
		return 0;
	}

	public void setApplicationVersion(int version) {
	}

	public void removeApplicationVersion() {
	}

	public byte[] getVBADigitalSignature() {
		Object value = getProperty(PropertyIDMap.PID_DIGSIG);
		if ((value != null) && (value instanceof byte[])) {
			return ((byte[]) (value));
		}
		return null;
	}

	public void setVBADigitalSignature(byte[] signature) {
	}

	public void removeVBADigitalSignature() {
	}

	public String getContentType() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_CONTENTTYPE);
	}

	public void setContentType(String type) {
	}

	public void removeContentType() {
	}

	public String getContentStatus() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_CONTENTSTATUS);
	}

	public void setContentStatus(String status) {
	}

	public void removeContentStatus() {
	}

	public String getLanguage() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_LANGUAGE);
	}

	public void setLanguage(String language) {
	}

	public void removeLanguage() {
	}

	public String getDocumentVersion() {
		return PropertySet.getPropertyStringValue(PropertyIDMap.PID_DOCVERSION);
	}

	public void setDocumentVersion(String version) {
	}

	public void removeDocumentVersion() {
	}

	public CustomProperties getCustomProperties() {
		CustomProperties cps = null;
		if ((getSectionCount()) >= 2) {
			cps = new CustomProperties();
			final Section section = getSections().get(1);
			final Map<Long, String> dictionary = section.getDictionary();
			final Property[] properties = section.getProperties();
			int propertyCount = 0;
			for (Property p : properties) {
				final long id = p.getID();
				if (id == (PropertyIDMap.PID_CODEPAGE)) {
					cps.setCodepage(((Integer) (p.getValue())));
				}else
					if (id > (PropertyIDMap.PID_CODEPAGE)) {
						propertyCount++;
						final CustomProperty cp = new CustomProperty(p, dictionary.get(id));
						cps.put(cp.getName(), cp);
					}

			}
			if ((cps.size()) != propertyCount) {
				cps.setPure(false);
			}
		}
		return cps;
	}

	public void setCustomProperties(final CustomProperties customProperties) {
		ensureSection2();
		final Section section = getSections().get(1);
		int cpCodepage = customProperties.getCodepage();
		if (cpCodepage < 0) {
			cpCodepage = section.getCodepage();
		}
		if (cpCodepage < 0) {
			cpCodepage = Property.DEFAULT_CODEPAGE;
		}
		customProperties.setCodepage(cpCodepage);
		section.setCodepage(cpCodepage);
		for (CustomProperty p : customProperties.properties()) {
			section.setProperty(p);
		}
	}

	private void ensureSection2() {
		if ((getSectionCount()) < 2) {
			Section s2 = new Section();
			s2.setFormatID(DocumentSummaryInformation.USER_DEFINED_PROPERTIES);
			addSection(s2);
		}
	}

	public void removeCustomProperties() {
		if ((getSectionCount()) < 2) {
			throw new HPSFRuntimeException("Illegal internal format of Document SummaryInformation stream: second section is missing.");
		}
		List<Section> l = new LinkedList<>(getSections());
		clearSections();
		int idx = 0;
		for (Section s : l) {
			if ((idx++) != 1) {
				addSection(s);
			}
		}
	}

	private void notYetImplemented(final String msg) {
		throw new UnsupportedOperationException((msg + " is not yet implemented."));
	}
}


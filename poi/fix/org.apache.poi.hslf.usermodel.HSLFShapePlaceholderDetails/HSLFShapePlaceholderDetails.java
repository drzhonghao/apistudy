

import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.record.OEPlaceholderAtom;
import org.apache.poi.hslf.record.RoundTripHFPlaceholder12;
import org.apache.poi.hslf.usermodel.HSLFPlaceholderDetails;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSheet;
import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.PlaceholderDetails;

import static org.apache.poi.sl.usermodel.PlaceholderDetails.PlaceholderSize.full;
import static org.apache.poi.sl.usermodel.PlaceholderDetails.PlaceholderSize.half;
import static org.apache.poi.sl.usermodel.PlaceholderDetails.PlaceholderSize.quarter;


public class HSLFShapePlaceholderDetails extends HSLFPlaceholderDetails {
	private enum PlaceholderContainer {

		slide,
		master,
		notes,
		notesMaster;}

	private final HSLFShapePlaceholderDetails.PlaceholderContainer source;

	final HSLFSimpleShape shape;

	private OEPlaceholderAtom oePlaceholderAtom;

	private RoundTripHFPlaceholder12 roundTripHFPlaceholder12;

	public Placeholder getPlaceholder() {
		updatePlaceholderAtom(false);
		final int phId;
		if ((oePlaceholderAtom) != null) {
			phId = oePlaceholderAtom.getPlaceholderId();
		}else
			if ((roundTripHFPlaceholder12) != null) {
				phId = roundTripHFPlaceholder12.getPlaceholderId();
			}else {
				return null;
			}

		switch (source) {
			case slide :
				return Placeholder.lookupNativeSlide(phId);
			default :
			case master :
				return Placeholder.lookupNativeSlideMaster(phId);
			case notes :
				return Placeholder.lookupNativeNotes(phId);
			case notesMaster :
				return Placeholder.lookupNativeNotesMaster(phId);
		}
	}

	public void setPlaceholder(final Placeholder placeholder) {
		final EscherSpRecord spRecord = shape.getEscherChild(EscherSpRecord.RECORD_ID);
		int flags = spRecord.getFlags();
		if (placeholder == null) {
			flags ^= EscherSpRecord.FLAG_HAVEMASTER;
		}else {
			flags |= (EscherSpRecord.FLAG_HAVEANCHOR) | (EscherSpRecord.FLAG_HAVEMASTER);
		}
		spRecord.setFlags(flags);
		shape.setEscherProperty(EscherProperties.PROTECTION__LOCKAGAINSTGROUPING, (placeholder == null ? -1 : 262144));
		if (placeholder == null) {
			removePlaceholder();
			return;
		}
		updatePlaceholderAtom(true);
		final byte phId = getPlaceholderId(placeholder);
		oePlaceholderAtom.setPlaceholderId(phId);
		roundTripHFPlaceholder12.setPlaceholderId(phId);
	}

	public PlaceholderDetails.PlaceholderSize getSize() {
		final Placeholder ph = getPlaceholder();
		if (ph == null) {
			return null;
		}
		final int size = ((oePlaceholderAtom) != null) ? oePlaceholderAtom.getPlaceholderSize() : OEPlaceholderAtom.PLACEHOLDER_HALFSIZE;
		switch (size) {
			case OEPlaceholderAtom.PLACEHOLDER_FULLSIZE :
				return full;
			default :
			case OEPlaceholderAtom.PLACEHOLDER_HALFSIZE :
				return half;
			case OEPlaceholderAtom.PLACEHOLDER_QUARTSIZE :
				return quarter;
		}
	}

	public void setSize(final PlaceholderDetails.PlaceholderSize size) {
		final Placeholder ph = getPlaceholder();
		if ((ph == null) || (size == null)) {
			return;
		}
		updatePlaceholderAtom(true);
		final byte ph_size;
		switch (size) {
			case full :
				ph_size = OEPlaceholderAtom.PLACEHOLDER_FULLSIZE;
				break;
			default :
			case half :
				ph_size = OEPlaceholderAtom.PLACEHOLDER_HALFSIZE;
				break;
			case quarter :
				ph_size = OEPlaceholderAtom.PLACEHOLDER_QUARTSIZE;
				break;
		}
		oePlaceholderAtom.setPlaceholderSize(ph_size);
	}

	private byte getPlaceholderId(final Placeholder placeholder) {
		final byte phId;
		switch (source) {
			default :
			case slide :
				phId = ((byte) (placeholder.nativeSlideId));
				break;
			case master :
				phId = ((byte) (placeholder.nativeSlideMasterId));
				break;
			case notes :
				phId = ((byte) (placeholder.nativeNotesId));
				break;
			case notesMaster :
				phId = ((byte) (placeholder.nativeNotesMasterId));
				break;
		}
		if (phId == (-2)) {
			throw new HSLFException((((("Placeholder " + (placeholder.name())) + " not supported for this sheet type (") + (shape.getSheet().getClass())) + ")"));
		}
		return phId;
	}

	private void removePlaceholder() {
		oePlaceholderAtom = null;
		roundTripHFPlaceholder12 = null;
	}

	private void updatePlaceholderAtom(final boolean create) {
		if (!create) {
			return;
		}
		if ((oePlaceholderAtom) == null) {
			oePlaceholderAtom = new OEPlaceholderAtom();
			oePlaceholderAtom.setPlaceholderSize(((byte) (OEPlaceholderAtom.PLACEHOLDER_FULLSIZE)));
			oePlaceholderAtom.setPlacementId((-1));
		}
		if ((roundTripHFPlaceholder12) == null) {
			roundTripHFPlaceholder12 = new RoundTripHFPlaceholder12();
		}
	}
}




import org.apache.poi.hwpf.model.FieldDescriptor;
import org.apache.poi.hwpf.model.PlexOfField;
import org.apache.poi.hwpf.model.types.FLDAbstractType;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Field;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.util.Internal;


@Internal
class FieldImpl implements Field {
	private PlexOfField endPlex;

	private PlexOfField separatorPlex;

	private PlexOfField startPlex;

	public FieldImpl(PlexOfField startPlex, PlexOfField separatorPlex, PlexOfField endPlex) {
		if (startPlex == null)
			throw new IllegalArgumentException("startPlex == null");

		if (endPlex == null)
			throw new IllegalArgumentException("endPlex == null");

		if ((startPlex.getFld().getBoundaryType()) != (FieldDescriptor.FIELD_BEGIN_MARK))
			throw new IllegalArgumentException((("startPlex (" + startPlex) + ") is not type of FIELD_BEGIN"));

		if ((separatorPlex != null) && ((separatorPlex.getFld().getBoundaryType()) != (FieldDescriptor.FIELD_SEPARATOR_MARK)))
			throw new IllegalArgumentException((("separatorPlex" + separatorPlex) + ") is not type of FIELD_SEPARATOR"));

		if ((endPlex.getFld().getBoundaryType()) != (FieldDescriptor.FIELD_END_MARK))
			throw new IllegalArgumentException((("endPlex (" + endPlex) + ") is not type of FIELD_END"));

		this.startPlex = startPlex;
		this.separatorPlex = separatorPlex;
		this.endPlex = endPlex;
	}

	public Range firstSubrange(Range parent) {
		if (hasSeparator()) {
			if (((getMarkStartOffset()) + 1) == (getMarkSeparatorOffset()))
				return null;

			return new Range(((getMarkStartOffset()) + 1), getMarkSeparatorOffset(), parent) {
				@Override
				public String toString() {
					return ("FieldSubrange1 (" + (super.toString())) + ")";
				}
			};
		}
		if (((getMarkStartOffset()) + 1) == (getMarkEndOffset()))
			return null;

		return new Range(((getMarkStartOffset()) + 1), getMarkEndOffset(), parent) {
			@Override
			public String toString() {
				return ("FieldSubrange1 (" + (super.toString())) + ")";
			}
		};
	}

	public int getFieldEndOffset() {
		return (endPlex.getFcStart()) + 1;
	}

	public int getFieldStartOffset() {
		return startPlex.getFcStart();
	}

	public CharacterRun getMarkEndCharacterRun(Range parent) {
		return null;
	}

	public int getMarkEndOffset() {
		return endPlex.getFcStart();
	}

	public CharacterRun getMarkSeparatorCharacterRun(Range parent) {
		if (!(hasSeparator()))
			return null;

		return null;
	}

	public int getMarkSeparatorOffset() {
		return separatorPlex.getFcStart();
	}

	public CharacterRun getMarkStartCharacterRun(Range parent) {
		return null;
	}

	public int getMarkStartOffset() {
		return startPlex.getFcStart();
	}

	public int getType() {
		return startPlex.getFld().getFieldType();
	}

	public boolean hasSeparator() {
		return (separatorPlex) != null;
	}

	public boolean isHasSep() {
		return endPlex.getFld().isFHasSep();
	}

	public boolean isLocked() {
		return endPlex.getFld().isFLocked();
	}

	public boolean isNested() {
		return endPlex.getFld().isFNested();
	}

	public boolean isPrivateResult() {
		return endPlex.getFld().isFPrivateResult();
	}

	public boolean isResultDirty() {
		return endPlex.getFld().isFResultDirty();
	}

	public boolean isResultEdited() {
		return endPlex.getFld().isFResultEdited();
	}

	public boolean isZombieEmbed() {
		return endPlex.getFld().isFZombieEmbed();
	}

	public Range secondSubrange(Range parent) {
		if ((!(hasSeparator())) || (((getMarkSeparatorOffset()) + 1) == (getMarkEndOffset())))
			return null;

		return new Range(((getMarkSeparatorOffset()) + 1), getMarkEndOffset(), parent) {
			@Override
			public String toString() {
				return ("FieldSubrange2 (" + (super.toString())) + ")";
			}
		};
	}

	@Override
	public String toString() {
		return ((((((("Field [" + (getFieldStartOffset())) + "; ") + (getFieldEndOffset())) + "] (type: 0x") + (Integer.toHexString(getType()))) + " = ") + (getType())) + " )";
	}
}


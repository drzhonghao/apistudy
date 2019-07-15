

import org.apache.poi.hssf.record.DVRecord;
import org.apache.poi.hssf.usermodel.DVConstraint;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.util.CellRangeAddressList;

import static org.apache.poi.ss.usermodel.DataValidation.ErrorStyle.STOP;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.LIST;


public final class HSSFDataValidation implements DataValidation {
	private String _prompt_title;

	private String _prompt_text;

	private String _error_title;

	private String _error_text;

	private int _errorStyle = STOP;

	private boolean _emptyCellAllowed = true;

	private boolean _suppress_dropdown_arrow;

	private boolean _showPromptBox = true;

	private boolean _showErrorBox = true;

	private CellRangeAddressList _regions;

	private DVConstraint _constraint;

	public HSSFDataValidation(CellRangeAddressList regions, DataValidationConstraint constraint) {
		_regions = regions;
		_constraint = ((DVConstraint) (constraint));
	}

	public DataValidationConstraint getValidationConstraint() {
		return _constraint;
	}

	public DVConstraint getConstraint() {
		return _constraint;
	}

	public CellRangeAddressList getRegions() {
		return _regions;
	}

	public void setErrorStyle(int error_style) {
		_errorStyle = error_style;
	}

	public int getErrorStyle() {
		return _errorStyle;
	}

	public void setEmptyCellAllowed(boolean allowed) {
		_emptyCellAllowed = allowed;
	}

	public boolean getEmptyCellAllowed() {
		return _emptyCellAllowed;
	}

	public void setSuppressDropDownArrow(boolean suppress) {
		_suppress_dropdown_arrow = suppress;
	}

	public boolean getSuppressDropDownArrow() {
		if ((_constraint.getValidationType()) == (LIST)) {
			return _suppress_dropdown_arrow;
		}
		return false;
	}

	public void setShowPromptBox(boolean show) {
		_showPromptBox = show;
	}

	public boolean getShowPromptBox() {
		return _showPromptBox;
	}

	public void setShowErrorBox(boolean show) {
		_showErrorBox = show;
	}

	public boolean getShowErrorBox() {
		return _showErrorBox;
	}

	public void createPromptBox(String title, String text) {
		if ((title != null) && ((title.length()) > 32)) {
			throw new IllegalStateException(("Prompt-title cannot be longer than 32 characters, but had: " + title));
		}
		if ((text != null) && ((text.length()) > 255)) {
			throw new IllegalStateException(("Prompt-text cannot be longer than 255 characters, but had: " + text));
		}
		_prompt_title = title;
		_prompt_text = text;
		this.setShowPromptBox(true);
	}

	public String getPromptBoxTitle() {
		return _prompt_title;
	}

	public String getPromptBoxText() {
		return _prompt_text;
	}

	public void createErrorBox(String title, String text) {
		if ((title != null) && ((title.length()) > 32)) {
			throw new IllegalStateException(("Error-title cannot be longer than 32 characters, but had: " + title));
		}
		if ((text != null) && ((text.length()) > 255)) {
			throw new IllegalStateException(("Error-text cannot be longer than 255 characters, but had: " + text));
		}
		_error_title = title;
		_error_text = text;
		this.setShowErrorBox(true);
	}

	public String getErrorBoxTitle() {
		return _error_title;
	}

	public String getErrorBoxText() {
		return _error_text;
	}

	public DVRecord createDVRecord(HSSFSheet sheet) {
		return null;
	}
}


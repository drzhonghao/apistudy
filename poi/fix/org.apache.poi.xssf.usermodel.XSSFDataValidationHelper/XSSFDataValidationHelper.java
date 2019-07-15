

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDataValidation;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STDataValidationErrorStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STDataValidationType;

import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.ANY;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.DATE;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.DECIMAL;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.FORMULA;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.INTEGER;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.LIST;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.TEXT_LENGTH;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.TIME;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDataValidation.Factory.newInstance;


public class XSSFDataValidationHelper implements DataValidationHelper {
	public XSSFDataValidationHelper(XSSFSheet xssfSheet) {
		super();
	}

	public DataValidationConstraint createDateConstraint(int operatorType, String formula1, String formula2, String dateFormat) {
		return new XSSFDataValidationConstraint(DATE, operatorType, formula1, formula2);
	}

	public DataValidationConstraint createDecimalConstraint(int operatorType, String formula1, String formula2) {
		return new XSSFDataValidationConstraint(DECIMAL, operatorType, formula1, formula2);
	}

	public DataValidationConstraint createExplicitListConstraint(String[] listOfValues) {
		return new XSSFDataValidationConstraint(listOfValues);
	}

	public DataValidationConstraint createFormulaListConstraint(String listFormula) {
		return new XSSFDataValidationConstraint(LIST, listFormula);
	}

	public DataValidationConstraint createNumericConstraint(int validationType, int operatorType, String formula1, String formula2) {
		if (validationType == (INTEGER)) {
			return createIntegerConstraint(operatorType, formula1, formula2);
		}else
			if (validationType == (DECIMAL)) {
				return createDecimalConstraint(operatorType, formula1, formula2);
			}else
				if (validationType == (TEXT_LENGTH)) {
					return createTextLengthConstraint(operatorType, formula1, formula2);
				}


		return null;
	}

	public DataValidationConstraint createIntegerConstraint(int operatorType, String formula1, String formula2) {
		return new XSSFDataValidationConstraint(INTEGER, operatorType, formula1, formula2);
	}

	public DataValidationConstraint createTextLengthConstraint(int operatorType, String formula1, String formula2) {
		return new XSSFDataValidationConstraint(TEXT_LENGTH, operatorType, formula1, formula2);
	}

	public DataValidationConstraint createTimeConstraint(int operatorType, String formula1, String formula2) {
		return new XSSFDataValidationConstraint(TIME, operatorType, formula1, formula2);
	}

	public DataValidationConstraint createCustomConstraint(String formula) {
		return new XSSFDataValidationConstraint(FORMULA, formula);
	}

	public DataValidation createValidation(DataValidationConstraint constraint, CellRangeAddressList cellRangeAddressList) {
		XSSFDataValidationConstraint dataValidationConstraint = ((XSSFDataValidationConstraint) (constraint));
		CTDataValidation newDataValidation = newInstance();
		int validationType = constraint.getValidationType();
		switch (validationType) {
			case LIST :
				newDataValidation.setType(STDataValidationType.LIST);
				newDataValidation.setFormula1(constraint.getFormula1());
				break;
			case ANY :
				newDataValidation.setType(STDataValidationType.NONE);
				break;
			case TEXT_LENGTH :
				newDataValidation.setType(STDataValidationType.TEXT_LENGTH);
				break;
			case DATE :
				newDataValidation.setType(STDataValidationType.DATE);
				break;
			case INTEGER :
				newDataValidation.setType(STDataValidationType.WHOLE);
				break;
			case DECIMAL :
				newDataValidation.setType(STDataValidationType.DECIMAL);
				break;
			case TIME :
				newDataValidation.setType(STDataValidationType.TIME);
				break;
			case FORMULA :
				newDataValidation.setType(STDataValidationType.CUSTOM);
				break;
			default :
				newDataValidation.setType(STDataValidationType.NONE);
		}
		if ((validationType != (ANY)) && (validationType != (LIST))) {
			if ((constraint.getFormula1()) != null) {
				newDataValidation.setFormula1(constraint.getFormula1());
			}
			if ((constraint.getFormula2()) != null) {
				newDataValidation.setFormula2(constraint.getFormula2());
			}
		}
		CellRangeAddress[] cellRangeAddresses = cellRangeAddressList.getCellRangeAddresses();
		List<String> sqref = new ArrayList<>();
		for (int i = 0; i < (cellRangeAddresses.length); i++) {
			CellRangeAddress cellRangeAddress = cellRangeAddresses[i];
			sqref.add(cellRangeAddress.formatAsString());
		}
		newDataValidation.setSqref(sqref);
		newDataValidation.setAllowBlank(true);
		newDataValidation.setErrorStyle(STDataValidationErrorStyle.STOP);
		return new XSSFDataValidation(dataValidationConstraint, cellRangeAddressList, newDataValidation);
	}
}


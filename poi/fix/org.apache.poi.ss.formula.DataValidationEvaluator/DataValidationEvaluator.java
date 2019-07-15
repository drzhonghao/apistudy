

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.WorkbookEvaluatorProvider;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.SheetUtil;

import static org.apache.poi.ss.usermodel.DataValidationConstraint.OperatorType.BETWEEN;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.OperatorType.NOT_BETWEEN;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.LIST;


public class DataValidationEvaluator {
	private final Map<String, List<? extends DataValidation>> validations = new HashMap<>();

	private final Workbook workbook;

	private final WorkbookEvaluator workbookEvaluator;

	public DataValidationEvaluator(Workbook wb, WorkbookEvaluatorProvider provider) {
		this.workbook = wb;
		this.workbookEvaluator = provider._getWorkbookEvaluator();
	}

	protected WorkbookEvaluator getWorkbookEvaluator() {
		return workbookEvaluator;
	}

	public void clearAllCachedValues() {
		validations.clear();
	}

	private List<? extends DataValidation> getValidations(Sheet sheet) {
		List<? extends DataValidation> dvs = validations.get(sheet.getSheetName());
		if ((dvs == null) && (!(validations.containsKey(sheet.getSheetName())))) {
			dvs = sheet.getDataValidations();
			validations.put(sheet.getSheetName(), dvs);
		}
		return dvs;
	}

	public DataValidation getValidationForCell(CellReference cell) {
		final DataValidationEvaluator.DataValidationContext vc = getValidationContextForCell(cell);
		return vc == null ? null : vc.getValidation();
	}

	public DataValidationEvaluator.DataValidationContext getValidationContextForCell(CellReference cell) {
		final Sheet sheet = workbook.getSheet(cell.getSheetName());
		if (sheet == null)
			return null;

		final List<? extends DataValidation> dataValidations = getValidations(sheet);
		if (dataValidations == null)
			return null;

		for (DataValidation dv : dataValidations) {
			final CellRangeAddressList regions = dv.getRegions();
			if (regions == null)
				return null;

			for (CellRangeAddressBase range : regions.getCellRangeAddresses()) {
				if (range.isInRange(cell)) {
					return new DataValidationEvaluator.DataValidationContext(dv, this, range, cell);
				}
			}
		}
		return null;
	}

	public List<ValueEval> getValidationValuesForCell(CellReference cell) {
		DataValidationEvaluator.DataValidationContext context = getValidationContextForCell(cell);
		if (context == null)
			return null;

		return DataValidationEvaluator.getValidationValuesForConstraint(context);
	}

	protected static List<ValueEval> getValidationValuesForConstraint(DataValidationEvaluator.DataValidationContext context) {
		final DataValidationConstraint val = context.getValidation().getValidationConstraint();
		if ((val.getValidationType()) != (LIST))
			return null;

		String formula = val.getFormula1();
		final List<ValueEval> values = new ArrayList<>();
		if (((val.getExplicitListValues()) != null) && ((val.getExplicitListValues().length) > 0)) {
			for (String s : val.getExplicitListValues()) {
				if (s != null)
					values.add(new StringEval(s));

			}
		}else
			if (formula != null) {
				ValueEval eval = context.getEvaluator().getWorkbookEvaluator().evaluateList(formula, context.getTarget(), context.getRegion());
				if (eval instanceof TwoDEval) {
					TwoDEval twod = ((TwoDEval) (eval));
					for (int i = 0; i < (twod.getHeight()); i++) {
						final ValueEval cellValue = twod.getValue(i, 0);
						values.add(cellValue);
					}
				}
			}

		return Collections.unmodifiableList(values);
	}

	public boolean isValidCell(CellReference cellRef) {
		final DataValidationEvaluator.DataValidationContext context = getValidationContextForCell(cellRef);
		if (context == null)
			return true;

		final Cell cell = SheetUtil.getCell(workbook.getSheet(cellRef.getSheetName()), cellRef.getRow(), cellRef.getCol());
		if (((cell == null) || (DataValidationEvaluator.isType(cell, CellType.BLANK))) || ((DataValidationEvaluator.isType(cell, CellType.STRING)) && (((cell.getStringCellValue()) == null) || (cell.getStringCellValue().isEmpty())))) {
			return context.getValidation().getEmptyCellAllowed();
		}
		return DataValidationEvaluator.ValidationEnum.isValid(cell, context);
	}

	public static boolean isType(Cell cell, CellType type) {
		final CellType cellType = cell.getCellType();
		return (cellType == type) || ((cellType == (CellType.FORMULA)) && ((cell.getCachedFormulaResultType()) == type));
	}

	public static enum ValidationEnum {

		ANY() {
			public boolean isValidValue(Cell cell, DataValidationEvaluator.DataValidationContext context) {
				return true;
			}
		},
		INTEGER() {
			public boolean isValidValue(Cell cell, DataValidationEvaluator.DataValidationContext context) {
				if (super.isValidValue(cell, context)) {
					final double value = cell.getNumericCellValue();
					return (Double.compare(value, ((int) (value)))) == 0;
				}
				return false;
			}
		},
		DECIMAL,
		LIST() {
			public boolean isValidValue(Cell cell, DataValidationEvaluator.DataValidationContext context) {
				final List<ValueEval> valueList = DataValidationEvaluator.getValidationValuesForConstraint(context);
				if (valueList == null)
					return true;

				for (ValueEval listVal : valueList) {
					ValueEval comp = (listVal instanceof RefEval) ? ((RefEval) (listVal)).getInnerValueEval(context.getSheetIndex()) : listVal;
					if (comp instanceof BlankEval)
						return true;

					if (comp instanceof ErrorEval)
						continue;

					if (comp instanceof BoolEval) {
						if ((DataValidationEvaluator.isType(cell, CellType.BOOLEAN)) && ((((BoolEval) (comp)).getBooleanValue()) == (cell.getBooleanCellValue()))) {
							return true;
						}else {
							continue;
						}
					}
					if (comp instanceof NumberEval) {
						if ((DataValidationEvaluator.isType(cell, CellType.NUMERIC)) && ((((NumberEval) (comp)).getNumberValue()) == (cell.getNumericCellValue()))) {
							return true;
						}else {
							continue;
						}
					}
					if (comp instanceof StringEval) {
						if ((DataValidationEvaluator.isType(cell, CellType.STRING)) && (((StringEval) (comp)).getStringValue().equalsIgnoreCase(cell.getStringCellValue()))) {
							return true;
						}else {
							continue;
						}
					}
				}
				return false;
			}
		},
		DATE,
		TIME,
		TEXT_LENGTH() {
			public boolean isValidValue(Cell cell, DataValidationEvaluator.DataValidationContext context) {
				if (!(DataValidationEvaluator.isType(cell, CellType.STRING)))
					return false;

				String v = cell.getStringCellValue();
				return isValidNumericValue(Double.valueOf(v.length()), context);
			}
		},
		FORMULA() {
			public boolean isValidValue(Cell cell, DataValidationEvaluator.DataValidationContext context) {
				ValueEval comp = context.getEvaluator().getWorkbookEvaluator().evaluate(context.getFormula1(), context.getTarget(), context.getRegion());
				if (comp instanceof RefEval) {
					comp = ((RefEval) (comp)).getInnerValueEval(((RefEval) (comp)).getFirstSheetIndex());
				}
				if (comp instanceof BlankEval)
					return true;

				if (comp instanceof ErrorEval)
					return false;

				if (comp instanceof BoolEval) {
					return ((BoolEval) (comp)).getBooleanValue();
				}
				if (comp instanceof NumberEval) {
					return (((NumberEval) (comp)).getNumberValue()) != 0;
				}
				return false;
			}
		};
		public boolean isValidValue(Cell cell, DataValidationEvaluator.DataValidationContext context) {
			return isValidNumericCell(cell, context);
		}

		protected boolean isValidNumericCell(Cell cell, DataValidationEvaluator.DataValidationContext context) {
			if (!(DataValidationEvaluator.isType(cell, CellType.NUMERIC)))
				return false;

			Double value = Double.valueOf(cell.getNumericCellValue());
			return isValidNumericValue(value, context);
		}

		protected boolean isValidNumericValue(Double value, final DataValidationEvaluator.DataValidationContext context) {
			try {
				Double t1 = evalOrConstant(context.getFormula1(), context);
				if (t1 == null)
					return true;

				Double t2 = null;
				if (((context.getOperator()) == (BETWEEN)) || ((context.getOperator()) == (NOT_BETWEEN))) {
					t2 = evalOrConstant(context.getFormula2(), context);
					if (t2 == null)
						return true;

				}
				return DataValidationEvaluator.OperatorEnum.values()[context.getOperator()].isValid(value, t1, t2);
			} catch (NumberFormatException e) {
				return false;
			}
		}

		private Double evalOrConstant(String formula, DataValidationEvaluator.DataValidationContext context) throws NumberFormatException {
			if ((formula == null) || (formula.trim().isEmpty()))
				return null;

			try {
				return Double.valueOf(formula);
			} catch (NumberFormatException e) {
			}
			ValueEval eval = context.getEvaluator().getWorkbookEvaluator().evaluate(formula, context.getTarget(), context.getRegion());
			if (eval instanceof RefEval) {
				eval = ((RefEval) (eval)).getInnerValueEval(((RefEval) (eval)).getFirstSheetIndex());
			}
			if (eval instanceof BlankEval)
				return null;

			if (eval instanceof NumberEval)
				return Double.valueOf(((NumberEval) (eval)).getNumberValue());

			if (eval instanceof StringEval) {
				final String value = ((StringEval) (eval)).getStringValue();
				if ((value == null) || (value.trim().isEmpty()))
					return null;

				return Double.valueOf(value);
			}
			throw new NumberFormatException((("Formula '" + formula) + "' evaluates to something other than a number"));
		}

		public static boolean isValid(Cell cell, DataValidationEvaluator.DataValidationContext context) {
			return DataValidationEvaluator.ValidationEnum.values()[context.getValidation().getValidationConstraint().getValidationType()].isValidValue(cell, context);
		}
	}

	public static enum OperatorEnum {

		BETWEEN() {
			public boolean isValid(Double cellValue, Double v1, Double v2) {
				return ((cellValue.compareTo(v1)) >= 0) && ((cellValue.compareTo(v2)) <= 0);
			}
		},
		NOT_BETWEEN() {
			public boolean isValid(Double cellValue, Double v1, Double v2) {
				return ((cellValue.compareTo(v1)) < 0) || ((cellValue.compareTo(v2)) > 0);
			}
		},
		EQUAL() {
			public boolean isValid(Double cellValue, Double v1, Double v2) {
				return (cellValue.compareTo(v1)) == 0;
			}
		},
		NOT_EQUAL() {
			public boolean isValid(Double cellValue, Double v1, Double v2) {
				return (cellValue.compareTo(v1)) != 0;
			}
		},
		GREATER_THAN() {
			public boolean isValid(Double cellValue, Double v1, Double v2) {
				return (cellValue.compareTo(v1)) > 0;
			}
		},
		LESS_THAN() {
			public boolean isValid(Double cellValue, Double v1, Double v2) {
				return (cellValue.compareTo(v1)) < 0;
			}
		},
		GREATER_OR_EQUAL() {
			public boolean isValid(Double cellValue, Double v1, Double v2) {
				return (cellValue.compareTo(v1)) >= 0;
			}
		},
		LESS_OR_EQUAL() {
			public boolean isValid(Double cellValue, Double v1, Double v2) {
				return (cellValue.compareTo(v1)) <= 0;
			}
		};
		public static final DataValidationEvaluator.OperatorEnum IGNORED = DataValidationEvaluator.OperatorEnum.BETWEEN;

		public abstract boolean isValid(Double cellValue, Double v1, Double v2);
	}

	public static class DataValidationContext {
		private final DataValidation dv;

		private final DataValidationEvaluator dve;

		private final CellRangeAddressBase region;

		private final CellReference target;

		public DataValidationContext(DataValidation dv, DataValidationEvaluator dve, CellRangeAddressBase region, CellReference target) {
			this.dv = dv;
			this.dve = dve;
			this.region = region;
			this.target = target;
		}

		public DataValidation getValidation() {
			return dv;
		}

		public DataValidationEvaluator getEvaluator() {
			return dve;
		}

		public CellRangeAddressBase getRegion() {
			return region;
		}

		public CellReference getTarget() {
			return target;
		}

		public int getOffsetColumns() {
			return (target.getCol()) - (region.getFirstColumn());
		}

		public int getOffsetRows() {
			return (target.getRow()) - (region.getFirstRow());
		}

		public int getSheetIndex() {
			return 0;
		}

		public String getFormula1() {
			return dv.getValidationConstraint().getFormula1();
		}

		public String getFormula2() {
			return dv.getValidationConstraint().getFormula2();
		}

		public int getOperator() {
			return dv.getValidationConstraint().getOperator();
		}
	}
}


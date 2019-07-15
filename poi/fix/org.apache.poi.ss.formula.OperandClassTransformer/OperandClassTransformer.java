

import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.AbstractFunctionPtg;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.Ptg;


final class OperandClassTransformer {
	private final FormulaType _formulaType;

	public OperandClassTransformer(FormulaType formulaType) {
		_formulaType = formulaType;
	}

	private static boolean isSingleArgSum(Ptg token) {
		if (token instanceof AttrPtg) {
			AttrPtg attrPtg = ((AttrPtg) (token));
			return attrPtg.isSum();
		}
		return false;
	}

	private static boolean isSimpleValueFunction(Ptg token) {
		if (token instanceof AbstractFunctionPtg) {
			AbstractFunctionPtg aptg = ((AbstractFunctionPtg) (token));
			if ((aptg.getDefaultOperandClass()) != (Ptg.CLASS_VALUE)) {
				return false;
			}
			int numberOfOperands = aptg.getNumberOfOperands();
			for (int i = numberOfOperands - 1; i >= 0; i--) {
				if ((aptg.getParameterClass(i)) != (Ptg.CLASS_VALUE)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private byte transformClass(byte currentOperandClass, byte desiredOperandClass, boolean callerForceArrayFlag) {
		switch (desiredOperandClass) {
			case Ptg.CLASS_VALUE :
				if (!callerForceArrayFlag) {
					return Ptg.CLASS_VALUE;
				}
			case Ptg.CLASS_ARRAY :
				return Ptg.CLASS_ARRAY;
			case Ptg.CLASS_REF :
				if (!callerForceArrayFlag) {
					return currentOperandClass;
				}
				return Ptg.CLASS_REF;
		}
		throw new IllegalStateException((("Unexpected operand class (" + desiredOperandClass) + ")"));
	}

	private void setSimpleValueFuncClass(AbstractFunctionPtg afp, byte desiredOperandClass, boolean callerForceArrayFlag) {
		if (callerForceArrayFlag || (desiredOperandClass == (Ptg.CLASS_ARRAY))) {
			afp.setClass(Ptg.CLASS_ARRAY);
		}else {
			afp.setClass(Ptg.CLASS_VALUE);
		}
	}
}


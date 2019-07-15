import org.apache.poi.ss.formula.eval.*;


import org.apache.poi.ss.formula.EvaluationName;

/**
 * Evaluation of a Name defined in a Sheet or Workbook scope
 */
public final class ExternalNameEval implements ValueEval {
	private final EvaluationName _name;

	public ExternalNameEval(EvaluationName name) {
		_name = name;
	}

	public EvaluationName getName() {
		return _name;
	}

	public String toString() {
		return getClass().getName() + " [" +
				_name.getNameText() +
				"]";
	}
}

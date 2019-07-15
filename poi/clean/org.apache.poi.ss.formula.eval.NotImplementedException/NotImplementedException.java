import org.apache.poi.ss.formula.eval.*;


import org.apache.poi.ss.usermodel.FormulaEvaluator;

/**
 * An exception thrown by implementors of {@link FormulaEvaluator},
 *  when attempting to evaluate a formula which requires features
 *   that POI does not (yet) support.
 * 
 * <p>Where possible, a subclass of this should be thrown, to provide
 *  more detail of what part of the formula couldn't be processed due
 *  to a missing implementation
 */
public class NotImplementedException extends RuntimeException {
    private static final long serialVersionUID = -5840703336495141301L;
    
    public NotImplementedException(String message) {
		super(message);
	}
	public NotImplementedException(String message, NotImplementedException cause) {
		super(message, cause);
	}
}

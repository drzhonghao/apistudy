import org.apache.poi.ss.formula.eval.*;


/**
 * Represents the (intermediate) evaluated result of a missing function argument.  In most cases
 * this can be translated into {@link BlankEval} but there are some notable exceptions.  Functions
 * COUNT and COUNTA <em>do</em> count their missing args.  Note - the differences between
 * {@link MissingArgEval} and {@link BlankEval} have not been investigated fully, so the POI
 * evaluator may need to be updated to account for these as they are found.
 *
 * @author Josh Micich
 */
public final class MissingArgEval implements ValueEval {

	public static final MissingArgEval instance = new MissingArgEval();

	private MissingArgEval() {
		// enforce singleton
	}
}

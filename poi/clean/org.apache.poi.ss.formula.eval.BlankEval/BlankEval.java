import org.apache.poi.ss.formula.eval.*;


/**
 * @author Amol S. Deshmukh &lt; amolweb at ya hoo dot com &gt; This class is a
 *         marker class. It is a special value for empty cells.
 */
public final class BlankEval implements ValueEval {

	public static final BlankEval instance = new BlankEval();

	private BlankEval() {
		// enforce singleton
	}
}

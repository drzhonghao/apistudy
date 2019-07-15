import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.lucene.util.automaton.*;



/**
 * This exception is thrown when determinizing an automaton would result in one
 * has too many states.
 */
public class TooComplexToDeterminizeException extends RuntimeException {
  private transient final Automaton automaton;
  private transient final RegExp regExp;
  private transient final int maxDeterminizedStates;

  /** Use this constructor when the RegExp failed to convert to an automaton. */
  public TooComplexToDeterminizeException(RegExp regExp, TooComplexToDeterminizeException cause) {
    super("Determinizing " + regExp.getOriginalString() + " would result in more than " +
      cause.maxDeterminizedStates + " states.", cause);
    this.regExp = regExp;
    this.automaton = cause.automaton;
    this.maxDeterminizedStates = cause.maxDeterminizedStates;
  }

  /** Use this constructor when the automaton failed to determinize. */
  public TooComplexToDeterminizeException(Automaton automaton, int maxDeterminizedStates) {
    super("Determinizing automaton with " + automaton.getNumStates() + " states and " + automaton.getNumTransitions() + " transitions would result in more than " + maxDeterminizedStates + " states.");
    this.automaton = automaton;
    this.regExp = null;
    this.maxDeterminizedStates = maxDeterminizedStates;
  }

  /** Returns the automaton that caused this exception, if any. */
  public Automaton getAutomaton() {
    return automaton;
  }

  /**
   * Return the RegExp that caused this exception if any.
   */
  public RegExp getRegExp() {
    return regExp;
  }

  /** Get the maximum number of allowed determinized states. */
  public int getMaxDeterminizedStates() {
    return maxDeterminizedStates;
  }
}

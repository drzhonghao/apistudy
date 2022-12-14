import org.apache.accumulo.core.constraints.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.accumulo.core.data.ConstraintViolationSummary;

/**
 * A class for accumulating constraint violations across a number of mutations.
 */
public class Violations {

  private static class CVSKey {
    private String className;
    private short vcode;

    CVSKey(ConstraintViolationSummary cvs) {
      this.className = cvs.constrainClass;
      this.vcode = cvs.violationCode;
    }

    @Override
    public int hashCode() {
      return className.hashCode() + vcode;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof CVSKey)
        return equals((CVSKey) o);
      return false;
    }

    public boolean equals(CVSKey ocvsk) {
      return className.equals(ocvsk.className) && vcode == ocvsk.vcode;
    }
  }

  private HashMap<CVSKey,ConstraintViolationSummary> cvsmap;

  /**
   * Creates a new empty object.
   */
  public Violations() {
    cvsmap = new HashMap<>();
  }

  /**
   * Checks if this object is empty, i.e., that no violations have been added.
   *
   * @return true if empty
   */
  public boolean isEmpty() {
    return cvsmap.isEmpty();
  }

  private void add(CVSKey cvsk, ConstraintViolationSummary cvs) {
    ConstraintViolationSummary existingCvs = cvsmap.get(cvsk);

    if (existingCvs == null) {
      cvsmap.put(cvsk, cvs);
    } else {
      existingCvs.numberOfViolatingMutations += cvs.numberOfViolatingMutations;
    }
  }

  /**
   * Adds a violation. If a matching violation was already added, then its count is increased.
   *
   * @param cvs
   *          summary of violation
   */
  public void add(ConstraintViolationSummary cvs) {
    CVSKey cvsk = new CVSKey(cvs);
    add(cvsk, cvs);
  }

  /**
   * Adds all violations from the given object to this one.
   *
   * @param violations
   *          violations to add
   */
  public void add(Violations violations) {
    Set<Entry<CVSKey,ConstraintViolationSummary>> es = violations.cvsmap.entrySet();

    for (Entry<CVSKey,ConstraintViolationSummary> entry : es) {
      add(entry.getKey(), entry.getValue());
    }

  }

  /**
   * Adds a list of violations.
   *
   * @param cvsList
   *          list of violation summaries
   */
  public void add(List<ConstraintViolationSummary> cvsList) {
    for (ConstraintViolationSummary constraintViolationSummary : cvsList) {
      add(constraintViolationSummary);
    }

  }

  /**
   * Gets the violations as a list of summaries.
   *
   * @return list of violation summaries
   */
  public List<ConstraintViolationSummary> asList() {
    return new ArrayList<>(cvsmap.values());
  }

}

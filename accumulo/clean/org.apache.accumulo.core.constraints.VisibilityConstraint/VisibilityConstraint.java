import org.apache.accumulo.core.constraints.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.accumulo.core.data.ColumnUpdate;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.security.VisibilityEvaluator;
import org.apache.accumulo.core.security.VisibilityParseException;
import org.apache.accumulo.core.util.BadArgumentException;

/**
 * A constraint that checks the visibility of columns against the actor's authorizations. Violation
 * codes:
 * <ul>
 * <li>1 = failure to parse visibility expression</li>
 * <li>2 = insufficient authorization</li>
 * </ul>
 */
public class VisibilityConstraint implements Constraint {

  @Override
  public String getViolationDescription(short violationCode) {
    switch (violationCode) {
      case 1:
        return "Malformed column visibility";
      case 2:
        return "User does not have authorization on column visibility";
    }

    return null;
  }

  @Override
  public List<Short> check(Environment env, Mutation mutation) {
    List<ColumnUpdate> updates = mutation.getUpdates();

    HashSet<String> ok = null;
    if (updates.size() > 1)
      ok = new HashSet<>();

    VisibilityEvaluator ve = null;

    for (ColumnUpdate update : updates) {

      byte[] cv = update.getColumnVisibility();
      if (cv.length > 0) {
        String key = null;
        if (ok != null && ok.contains(key = new String(cv, UTF_8)))
          continue;

        try {

          if (ve == null)
            ve = new VisibilityEvaluator(env.getAuthorizationsContainer());

          if (!ve.evaluate(new ColumnVisibility(cv)))
            return Collections.singletonList(Short.valueOf((short) 2));

        } catch (BadArgumentException bae) {
          return Collections.singletonList(Short.valueOf((short) 1));
        } catch (VisibilityParseException e) {
          return Collections.singletonList(Short.valueOf((short) 1));
        }

        if (ok != null)
          ok.add(key);
      }
    }

    return null;
  }
}

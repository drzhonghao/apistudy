import org.apache.accumulo.core.constraints.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.accumulo.core.data.ColumnUpdate;
import org.apache.accumulo.core.data.Mutation;

/**
 * A constraints that limits the size of keys to 1mb.
 */
public class DefaultKeySizeConstraint implements Constraint {

  protected static final short MAX__KEY_SIZE_EXCEEDED_VIOLATION = 1;
  protected static final long maxSize = 1048576; // 1MB default size

  @Override
  public String getViolationDescription(short violationCode) {

    switch (violationCode) {
      case MAX__KEY_SIZE_EXCEEDED_VIOLATION:
        return "Key was larger than 1MB";
    }

    return null;
  }

  final static List<Short> NO_VIOLATIONS = new ArrayList<>();

  @Override
  public List<Short> check(Environment env, Mutation mutation) {

    // fast size check
    if (mutation.numBytes() < maxSize)
      return NO_VIOLATIONS;

    List<Short> violations = new ArrayList<>();

    for (ColumnUpdate cu : mutation.getUpdates()) {
      int size = mutation.getRow().length;
      size += cu.getColumnFamily().length;
      size += cu.getColumnQualifier().length;
      size += cu.getColumnVisibility().length;

      if (size > maxSize)
        violations.add(MAX__KEY_SIZE_EXCEEDED_VIOLATION);
    }

    return violations;
  }
}

import org.apache.accumulo.minicluster.impl.*;


import java.util.Objects;

/**
 * Opaque handle to a process.
 */
public class ProcessReference {
  private final Process process;

  ProcessReference(Process process) {
    this.process = Objects.requireNonNull(process);
  }

  public Process getProcess() {
    return process;
  }

  @Override
  public String toString() {
    return getProcess().toString();
  }

  @Override
  public int hashCode() {
    return getProcess().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof ProcessReference) {
      return getProcess().equals(((ProcessReference) obj).getProcess());
    }
    throw new IllegalArgumentException(
        String.valueOf(obj) + " is not of type " + this.getClass().getName());
  }
}

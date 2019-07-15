import org.apache.karaf.shell.console.*;


import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
@Deprecated
public class MultiException extends Exception {

    private List<Exception> exceptions = new ArrayList<>();

    public MultiException(String message) {
        super(message);
    }

    public MultiException(String message, List<Exception> exceptions) {
        super(message);
        this.exceptions = exceptions;
    }

    public void addException(Exception e) {
        exceptions.add(e);
    }

    public void throwIfExceptions() throws MultiException {
        if (!exceptions.isEmpty()) {
            throw this;
        }
    }
    
    public Throwable[] getCauses() {
        return exceptions.toArray(new Throwable[exceptions.size()]);
    }

    @Override
    public void printStackTrace()
    {
        super.printStackTrace();
        for (Exception e : exceptions) {
            e.printStackTrace();
        }
    }


    /* ------------------------------------------------------------------------------- */
    /**
     * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
     */
    @Override
    public void printStackTrace(PrintStream out)
    {
        super.printStackTrace(out);
        for (Exception e : exceptions) {
            e.printStackTrace(out);
        }
    }

    @Override
    public void printStackTrace(PrintWriter out)
    {
        super.printStackTrace(out);
        for (Exception e : exceptions) {
            e.printStackTrace(out);
        }
    }

    public static void throwIf(String message, List<Exception> exceptions) throws MultiException {
        if (exceptions != null && !exceptions.isEmpty()) {
            StringBuilder sb = new StringBuilder(message);
            sb.append(":");
            for (Exception e : exceptions) {
                sb.append("\n\t");
                sb.append(e.getMessage());
            }
            throw new MultiException(sb.toString(), exceptions);
        }
    }
}

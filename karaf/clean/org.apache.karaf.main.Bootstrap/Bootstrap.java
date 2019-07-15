import org.apache.karaf.main.Main;
import org.apache.karaf.main.*;


/**
 * The Bootstrap class is used by the wrapper shell to bootstrap Karaf.
 * Given JSW is using introspection to launch the main method, it would cause
 * a CNFE on the OSGi related classes.  Using an indirection solves this problem.
 */
public class Bootstrap {

    public static void main(String[] args) throws Exception {
        Main.main(args);
    }
}

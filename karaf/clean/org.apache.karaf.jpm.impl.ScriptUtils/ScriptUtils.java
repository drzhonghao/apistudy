import org.apache.karaf.jpm.ProcessBuilder;
import org.apache.karaf.jpm.impl.*;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Scanner;

public class ScriptUtils {

    public static int execute(String name, Map<String, String> props) throws IOException {
        File script = File.createTempFile("jpm.", ".script");
        try {
            if (isWindows()) {
                String res = "windows/" + name + ".vbs";
                ScriptUtils.copyFilteredResource(res, script, props);
                return executeProcess(new java.lang.ProcessBuilder("cscript",
                                                                   "/NOLOGO",
                                                                   "//E:vbs",
                                                                   script.getCanonicalPath()));
            } else {
                String res = "unix/" + name + ".sh";
                ScriptUtils.copyFilteredResource(res, script, props);
                return executeProcess(new java.lang.ProcessBuilder("/bin/sh",
                                                                   script.getCanonicalPath()));
            }
        } finally {
            script.delete();
        }
    }

    public static int executeProcess(java.lang.ProcessBuilder builder) throws IOException {
        try {
            java.lang.Process process = builder.start();
            return process.waitFor();
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    public static void copyFilteredResource(String resource, File outFile, Map<String, String> props) throws IOException {
        InputStream is = null;
        try {
            is = ScriptUtils.class.getResourceAsStream(resource);
            // Read it line at a time so that we can use the platform line ending when we write it out.
            PrintStream out = new PrintStream(new FileOutputStream(outFile));
            try {
                Scanner scanner = new Scanner(is);
                while (scanner.hasNextLine() ) {
                    String line = scanner.nextLine();
                    line = filter(line, props);
                    out.println(line);
                }
                scanner.close();
            } finally {
                safeClose(out);
            }
        } finally {
            safeClose(is);
        }
    }

    private static void safeClose(InputStream is) throws IOException {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (Throwable ignore) {
        }
    }

    private static void safeClose(OutputStream is) throws IOException {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (Throwable ignore) {
        }
    }

    private static String filter(String line, Map<String, String> props) {
        for (Map.Entry<String, String> i : props.entrySet()) {
            int p1 = line.indexOf(i.getKey());
            if( p1 >= 0 ) {
                String l1 = line.substring(0, p1);
                String l2 = line.substring(p1+i.getKey().length());
                line = l1+i.getValue()+l2;
            }
        }
        return line;
    }

    private static final boolean windows;

    static {
        windows = System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
    }

    public static boolean isWindows() {
        return windows;
    }

    public static String getJavaCommandPath() throws IOException {
        return new File(System.getProperty("java.home"), isWindows() ? "bin\\java.exe" : "bin/java").getCanonicalPath();
    }

}

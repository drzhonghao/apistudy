import org.apache.karaf.jpm.impl.ScriptUtils;
import org.apache.karaf.jpm.ProcessBuilder;
import org.apache.karaf.jpm.impl.*;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.jpm.Process;

public class ProcessImpl implements Process {

    /**
     * 
     */
    private static final long serialVersionUID = -8140632422386086507L;

    private int pid;
    //private File input;
    //private File output;
    //private File error;

    public ProcessImpl(int pid/*, File input, File output, File error*/) {
        this.pid = pid;
        //this.input = input;
        //this.output = output;
        //this.error = error;
    }

    public int getPid() {
        return pid;
    }

    public boolean isRunning() throws IOException {
        if (ScriptUtils.isWindows()) {
            Map<String, String> props = new HashMap<>();
            props.put("${pid}", Integer.toString(pid));
            int ret = ScriptUtils.execute("running", props);
            return ret == 0;
        } else {
            try {
                java.lang.Process process = new java.lang.ProcessBuilder("ps", "-p", Integer.toString(pid)).start();
                BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
                r.readLine(); // skip headers
                String s = r.readLine();
                boolean running = s != null && s.length() > 0;
                process.waitFor();
                return running;
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
        }
    }

    public void destroy() throws IOException {
        int ret;
        if (ScriptUtils.isWindows()) {
            Map<String, String> props = new HashMap<>();
            props.put("${pid}", Integer.toString(pid));
            ret = ScriptUtils.execute("destroy", props);
        } else {
            ret = ScriptUtils.executeProcess(new java.lang.ProcessBuilder("kill", "-9", Integer.toString(pid)));
        }
        if (ret != 0) {
            throw new IOException("Unable to destroy process, it may already be terminated");
        }
    }

    /*
    public OutputStream getInputStream() throws FileNotFoundException {
        return new FileOutputStream(input);
    }

    public InputStream getOutputStream() throws FileNotFoundException {
        return new FileInputStream(output);
    }

    public InputStream getErrorStream() throws FileNotFoundException {
        return new FileInputStream(error);
    }
    */

    public int waitFor() throws InterruptedException {
        return 0;
    }

    public int exitValue() {
        return 0;
    }

    public static Process create(File dir, String command) throws IOException {
        //File input = File.createTempFile("jpm.", ".input");
        //File output = File.createTempFile("jpm.", ".output");
        //File error = File.createTempFile("jpm.", ".error");
        File pidFile = File.createTempFile("jpm.", ".pid");
        try {
            Map<String, String> props = new HashMap<>();
            //props.put("${in.file}", input.getCanonicalPath());
            //props.put("${out.file}", output.getCanonicalPath());
            //props.put("${err.file}", error.getCanonicalPath());
            props.put("${pid.file}", pidFile.getCanonicalPath());
            props.put("${dir}", dir != null ? dir.getCanonicalPath() : "");
            if (ScriptUtils.isWindows()) {
                command = command.replaceAll("\"", "\"\"");
            }
            props.put("${command}", command);
            int ret = ScriptUtils.execute("start", props);
            if (ret != 0) {
                throw new IOException("Unable to create process (error code: " + ret + ")");
            }
            int pid = readPid(pidFile);
            return new ProcessImpl(pid/*, input, output, error*/);
        } finally {
            pidFile.delete();
        }
    }

    public static Process attach(int pid) throws IOException {
        return new ProcessImpl(pid);
    }

    private static int readPid(File pidFile) throws IOException {
        InputStream is = new FileInputStream(pidFile);
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String pidString = r.readLine();
            return Integer.valueOf(pidString);
        } finally {
            try {
                is.close();
            } catch (IOException e) {}
        }
    }

}

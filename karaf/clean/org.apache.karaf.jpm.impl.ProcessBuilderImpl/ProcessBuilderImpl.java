import org.apache.karaf.jpm.impl.ProcessImpl;
import org.apache.karaf.jpm.impl.*;


import java.io.File;
import java.io.IOException;

import org.apache.karaf.jpm.Process;
import org.apache.karaf.jpm.ProcessBuilder;


public class ProcessBuilderImpl implements ProcessBuilder {

    private File dir;
    private String command;

    public ProcessBuilder directory(File dir) {
        this.dir = dir;
        return this;
    }

    public ProcessBuilder command(String command) {
        this.command = command;
        return this;
    }

    public Process start() throws IOException {
        return ProcessImpl.create(dir, command);
    }

    public Process attach(int pid) throws IOException {
        return ProcessImpl.attach(pid);
    }
}

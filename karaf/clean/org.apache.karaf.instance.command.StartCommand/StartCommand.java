import org.apache.karaf.instance.command.InstanceCommandSupport;
import org.apache.karaf.instance.command.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.instance.command.completers.StoppedInstanceCompleter;
import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.MultiException;

@Command(scope = "instance", name = "start", description = "Start an existing container instance.")
@Service
public class StartCommand extends InstanceCommandSupport {
                      
    @Option(name = "-d", aliases = { "--debug"}, description = "Start the instance in debug mode", required = false, multiValued = false)
    private boolean debug; 
    
    @Option(name = "-o", aliases = { "--java-opts"}, description = "Java options when launching the instance", required = false, multiValued = false)
    private String javaOpts;

    @Option(name = "-w", aliases = { "--wait"}, description = "Wait for the instance to be fully started", required = false, multiValued = false)
    private boolean wait;

    @Argument(index = 0, name = "name", description = "The name of the container instance", required = true, multiValued = true)
    @Completion(StoppedInstanceCompleter.class)
    private List<String> instances = null;

    static final String DEBUG_OPTS = " -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005";
    static final String DEFAULT_OPTS = "-server -Xmx512M -Dcom.sun.management.jmxremote";

    @SuppressWarnings("deprecation")
    protected Object doExecute() throws Exception {
        MultiException exception = new MultiException("Error starting instance(s)");
        List<Instance> toWaitFor = new ArrayList<>();
        for (Instance instance : getMatchingInstances(instances)) {
            try {
                String opts = javaOpts;
                if (opts == null) {
                    opts = instance.getJavaOpts();
                }
                if (opts == null) {
                    opts = DEFAULT_OPTS;
                }
                if (debug) {
                    opts += DEBUG_OPTS;
                }
                if (wait) {
                    String state = instance.getState();
                    if (Instance.STOPPED.equals(state)) {
                        instance.start(opts);
                        toWaitFor.add(instance);
                    }
                } else {
                    instance.start(opts);
                }
            } catch (Exception e) {
                exception.addException(e);
            }
        }
        exception.throwIfExceptions();
        while (true) {
            boolean allStarted = true;
            for (Instance child : toWaitFor) {
                allStarted &= Instance.STARTED.equals(child.getState());
            }
            if (allStarted) {
                break;
            } else {
                Thread.sleep(500);
            }
        }
        return null;
    }

}

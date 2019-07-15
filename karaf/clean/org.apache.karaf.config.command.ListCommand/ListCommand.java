import org.apache.karaf.config.command.ConfigCommandSupport;
import org.apache.karaf.config.command.*;


import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.service.cm.Configuration;

@Command(scope = "config", name = "list", description = "Lists existing configurations.")
@Service
public class ListCommand extends ConfigCommandSupport {

    @Option(name = "-s", aliases = "--short", description = "Only list the PIDs, not the properties", required = false, multiValued = false)
    boolean shortOutput;

    @Argument(index = 0, name = "query", description = "Query in LDAP syntax. Example: \"(service.pid=org.apache.karaf.log)\"", required = false, multiValued = false)
    String query;

    @SuppressWarnings("rawtypes")
    protected Object doExecute() throws Exception {
        Configuration[] configs = configRepository.getConfigAdmin().listConfigurations(query);
        if (configs != null) {
            Map<String, Configuration> sortedConfigs = new TreeMap<>();
            for (Configuration config : configs) {
                sortedConfigs.put(config.getPid(), config);
            }
            if (shortOutput) {
                for (Configuration config : sortedConfigs.values()) {
                    System.out.println(config.getPid());
                }
            } else {
                for (Configuration config : sortedConfigs.values()) {
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("Pid:            " + config.getPid());
                    if (config.getFactoryPid() != null) {
                        System.out.println("FactoryPid:     " + config.getFactoryPid());
                    }
                    System.out.println("BundleLocation: " + config.getBundleLocation());
                    if (config.getProperties() != null) {
                        System.out.println("Properties:");
                        Dictionary props = config.getProperties();
                        Map<String, Object> sortedProps = new TreeMap<>();
                        for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
                            Object key = e.nextElement();
                            sortedProps.put(key.toString(), props.get(key));
                        }
                        for (Map.Entry<String, Object> entry : sortedProps.entrySet()) {
                            System.out.println("   " + entry.getKey() + " = " + displayValue(entry.getValue()));
                        }
                    }
                }
            }
        }
        return null;
    }
}

import org.apache.karaf.event.command.EventPrinter;
import org.apache.karaf.event.command.*;


import static org.apache.karaf.event.service.TopicPredicate.matchTopic;

import org.apache.karaf.event.service.EventCollector;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.framework.BundleContext;

@Command(scope = "event", name = "display", description = "Shows events")
@Service
public class EventDisplayCommand implements Action {

    @Reference
    Session session;

    @Reference
    BundleContext context;

    @Reference
    EventCollector collector;

    @Argument
    String topicFilter = "*";

    @Option(name = "-v")
    boolean verbose = false;

    @Override
    public Object execute() throws Exception {
        EventPrinter printer = new EventPrinter(session.getConsole(), verbose);
        collector.getEvents().filter(matchTopic(topicFilter)).forEach(printer);
        return null;
    }

}

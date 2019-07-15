import org.apache.karaf.event.command.EventPrinter;
import org.apache.karaf.event.command.*;


import static org.apache.karaf.event.service.TopicPredicate.matchTopic;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.karaf.event.service.EventCollector;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;

@Command(scope = "event", name = "tail", description = "Shows events and listens for incoming events")
@Service
public class EventTailCommand implements Action {

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
        Consumer<Event> filteredPrinter = executeIf(matchTopic(topicFilter), printer);
        collector.addConsumer(filteredPrinter);
        try {
            waitTillInterrupted();
        } catch (InterruptedException e) {
            collector.removeConsumer(filteredPrinter);
        }
        return null;
    }
    
    private <T> Consumer<T> executeIf(Predicate<T> pred, Consumer<T> consumer) {
        return t -> {if (pred.test(t)) consumer.accept(t);};
    }

    private void waitTillInterrupted() throws InterruptedException {
        while (true) {
            Thread.sleep(100);
        }
    }
}

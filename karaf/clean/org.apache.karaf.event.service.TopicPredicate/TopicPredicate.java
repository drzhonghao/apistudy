import org.apache.karaf.event.service.*;


import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.osgi.service.event.Event;

public class TopicPredicate implements Predicate<Event> {
    private Pattern pattern;

    private TopicPredicate(String topicFilter) {
        pattern = Pattern.compile(topicFilter.replace("*", ".*"));
    }
    
    @Override
    public boolean test(Event event) {
        return pattern.matcher(event.getTopic()).matches();
    }
 
    public static Predicate<Event> matchTopic(String topicFilter) {
        return new TopicPredicate(topicFilter);
    }

}

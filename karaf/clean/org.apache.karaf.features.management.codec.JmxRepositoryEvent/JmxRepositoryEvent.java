import org.apache.karaf.features.management.codec.*;


import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.apache.karaf.features.RepositoryEvent;
import org.apache.karaf.features.management.FeaturesServiceMBean;

public class JmxRepositoryEvent {

    public static final CompositeType REPOSITORY_EVENT;

    private final CompositeData data;

    public JmxRepositoryEvent(RepositoryEvent event) {
        try {
            String[] itemNames = FeaturesServiceMBean.REPOSITORY_EVENT;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = event.getRepository().getURI().toString();
            switch (event.getType()) {
            case RepositoryAdded:
                itemValues[1] = FeaturesServiceMBean.REPOSITORY_EVENT_EVENT_TYPE_ADDED;
                break;
            case RepositoryRemoved:
                itemValues[1] = FeaturesServiceMBean.REPOSITORY_EVENT_EVENT_TYPE_REMOVED;
                break;
            default:
                throw new IllegalStateException("Unsupported event type: " + event.getType());
            }
            data = new CompositeDataSupport(REPOSITORY_EVENT, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form repository event open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    static {
        REPOSITORY_EVENT = createRepositoryEventType();
    }

    private static CompositeType createRepositoryEventType() {
        try {
            String description = "This type identify a Karaf repository event";
            String[] itemNames = FeaturesServiceMBean.REPOSITORY_EVENT;
            OpenType<?>[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;

            itemDescriptions[0] = "The uri of the repository";
            itemDescriptions[1] = "The type of event";

            return new CompositeType("RepositoryEvent", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build repositoryEvent type", e);
        }
    }
}

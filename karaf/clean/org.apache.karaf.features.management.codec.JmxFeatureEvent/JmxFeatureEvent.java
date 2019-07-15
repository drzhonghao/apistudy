import org.apache.karaf.features.management.codec.*;


import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.management.FeaturesServiceMBean;

public class JmxFeatureEvent {

    public static final CompositeType FEATURE_EVENT;

    private final CompositeData data;

    public JmxFeatureEvent(FeatureEvent event) {
        try {
            String[] itemNames = FeaturesServiceMBean.FEATURE_EVENT;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = event.getFeature().getName();
            itemValues[1] = event.getFeature().getVersion();
            itemValues[2] = event.getRegion();
            switch (event.getType()) {
            case FeatureInstalled:
                itemValues[2] = FeaturesServiceMBean.FEATURE_EVENT_EVENT_TYPE_INSTALLED;
                break;
            case FeatureUninstalled:
                itemValues[2] = FeaturesServiceMBean.FEATURE_EVENT_EVENT_TYPE_UNINSTALLED;
                break;
            default:
                throw new IllegalStateException("Unsupported event type: " + event.getType());
            }
            data = new CompositeDataSupport(FEATURE_EVENT, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature event open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    static {
        FEATURE_EVENT = createFeatureEventType();
    }

    private static CompositeType createFeatureEventType() {
        try {
            String description = "This type identify a Karaf feature event";
            String[] itemNames = FeaturesServiceMBean.FEATURE_EVENT;
            OpenType<?>[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = SimpleType.STRING;
            itemTypes[3] = SimpleType.STRING;

            itemDescriptions[0] = "The id of the feature";
            itemDescriptions[1] = "The version of the feature";
            itemDescriptions[2] = "The type of the event";
            itemDescriptions[3] = "The region of this feature";

            return new CompositeType("FeatureEvent", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build featureEvent type", e);
        }
    }
}

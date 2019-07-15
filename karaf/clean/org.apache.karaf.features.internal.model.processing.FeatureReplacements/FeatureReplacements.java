import org.apache.karaf.features.internal.model.processing.*;


import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import org.apache.karaf.features.FeaturesNamespaces;
import org.apache.karaf.features.internal.model.Feature;

@XmlType(name = "featureReplacements", propOrder = {
        "replacements"
})
@XmlAccessorType(XmlAccessType.FIELD)
public class FeatureReplacements {

    @XmlElement(name = "replacement")
    private List<OverrideFeature> replacements = new LinkedList<>();

    public List<OverrideFeature> getReplacements() {
        return replacements;
    }

    @XmlType(name = "featureOverrideMode")
    @XmlEnum
    public enum FeatureOverrideMode {
        @XmlEnumValue("replace")
        REPLACE,
        @XmlEnumValue("merge")
        MERGE,
        @XmlEnumValue("remove")
        REMOVE
    }

    @XmlType(name = "overrideFeature", propOrder = {
            "feature"
    })
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OverrideFeature {
        @XmlAttribute
        private FeatureOverrideMode mode = FeatureOverrideMode.REPLACE;
        @XmlElement
        private Feature feature;

        public FeatureOverrideMode getMode() {
            return mode;
        }

        public void setMode(FeatureOverrideMode mode) {
            this.mode = mode;
        }

        public Feature getFeature() {
            return feature;
        }

        public void setFeature(Feature feature) {
            this.feature = feature;
        }
    }

}

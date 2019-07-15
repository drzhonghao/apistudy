import org.apache.karaf.features.internal.model.processing.*;


import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.karaf.features.LocationPattern;

@XmlType(name = "bundleReplacements", propOrder = {
        "overrideBundles"
})
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleReplacements {

    @XmlElement(name = "bundle")
    private List<OverrideBundle> overrideBundles = new LinkedList<>();

    public List<OverrideBundle> getOverrideBundles() {
        return overrideBundles;
    }

    @XmlType(name = "bundleOverrideMode")
    @XmlEnum
    public enum BundleOverrideMode {
        @XmlEnumValue("osgi")
        OSGI,
        @XmlEnumValue("maven")
        MAVEN
    }

    @XmlType(name = "overrideBundle")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OverrideBundle {
        @XmlAttribute
        private String originalUri;
        @XmlTransient
        private LocationPattern originalUriPattern;
        @XmlAttribute
        private String replacement;
        @XmlAttribute
        private BundleOverrideMode mode = BundleOverrideMode.OSGI;

        public String getOriginalUri() {
            return originalUri;
        }

        public void setOriginalUri(String originalUri) {
            this.originalUri = originalUri;
        }

        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }

        public BundleOverrideMode getMode() {
            return mode;
        }

        public void setMode(BundleOverrideMode mode) {
            this.mode = mode;
        }

        public LocationPattern getOriginalUriPattern() {
            return originalUriPattern;
        }

        /**
         * Changes String for <code>originalUri</code> into {@link LocationPattern}
         */
        public void compile() throws MalformedURLException {
            originalUriPattern = new LocationPattern(originalUri);
        }
    }

}

import org.apache.karaf.features.internal.model.processing.*;


import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "overrideBundleDependency", propOrder = {
        "repositories",
        "features",
        "bundles"
})
@XmlAccessorType(XmlAccessType.FIELD)
public class OverrideBundleDependency {

    @XmlElement(name = "repository")
    private List<OverrideDependency> repositories = new LinkedList<>();
    @XmlElement(name = "feature")
    private List<OverrideFeatureDependency> features = new LinkedList<>();
    @XmlElement(name = "bundle")
    private List<OverrideDependency> bundles = new LinkedList<>();

    public List<OverrideDependency> getRepositories() {
        return repositories;
    }

    public List<OverrideFeatureDependency> getFeatures() {
        return features;
    }

    public List<OverrideDependency> getBundles() {
        return bundles;
    }

    @XmlType(name = "overrideDependency")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OverrideDependency {
        @XmlAttribute
        private String uri;
        @XmlAttribute
        private boolean dependency = false;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public boolean isDependency() {
            return dependency;
        }

        public void setDependency(boolean dependency) {
            this.dependency = dependency;
        }
    }

    @XmlType(name = "overrideFeatureDependency")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OverrideFeatureDependency {
        @XmlAttribute
        private String name;
        @XmlAttribute
        private String version;
        @XmlAttribute
        private boolean dependency = false;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public boolean isDependency() {
            return dependency;
        }

        public void setDependency(boolean dependency) {
            this.dependency = dependency;
        }
    }

}
